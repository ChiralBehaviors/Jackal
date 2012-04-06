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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.locator.msg.RegisterMsg;
import org.smartfrog.services.anubis.locator.registers.GlobalRegisterImpl;
import org.smartfrog.services.anubis.locator.registers.LocalRegisterImpl;
import org.smartfrog.services.anubis.partition.Partition;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.PartitionNotification;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;

public class Locator implements PartitionNotification, AnubisLocator {

    private class InstanceGenerator {
        private long counter = 0;

        public String instance() {
            return identity.id + "/" + identity.epoch + "/"
                   + Long.toString(counter++);
        }
    }

    private static final Logger                   log               = LoggerFactory.getLogger(Locator.class.getCanonicalName());

    @SuppressWarnings("rawtypes")
    public final ThreadLocal                      callingThread     = new ThreadLocal();
    public final GlobalRegisterImpl               global;                                                                       // public for debug
    public final LocalRegisterImpl                local;                                                                        // public for debug
    public final Integer                          me;
    private final Identity                        identity;
    private final InstanceGenerator               instanceGenerator = new InstanceGenerator();
    private Integer                               leader            = null;
    private final Map<Integer, MessageConnection> links             = new HashMap<Integer, MessageConnection>();
    private final long                            maxTransDelay;
    private final Partition                       partition;
    private final AtomicBoolean                   stable            = new AtomicBoolean();
    private final ScheduledExecutorService        timers;

    public Locator(Identity partitionIdentity, PartitionManager partition,
                   long heartbeatInterval, long heartbeatTimeout) {
        identity = partitionIdentity;
        this.partition = partition;
        me = Integer.valueOf(identity.id);
        maxTransDelay = heartbeatTimeout * heartbeatInterval;
        global = new GlobalRegisterImpl(identity, this);
        local = new LocalRegisterImpl(identity, this);
        timers = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r, "Locator timers (node "
                                              + me + ")");
                daemon.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.warn("Uncaught exceptiion", e);
                    }
                });
                daemon.setDaemon(true);
                return daemon;
            }
        });
    }

    @PostConstruct
    public void deployed() {
        global.start();
        local.start();
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

    @Override
    public Identity getIdentity() {
        return identity;
    }

    @Override
    public long getmaxDelay() {
        return maxTransDelay;
    }

    @Override
    public ScheduledExecutorService getScheduler() {
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
        } else {
            if (log.isTraceEnabled()) {
                log.trace("Locator received un-recognised message " + obj
                          + " in objectNotificaiton");
            }
        }
    }

    /**
     * PartitionNotification interface
     * 
     * @param view
     * @param leader
     */
    @Override
    public void partitionNotification(final View view, final int leader) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Partition view: %s, leader: %s, on member %s",
                                    view, leader, me));
        }

        /**
         * Keep record of the current stability and which node is leader. The
         * leader holds the active global register.
         */
        stable.set(view.isStable());
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
        listener.setTimer(timers);
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
        stability.setTimer(timers);
        local.registerStability(stability);
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
        if (stable.get()) {
            if (leader.equals(me)) {
                global.deliverRequest(msg);
            } else {
                send(msg, leader);
            }
        } else {
            if (log.isInfoEnabled()) {
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

    @PreDestroy
    public void terminate() {
        if (log.isTraceEnabled()) {
            log.trace("Terminating Locator");
        }
        global.terminate();
        local.terminate();
        timers.shutdownNow();
    }

    /**
     * Remove all connections managed by the locator.
     */
    @SuppressWarnings("unused")
    private void clearConnections() {
        synchronized (links) {
            Iterator<MessageConnection> iter = links.values().iterator();
            while (iter.hasNext()) {
                iter.next().disconnect();
            }
            links.clear();
        }
    }

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
                Integer node = entry.getKey();
                if (!v.contains(node.intValue())) {
                    entry.getValue().disconnect();
                    iter.remove();
                }
            }
        }
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
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("Dropping message: %s on: %s destined for: %s, as there is no connection",
                                                obj, me, node));
                    }
                    return;
                }
                links.put(node, con);
            }
            if (log.isTraceEnabled()) {
                log.trace(String.format("Sending message: %s on: %s destined for: %s",
                                        obj, me, node));
            }
            con.sendObject(obj);
        }
    }

}
