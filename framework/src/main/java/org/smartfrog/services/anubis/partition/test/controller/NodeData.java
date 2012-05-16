/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

 */
package org.smartfrog.services.anubis.partition.test.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.test.msg.GetStatsMsg;
import org.smartfrog.services.anubis.partition.test.msg.GetThreadsMsg;
import org.smartfrog.services.anubis.partition.test.msg.IgnoringMsg;
import org.smartfrog.services.anubis.partition.test.msg.PartitionMsg;
import org.smartfrog.services.anubis.partition.test.msg.SetIgnoringMsg;
import org.smartfrog.services.anubis.partition.test.msg.SetTimingMsg;
import org.smartfrog.services.anubis.partition.test.msg.StatsMsg;
import org.smartfrog.services.anubis.partition.test.msg.ThreadsMsg;
import org.smartfrog.services.anubis.partition.test.msg.TimingMsg;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.Wire;
import org.smartfrog.services.anubis.partition.wire.WireMsg;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.SerializedMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.jackal.partition.comms.AbstractMessageHandler;
import com.hellblazer.pinkie.SocketChannelHandler;

public class NodeData {
    private class Connection extends AbstractMessageHandler {

        public Connection(WireSecurity wireSecurity) {
            super(wireSecurity);
        }

        @Override
        public void accept(SocketChannelHandler handler) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void closing() {
            controller.disconnectNode(NodeData.this);
        }

        @Override
        public void connect(SocketChannelHandler handler) {
            this.handler = handler;
            this.handler.selectForRead();
            connection.sendObject(new SetTimingMsg(
                                                   controller.getHeartbeatInterval(),
                                                   controller.getHeartbeatTimeout()));
        }

        public void sendObject(Object obj) {
            SerializedMsg msg = new SerializedMsg(obj);
            try {
                super.sendObject(wireSecurity.toWireForm(msg, bufferPool));
            } catch (Exception ex) {
                if (log.isWarnEnabled()) {
                    log.warn("", ex);
                }
            }
        }

        @Override
        protected void deliverObject(long order, ByteBuffer readBuffer) {
            WireMsg wire;
            try {
                wire = Wire.fromWire(readBuffer);
            } catch (Exception ex) {
                if (log.isWarnEnabled()) {
                    log.warn("unable to deserialize message", ex);
                }
                return;
            }

            if (wire instanceof Heartbeat) {
                controller.receiveHeartbeat((Heartbeat) wire);
                return;
            }

            SerializedMsg msg = null;
            try {
                msg = (SerializedMsg) wire;
                Object obj = msg.getObject();

                controller.deliverObject(obj, NodeData.this);
            } catch (Exception ex) {
                if (log.isWarnEnabled()) {
                    log.warn("", ex);
                }
            }
        }

        @Override
        protected Logger getLog() {
            return log;
        }

        public String toString() {
            return String.format("NodeData Connection for %s", nodeId);
        }
    }

    private static final Logger   log               = LoggerFactory.getLogger(NodeData.class.getCanonicalName());
    protected volatile Connection connection;
    protected final Controller    controller;
    protected volatile long       heartbeatInterval = 0;
    protected volatile View       ignoring          = new BitView();
    protected volatile long       lastHB            = 0;
    protected volatile long       lastReceive       = 0;
    protected volatile int        leader            = -1;
    protected final Identity      nodeId;
    protected volatile View       partition         = new BitView();
    protected volatile StatsMsg   stats             = null;
    protected volatile ThreadsMsg threadsInfo       = null;
    protected volatile long       threadsInfoExpire = 0;
    protected volatile long       timeout           = 0;

    protected volatile View       view              = null;

    public NodeData(Heartbeat hb, Controller controller) {
        this.controller = controller;
        lastReceive = System.currentTimeMillis();
        lastHB = hb.getTime();
        view = hb.getView();
        nodeId = hb.getSender();
        connectIfAvailable(hb.getControllerInterface());
    }

    public void deliverObject(Object obj) {
        lastReceive = System.currentTimeMillis();
        if (obj instanceof PartitionMsg) {
            PartitionMsg msg = (PartitionMsg) obj;
            partitionNotification(msg.partition, msg.leader);
        } else if (obj instanceof TimingMsg) {
            TimingMsg msg = (TimingMsg) obj;
            timing(msg.interval, msg.timeout);
        } else if (obj instanceof StatsMsg) {
            stats((StatsMsg) obj);
        } else if (obj instanceof IgnoringMsg) {
            ignoring((IgnoringMsg) obj);
        } else if (obj instanceof ThreadsMsg) {
            threads((ThreadsMsg) obj);
        } else {
            log.error("Unrecognised object received in test connection at console"
                      + obj);
            connection.shutdown();
        }
    }

    public void disconnected() {
        connection = null;
        update();
    }

    public Identity getIdentity() {
        return nodeId;
    }

    public String getIgnoringString() {
        return ignoring.toString();
    }

    public View getPartition() {
        return partition;
    }

    public String getPartitionString() {
        return partition.toBitSet().toString();
    }

    public void getStats() {
        if (connection == null) {
            return;
        }
        connection.sendObject(new GetStatsMsg());
    }

    public void getThreads() {
        if (connection == null) {
            return;
        }
        connection.sendObject(new GetThreadsMsg());
    }

    public View getView() {
        return view;
    }

    public String getViewString() {
        return view.toBitSet().toString();
    }

    public void heartbeat(Heartbeat hb) {

        if (lastHB <= hb.getTime()) {
            lastHB = hb.getTime();
            lastReceive = System.currentTimeMillis();
            if (view.isStable() != hb.getView().isStable()
                || !view.equals(hb.getView())) {
                view = hb.getView();
                update();
            }
            // System.out.println("Heartbeat from Node" + nodeId.id + " stamped " + lastHB);
        } else {
            // System.out.println("Heartbeat from Node" + nodeId.id + " stamped " + hb.getTime() + " <==== This is out of order!!!!");
        }
        if (connection == null) {
            connectIfAvailable(hb.getControllerInterface());
        }
    }

    public boolean olderThan(long oldTime) {
        return lastReceive < oldTime;
    }

    public void removeNode() {
        if (connection != null) {
            connection.shutdown();
        }
        controller.removeNode(this);
    }

    public void setIgnoring(View ignoring) {
        if (connection != null) {
            connection.sendObject(new SetIgnoringMsg(ignoring));
        }
    }

    public void setIgnoringAsymPartition(View globalView, View partition) {
        if (connection == null) {
            return;
        }

        if (partition.contains(getIdentity())) {
            connection.sendObject(new SetIgnoringMsg(
                                                     partCompOrIgnoring(globalView,
                                                                        partition,
                                                                        ignoring)));
        }
    }

    public void setIgnoringSymPartition(View globalView, View partition) {
        if (connection == null) {
            return;
        }

        if (partition.contains(getIdentity())) {
            connection.sendObject(new SetIgnoringMsg(
                                                     partCompOrIgnoring(globalView,
                                                                        partition,
                                                                        ignoring)));
        } else {
            connection.sendObject(new SetIgnoringMsg(partOrIgnoring(partition,
                                                                    ignoring)));
        }
    }

    public void setTiming(long interval, long timeout) {
        if (connection == null) {
            return;
        }
        connection.sendObject(new SetTimingMsg(interval, timeout));
    }

    @Override
    public String toString() {
        return "NodeData [id=" + nodeId + "]";
    }

    private void connectIfAvailable(InetSocketAddress address) {

        if (address == null) {
            return;
        }

        connection = new Connection(controller.getWireSecurity());
        try {
            controller.getHandler().connectTo(address, connection);
        } catch (IOException e) {
            connection = null;
            log.error(String.format("Cannot connect to node: %s", nodeId), e);
        }

    }

    private View partOrIgnoring(View partition, View ignoring) {
        return new BitView().copyView(partition).merge(ignoring);
    }

    protected void ignoring(IgnoringMsg ignoringMsg) {
        ignoring = ignoringMsg.ignoring;
        update();
    }

    protected View partCompOrIgnoring(View globalView, View partition,
                                      View ignoring) {
        View complement = new BitView().copyView(globalView).subtract(partition);
        return new BitView().copyView(complement).merge(ignoring);
    }

    protected void partitionNotification(View partition, int leader) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Updating node view %s with partition view %s, leader %s",
                                    nodeId, partition, leader));
        }
        this.partition = partition;
        this.leader = leader;
        update();
    }

    protected void stats(StatsMsg stats) {
        this.stats = stats;
        update();
    }

    protected void threads(ThreadsMsg threads) {
        threadsInfoExpire = System.currentTimeMillis() + 10000;
        threadsInfo = threads;
        update();
    }

    protected void timing(long interval, long timeout) {
        heartbeatInterval = interval;
        this.timeout = timeout;
        update();
    }

    protected void update() {
        if (threadsInfoExpire < System.currentTimeMillis()) {
            threadsInfo = null;
        }
    }

}
