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
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionManager;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.pinkie.ChannelHandler;
import com.hellblazer.pinkie.SocketOptions;

public class Controller implements ConnectionManager {
    private final ScheduledExecutorService  timer;
    private BitView                         globalView = new BitView();
    protected long                          heartbeatInterval;
    protected long                          heartbeatTimeout;
    protected final Identity                identity;
    protected final Map<Identity, NodeData> nodes      = new ConcurrentHashMap<Identity, NodeData>();
    private ScheduledFuture<?>              task;
    private final ChannelHandler            handler;
    private final WireSecurity              wireSecurity;

    public Controller(Identity partitionIdentity, int heartbeatTimeout,
                      int heartbeatInterval, SocketOptions socketOptions,
                      ExecutorService dispatcher, WireSecurity wireSecurity)
                                                                            throws IOException {
        timer = Executors.newScheduledThreadPool(1);
        identity = partitionIdentity;
        this.heartbeatTimeout = heartbeatTimeout;
        this.heartbeatInterval = heartbeatInterval;
        handler = new ChannelHandler("Partition Controller", socketOptions,
                                     dispatcher);
        this.wireSecurity = wireSecurity;
    }

    public synchronized void asymPartition(BitView partition) {
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NodeData node = iter.next();
            if (partition.contains(node.getIdentity())) {
                node.setIgnoringAsymPartition(globalView, partition);
            }
        }
    }

    public synchronized void checkNodes() {
        long timeNow = System.currentTimeMillis();
        long expireTime = timeNow - heartbeatInterval * heartbeatTimeout;
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NodeData nodeData = iter.next();
            if (nodeData.olderThan(expireTime)) {
                System.out.println(String.format("Removing node: %s, timeNow: %s expireTime: %s, lastReceive: %s",
                                                 nodeData.nodeId, timeNow,
                                                 expireTime,
                                                 nodeData.lastReceive));
                iter.remove();
                globalView.remove(nodeData.getIdentity());
                nodeData.removeNode();
            }
        }
    }

    public synchronized void clearNodes() {
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NodeData nodeData = iter.next();
            nodeData.removeNode();
            iter.remove();
        }
        globalView = new BitView();
    }

    public synchronized void clearPartitions() {
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            iter.next().setIgnoring(new BitView());
        }
    }

    @Override
    public void connectTo(Identity peer) {
        throw new UnsupportedOperationException();
    }

    public synchronized void deliverObject(Object obj, NodeData node) {
        node.deliverObject(obj);
    }

    /**
     * @throws IOException
     */
    public synchronized void deploy() throws IOException {
        task = timer.schedule(getTask(), heartbeatInterval,
                              TimeUnit.MILLISECONDS);
    }

    public synchronized void disconnectNode(NodeData node) {
        node.disconnected();
    }

    /**
     * @return
     */
    public ChannelHandler getHandler() {
        return handler;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public NodeData getNode(Identity id) {
        return nodes.get(id);
    }

    public Collection<? extends NodeData> getNodes() {
        return nodes.values();
    }

    /**
     * @return
     */
    public WireSecurity getWireSecurity() {
        return wireSecurity;
    }

    public String nodesToString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Nodes Table\n===========\n");
        Iterator<Map.Entry<Identity, NodeData>> iter = nodes.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Identity, NodeData> entry = iter.next();
            builder.append(entry.getKey() + "--->" + entry.getValue());
        }
        return builder.toString();
    }

    @Override
    public boolean receiveHeartbeat(Heartbeat hb) {
        NodeData nodeData = nodes.get(hb.getSender());
        if (nodeData != null) {
            nodeData.heartbeat(hb);
            return false;
        }
        nodeData = createNode(hb);
        addNode(hb, nodeData);
        return false;
    }

    public void removeNode(NodeData nodeData) {
        nodes.remove(nodeData.getIdentity());
    }

    public synchronized void setTiming(long interval, long timeout) {

        /** set local timers **/
        // checkPeriod = interval;
        // expirePeriod = interval * timeout;
        if (task != null) {
            task.cancel(true);
        }
        timer.schedule(getTask(), interval, TimeUnit.MILLISECONDS);

        /** set node's timers **/
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            iter.next().setTiming(interval, timeout);
        }
    }

    @PostConstruct
    public void start() throws IOException {
        handler.start();
        deploy();
    }

    public synchronized void symPartition(BitView partition) {
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            iter.next().setIgnoringSymPartition(globalView, partition);
        }
    }

    @PreDestroy
    public void terminate() {
        handler.terminate();
        if (task != null) {
            task.cancel(true);
        }
        clearNodes();
    }

    private synchronized Runnable getTask() {
        return new Runnable() {
            @Override
            public void run() {
                checkNodes();
            }
        };
    }

    protected synchronized void addNode(Heartbeat hb, NodeData nodeData) {
        nodes.put(hb.getSender(), nodeData);
        globalView.add(hb.getSender());
    }

    protected NodeData createNode(Heartbeat hb) {
        NodeData nodeData;
        nodeData = new NodeData(hb, this);
        return nodeData;
    }
}
