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
package org.smartfrog.services.anubis.locator;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.locator.msg.RegisterMsg;
import org.smartfrog.services.anubis.locator.registers.GlobalRegisterImpl;
import org.smartfrog.services.anubis.locator.registers.LocalRegisterImpl;
import org.smartfrog.services.anubis.locator.registers.StabilityQueue;
import org.smartfrog.services.anubis.locator.util.ActiveTimeQueue;
import org.smartfrog.services.anubis.partition.Partition;
import org.smartfrog.services.anubis.partition.PartitionNotification;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;

import com.hellblazer.anubis.annotations.Deployed;

public class Locator implements PartitionNotification, AnubisLocator {

    private class InstanceGenerator {
        private long counter = 0;

        public String instance() {
            return identity.id + "/" + identity.epoch + "/"
                   + Long.toString(counter++);
        }
    }

    public ThreadLocal<Object> callingThread = new ThreadLocal<Object>();
    public GlobalRegisterImpl global = null; // public for debug
    public LocalRegisterImpl local = null; // public for debug
    public Integer me = null; // public for debug
    private long heartbeatInterval = 0;
    private long heartbeatTimeout = 0;
    private InstanceGenerator instanceGenerator = new InstanceGenerator();
    private Integer leader = null;
    private Map<Integer, MessageConnection> links = new HashMap<Integer, MessageConnection>();
    private Logger log = Logger.getLogger(Locator.class.getCanonicalName());
    private long maxTransDelay;
    private Identity identity = null;
    private Partition partition = null;
    private Random random;

    private StabilityQueue stabilityQueue = new StabilityQueue() {
        @Override
        public void doit(View v, int l) {
            partitionNotificationImpl(v, l);
        }
    };
    private boolean stable = false;
    private ActiveTimeQueue timers = null; // public for debug

    /**
     * Deliver a request to the locator.
     * 
     * @param msg
     */
    private void deliverRequest(RegisterMsg msg) {
        if (msg.register == RegisterMsg.GlobalRegister) {
            global.deliverRequest(msg);
        } else {
            local.deliverRequest(msg);
        }
    }

    @Deployed
    public void deployed() {
        me = Integer.valueOf(identity.id);
        maxTransDelay = heartbeatTimeout * heartbeatInterval;
        timers = new ActiveTimeQueue("Anubis: Locator timers (node " + me + ")");
        global = new GlobalRegisterImpl(identity, this);
        local = new LocalRegisterImpl(identity, this);
        random = new Random(System.currentTimeMillis() + 1966 * me.longValue());

        global.start();
        local.start();
        timers.start();
        stabilityQueue.start();
        partition.register(this);
    }

    @Override
    public void deregisterListener(AnubisListener listener) {
        local.deregisterListener(listener);
        // don't set timers to null in listener
    }

    @Override
    public void deregisterProvider(AnubisProvider provider) {
        local.deregisterProvider(provider);
    }

    /**
     * deregisterStability deregisters a stability notification iterface.
     */
    @Override
    public void deregisterStability(AnubisStability stability) {
        local.deregisterStability(stability);
    }

    /**
     * Drop connections to nodes that are not in the view - they will be broken.
     * 
     * @param v
     */
    private void dropBrokenConnections(View v) {
        synchronized (links) {
            Iterator<Map.Entry<Integer, MessageConnection>> iter = links.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer, MessageConnection> entry = iter.next();
                if (!v.contains(entry.getKey().intValue())) {
                    entry.getValue().disconnect();
                    iter.remove();
                }
            }
        }
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

    @Override
    public long getmaxDelay() {
        return maxTransDelay;
    }

    public Partition getPartition() {
        return partition;
    }

    @Override
    public ActiveTimeQueue getTimeQueue() {
        return timers;
    }

    @Override
    public void newProviderValue(AnubisProvider provider) {
        local.newProviderValue(provider);
    }

    /**
     * PartitionNotification interface. delivery of a message from the partition
     * transport
     * 
     * @param obj
     *            - the message
     * @param sender
     *            - the sending node
     * @param time
     *            - the time sent
     */
    @Override
    public void objectNotification(Object obj, int sender, long time) {

        /**
         * As a sanity check see that it is a valid message type. Then pass to
         * the approriate register.
         */
        if (obj instanceof RegisterMsg) {
            deliverRequest((RegisterMsg) obj);
        } else if (log.isLoggable(Level.SEVERE)) {
            log.severe("Locator received un-recognised message " + obj
                       + " in objectNotificaiton");
        }
    }

    /**
     * This is a temporary fix for a deadlock bug. The implementation of links
     * maintenance (calls to send(), clearConnections() and
     * dropBrokenConnections()) is brain dead in that it can cause a deadlock
     * because upcoming notifications from the partitionManager may deadlock
     * with downward send() invocations from the registers. Making
     * partitionNotification() asynchronous by queueing the requests will avoid
     * the deadlock. Later the implementation of request servicing and link
     * maintenance should be examined.
     * 
     * @param view
     * @param leader
     */
    @Override
    public void partitionNotification(View view, int leader) {
        stabilityQueue.put(view, leader);
    }

    /**
     * PartitionNotification interface
     */
    public void partitionNotificationImpl(View view, int leader) {

        /**
         * Keep record of the current stability and which node is leader. The
         * leader holds the active global register.
         */
        stable = view.isStable();
        this.leader = Integer.valueOf(leader);

        /**
         * The view will only be the same or larger when a stable report is
         * received. If it was stable already it may have jumped to a larger
         * view that has become stable. If it was unstable, then again the new
         * one will be the same or larger. Accordingly, we can not lose any
         * nodes in becoming stable, however, a different node may have over as
         * leader - so the global may have moved. So......
         * 
         * ....if the global has changed we need to start using the new one.
         */
        if (view.isStable()) {

            /**
             * No point in putting jitter in at the global because it doesn't
             * send messages at stability.
             */
            global.stable(leader);

            /**
             * Introduce jitter to prevent the nodes all hitting the leader at
             * the same time. This should really have a max delay proportional
             * to the heartbeat interval.
             */
            try {
                wait(random.nextInt(1000));
            } catch (Exception ex) {
            }
            local.stable(leader, view.getTimeStamp());
        }

        /**
         * If a view is unstable it may be the same size as previously (in the
         * case that a node has been added) or it may be smaller (in the case
         * that a node has been removed). If the view shrinks all nodes will
         * become unstable. Accordingly, some providers may have become
         * inaccessible (absent) if they are on nodes that have vanished. Also
         * the global register may have been lost. So....
         * 
         * ....check if the global has changed and check for lost providers.
         */
        else {
            global.unstable(leader);
            local.unstable(view);
            dropBrokenConnections(view);
        }
    }

    @Override
    public void registerListener(AnubisListener listener) {
        listener.setTimerQueue(timers);
        local.registerListener(listener);
    }

    /**
     * AnubisLocator interface
     * 
     */
    @Override
    public void registerProvider(AnubisProvider provider) {
        /**
         * set the time of registration and create an instance (UID) for this
         * provider
         */
        provider.setAnubisData(this, System.currentTimeMillis(),
                               instanceGenerator.instance());
        local.registerProvider(provider);
    }

    /**
     * registerStability registers an interface for stability notifications.
     * This interface is called to inform the user when the local partition
     * becomes stable or unstable.
     */
    @Override
    public void registerStability(AnubisStability stability) {
        stability.setTimerQueue(timers);
        local.registerStability(stability);
    }

    /**
     * The locator manages connections between nodes. Connections are created on
     * demand in the send method. If the recipient drops out of the partition
     * during the connect() call we will get a null connection - in that case
     * just do nothing.
     * 
     * @param obj
     * @param node
     */
    private void send(Object obj, Integer node) {
        synchronized (links) {
            MessageConnection con = links.get(node);
            if (con == null) {
                con = partition.connect(node.intValue());
                if (con == null) {
                    return;
                }
                links.put(node, con);
            }
            con.sendObject(obj);
        }
    }

    /**
     * Handle deliver of messages to the global register. If the partition is
     * not stable the message will be dropped. Access to the global register is
     * suspended during periods of instability because it may move. This
     * constraint will be relaxed in a later version. If the leader is this node
     * then this nodes global register is the active one, so bypass the comms
     * and deliver directly to the global register.
     * 
     * @param msg
     */
    public void sendToGlobal(RegisterMsg msg) {
        if (stable) {
            if (leader.equals(me)) {

                global.deliverRequest(msg);

            } else {

                send(msg, leader);

            }
        } else {

            if (log.isLoggable(Level.INFO)) {
                log.info("Due to instability I am _NOT_ Sending " + msg
                         + " to global register");
            }

        }
    }

    /**
     * Handle delivery of messages to local registers. Messages are delivered to
     * peer local registers at any time, regardless of partition stability. If
     * the target node is this one then bypass the communications and deliver
     * directly to the local register.
     * 
     * @param msg
     * @param node
     */
    public void sendToLocal(RegisterMsg msg, Integer node) {

        if (node.equals(me)) {

            local.deliverRequest(msg);

        } else {

            send(msg, node);
        }
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public void setHeartbeatTimeout(long heartbeatTimeout) {
        this.heartbeatTimeout = heartbeatTimeout;
    }

    public void setIdentity(Identity myId) {
        identity = myId;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    @PreDestroy
    public void terminate() {
        if (log.isLoggable(Level.INFO)) {
            log.info("Terminating Locator");
        }
        stabilityQueue.terminate();
        global.terminate();
        local.terminate();
        timers.terminate();
        log = null;
    }

}
