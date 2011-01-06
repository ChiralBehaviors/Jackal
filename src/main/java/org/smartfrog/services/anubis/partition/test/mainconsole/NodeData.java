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
package org.smartfrog.services.anubis.partition.test.mainconsole;

import java.awt.Color;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.test.colors.ColorAllocator;
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
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

public class NodeData {
    private NodeButton button;
    private ColorAllocator colorAllocator;
    private TestConnection connection = null;
    private Controller controller = null;
    private long heartbeatInterval = 0;
    private View ignoring = new BitView();
    private long lastHB = 0;
    private long lastReceive = 0;
    private int leader = -1;
    private static final Logger log = Logger.getLogger(NodeData.class.getCanonicalName());
    private Identity nodeId = null;
    private final boolean headless;
    private View partition = new BitView();
    private Color partitionColor = Color.lightGray;
    private StatsMsg stats = null;
    private ThreadsMsg threadsInfo = null;
    private long threadsInfoExpire = 0;
    private long timeout = 0;
    private View view = null;
    private NodeFrame window = null;

    public NodeData(HeartbeatMsg hb, ColorAllocator colorAllocator,
                    Controller controller, boolean headless) {
        this.controller = controller;
        this.colorAllocator = colorAllocator;
        this.headless = headless;
        lastReceive = System.currentTimeMillis();
        lastHB = hb.getTime();
        view = hb.getView();
        nodeId = hb.getSender();
        ConnectionAddress address = hb.getTestInterface();
        if (!headless) {
            button = new NodeButton(nodeId, this);
            button.setOpaque(true);
            button.setBackground(Color.lightGray);
            button.setForeground(Color.black);
        }
        connectIfAvailable(address);
        //System.out.print("Heartbeat from new Node" + nodeId.id + " stamped " + lastHB);
        //long timeNow = System.currentTimeMillis();
        //if( timeNow < (lastHB-100) )
        //    System.out.println(" <=== this is " + (lastHB - timeNow) + "ms old!!!!!");
        //else
        //    System.out.println("");
    }

    public void closeWindow() {
        if (window != null) {
            window.setVisible(false);
            window.dispose();
            window = null;
        }
    }

    public void deliverObject(Object obj) {
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
            if (log.isLoggable(Level.SEVERE)) {
                log.severe("Unrecognised object received in test connection at console"
                           + obj);
            }
            connection.shutdown();
        }
    }

    public void disconnected() {
        connection = null;
        if (!headless) {
            colorAllocator.deallocate(partition, this);
            partitionColor = Color.lightGray;
        }
        update();
    }

    public NodeButton getButton() {
        return button;
    }

    public Identity getIdentity() {
        return nodeId;
    }

    public String getIgnoringString() {
        return ignoring.toString();
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

    public void heartbeat(HeartbeatMsg hb) {

        if (lastHB <= hb.getTime()) {
            lastHB = hb.getTime();
            lastReceive = System.currentTimeMillis();
            if (view.isStable() != hb.getView().isStable()
                || !view.equals(hb.getView())) {
                view = hb.getView();
                update();
            }
            //System.out.println("Heartbeat from Node" + nodeId.id + " stamped " + lastHB);
        } else {
            //System.out.println("Heartbeat from Node" + nodeId.id + " stamped " + hb.getTime() + " <==== This is out of order!!!!");
        }
        if (connection == null) {
            connectIfAvailable(hb.getTestInterface());
        }
    }

    public boolean olderThan(long oldTime) {
        return lastReceive < oldTime;
    }

    public void openWindow() {
        if (window == null) {
            window = new NodeFrame(this);
            window.setTitle("Node: " + nodeId.toString());
            update();
            window.setVisible(true);
        }
    }

    public void removeNode() {
        if (connection != null && connection.connected()) {
            connection.shutdown();
        }
        closeWindow();
        controller.removeNode(this);
    }

    public void setIgnoring(String str) {
        StringTokenizer nodes = new StringTokenizer(str);
        BitView ignoring = new BitView();
        String token = "";

        if (nodes.countTokens() == 0) {
            setIgnoring(new BitView());
            return;
        }

        try {
            while (nodes.hasMoreTokens()) {
                token = nodes.nextToken();
                // System.out.println("Looking at token: " + token);
                Integer inode = new Integer(token);
                int node = inode.intValue();
                if (node < 0) {
                    throw new NumberFormatException();
                }
                ignoring.add(node);
            }
        } catch (NumberFormatException ex) {
            window.inputError("Not a correct node value: " + token);
            return;
        }

        setIgnoring(ignoring);
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

    private void connectIfAvailable(ConnectionAddress address) {

        if (address == null) {
            return;
        }

        connection = new TestConnection(address, this, nodeId, controller);
        if (connection.connected()) {
            connection.sendObject(new SetTimingMsg(
                                                   controller.getHeartbeatInterval(),
                                                   controller.getHeartbeatTimeout()));
            connection.start();
        } else {
            connection = null;
        }
    }

    private View partOrIgnoring(View partition, View ignoring) {
        return new BitView().copyView(partition).merge(ignoring);
    }

    private void update() {
        if (threadsInfoExpire < System.currentTimeMillis()) {
            threadsInfo = null;
        }
        if (!headless) {
            if (window != null) {
                window.update(partition, view, leader, ignoring,
                              heartbeatInterval, timeout, stats, threadsInfo);
            }
            button.setBackground(partitionColor);
            if (partition.isStable()) {
                button.setForeground(Color.black);
            } else {
                button.setForeground(Color.yellow);
            }
        }
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
        if (log.isLoggable(Level.FINEST)) {
            log.finest(String.format("Updating node view %s with partition view %s, leader %s",
                                     nodeId, partition, leader));
        }

        /**
         * if partition has changed membership deallocate current color and then
         * reallocate new color
         */
        if (!headless
            && !this.partition.toBitSet().equals(partition.toBitSet())) {
            colorAllocator.deallocate(this.partition, this);
            partitionColor = colorAllocator.allocate(partition, this);
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

}
