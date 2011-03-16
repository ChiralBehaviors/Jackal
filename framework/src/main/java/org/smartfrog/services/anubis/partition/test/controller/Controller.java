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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

import com.hellblazer.anubis.annotations.Deployed;

public class Controller implements HeartbeatReceiver {
    private long                      checkPeriod       = 1000;
    private long                      expirePeriod      = 10000;
    private BitView                   globalView        = new BitView();
    protected long                    heartbeatInterval = 1000;
    protected long                    heartbeatTimeout  = 2000;
    protected Identity                identity;
    protected Map<Identity, NodeData> nodes             = new HashMap<Identity, NodeData>();
    private TimerTask                 task;
    private Timer                     timer;

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
        long expireTime = timeNow - expirePeriod;
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            NodeData nodeData = iter.next();
            if (nodeData.olderThan(expireTime)) {
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

    protected void addNode(Heartbeat hb, NodeData nodeData) {
        nodes.put(hb.getSender(), nodeData);
        globalView.add(hb.getSender());
    }

    public synchronized void deliverObject(Object obj, NodeData node) {
        node.deliverObject(obj);
    }

    @Deployed
    public synchronized void deploy() throws IOException {
        timer.schedule(getTask(), checkPeriod, checkPeriod);
    }

    public synchronized void disconnectNode(NodeData node) {
        node.disconnected();
    }

    public long getCheckPeriod() {
        return checkPeriod;
    }

    public long getExpirePeriod() {
        return expirePeriod;
    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }

    public Identity getIdentity() {
        return identity;
    }

    public NodeData getNode(Identity id) {
        return nodes.get(id);
    }

    public Collection<? extends NodeData> getNodes() {
        return nodes.values();
    }

    public Timer getTimer() {
        return timer;
    }

    public String nodesToString() {
        String str = "Nodes Table\n===========\n";
        Iterator<Map.Entry<Identity, NodeData>> iter = nodes.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Identity, NodeData> entry = iter.next();
            str += entry.getKey() + "--->" + entry.getValue();
        }
        return str;
    }

    public void removeNode(NodeData nodeData) {
    }

    public void setCheckPeriod(long checkPeriod) {
        this.checkPeriod = checkPeriod;
    }

    public synchronized void setExpirePeriod(long expirePeriod) {
        this.expirePeriod = expirePeriod;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public void setHeartbeatTimeout(long heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public void setTimer(Timer timer) {
        this.timer = timer;
    }

    public synchronized void setTiming(long interval, long timeout) {

        /** set local timers **/
        checkPeriod = interval;
        expirePeriod = interval * timeout;
        if (task != null) {
            task.cancel();
        }
        timer.schedule(getTask(), checkPeriod, checkPeriod);

        /** set node's timers **/
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            iter.next().setTiming(interval, timeout);
        }
    }

    public synchronized void symPartition(BitView partition) {
        Iterator<NodeData> iter = nodes.values().iterator();
        while (iter.hasNext()) {
            iter.next().setIgnoringSymPartition(globalView, partition);
        }
    }

    @PreDestroy
    public synchronized void terminate() {
        if (task != null) {
            task.cancel();
        }
        clearNodes();
    }

    private synchronized TimerTask getTask() {
        task = new TimerTask() {

            @Override
            public void run() {
                checkNodes();
            }
        };
        return task;
    }

    protected NodeData createNode(Heartbeat hb) {
        NodeData nodeData;
        nodeData = new NodeData(hb, this);
        return nodeData;
    }
}
