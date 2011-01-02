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
package org.smartfrog.services.anubis.partition;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.Anubis;
import org.smartfrog.services.anubis.locator.util.ActiveTimeQueue;
import org.smartfrog.services.anubis.locator.util.TimeQueueElement;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.PartitionProtocol;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;

import com.hellblazer.anubis.annotations.Deployed;

public class PartitionManager implements Partition {

    static final int UNDEFINED_LEADER = -1;

    Identity identity = null;
    Logger log = Logger.getLogger(PartitionManager.class.getCanonicalName()); // TODO
                                                                              // Need
                                                                              // to
                                                                              // wrap
                                                                              // Async
    Set<PartitionNotification> notificationSet = new CopyOnWriteArraySet<PartitionNotification>();
    int notifiedLeader = UNDEFINED_LEADER;
    View notifiedView = null;
    PartitionProtocol partitionProtocol = null;
    boolean terminated = false;
    ActiveTimeQueue timer = null;

    @Override
    public MessageConnection connect(int node) {
        return partitionProtocol.connect(node);
    }

    @Deployed
    public synchronized void deployed() {
        timer = new ActiveTimeQueue("Anubis: Partition Manager timers (node "
                                    + identity.id + ")");
        notifiedView = BitView.create(identity, identity.epoch);
        notifiedLeader = identity.id;
    }

    @Override
    public synchronized void deregister(PartitionNotification pn) {
        notificationSet.remove(pn);
    }

    @Override
    public int getId() {
        return identity.id;
    }

    public Identity getIdentity() {
        return identity;
    }

    @Override
    public InetAddress getNodeAddress(int node) {
        return partitionProtocol.getNodeAddress(node);
    }

    public PartitionProtocol getPartitionProtocol() {
        return partitionProtocol;
    }

    @Override
    public synchronized Status getStatus() {
        return new Status(notifiedView, notifiedLeader);
    }

    public synchronized void notify(View view, int leader) {

        if (view.isStable() && leader == identity.id
            && notifiedLeader != leader && notifiedLeader != UNDEFINED_LEADER) {
            log.severe("Leader changed to me on stabalization, old leader = "
                       + notifiedLeader + ", new leader = " + leader
                       + ", view = " + view);
        }

        notifiedView = new BitView(view);
        notifiedLeader = leader;
        for (PartitionNotification notification : notificationSet) {
            safePartitionNotification(notification, notifiedView,
                                      notifiedLeader);
        }
    }

    public synchronized void receiveObject(Object obj, int sender, long time) {
        if (terminated) {
            return;
        }
        for (PartitionNotification notification : notificationSet) {
            safeObjectNotification(notification, obj, sender, time);
        }
    }

    @Override
    public synchronized void register(PartitionNotification pn) {
        notificationSet.add(pn);
    }

    /**
     * This method will invoke user code in the listener. It is timed, logs
     * timeliness severes and catches Throwables.
     * 
     * @param listener
     */
    private void safeObjectNotification(PartitionNotification pn, Object obj,
                                        int sender, long time) {
        long timein = System.currentTimeMillis();
        long timeout = 0;
        class TimeoutErrorLogger extends TimeQueueElement {
            Object obj;
            int sender;
            long time;

            TimeoutErrorLogger(Object o, int s, long t) {
                obj = o;
                sender = s;
                time = t;
            }

            @Override
            public void expired() {
                if (log.isLoggable(Level.SEVERE)) {
                    log.severe("User API Upcall took >200ms in "
                               + "objectNotification(obj, sender, time) where obj="
                               + obj + ", sender=" + sender + ", time=" + time);
                }
            }
        }
        TimeoutErrorLogger timeoutErrorLogger = new TimeoutErrorLogger(obj,
                                                                       sender,
                                                                       time);

        timer.add(timeoutErrorLogger, (timein + 200));
        try {
            pn.objectNotification(obj, sender, time);
        } catch (Throwable ex) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE,
                        "User API Upcall threw Throwable in "
                                + "objectNotification(obj, sender, time) where obj="
                                + obj + ", sender=" + sender + ", time=" + time,
                        ex);
            }
        }
        timeout = System.currentTimeMillis();
        timer.remove(timeoutErrorLogger);
        if (log.isLoggable(Level.FINER)) {
            log.finer("User API Upcall took "
                      + (timeout - timein)
                      + "ms in objectNotification(obj, sender, time) where obj="
                      + obj + ", sender=" + sender + ", time=" + time);
        }
    }

    /**
     * This method will invoke user code in the listener. It is timed, logs
     * timeliness severes and catches Throwables.
     * 
     * @param listener
     */
    private void safePartitionNotification(PartitionNotification pn, View view,
                                           int leader) {
        long timein = System.currentTimeMillis();
        long timeout = 0;
        class TimeoutErrorLogger extends TimeQueueElement {
            int leader;
            View view;

            TimeoutErrorLogger(View v, int l) {
                view = v;
                leader = l;
            }

            @Override
            public void expired() {
                if (log.isLoggable(Level.SEVERE)) {
                    log.severe("User API Upcall took >200ms in "
                               + "partitionNotification(view, leader) where view="
                               + view + ", leader=" + leader);
                }
            }
        }
        TimeoutErrorLogger timeoutErrorLogger = new TimeoutErrorLogger(view,
                                                                       leader);

        timer.add(timeoutErrorLogger, (timein + 200));
        try {
            pn.partitionNotification(view, leader);
        } catch (Throwable ex) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE,
                        "User API Upcall threw Throwable in "
                                + "partitionNotification(view, leader) where view="
                                + view + ", leader=" + leader, ex);
            }
        }
        timeout = System.currentTimeMillis();
        timer.remove(timeoutErrorLogger);
        if (log.isLoggable(Level.FINER)) {
            log.finer("User API Upcall took " + (timeout - timein)
                      + "ms in partitionNotification(view, leader) where view="
                      + view + ", leader=" + leader);
        }
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public void setPartitionProtocol(PartitionProtocol partitionProtocol) {
        this.partitionProtocol = partitionProtocol;
    }

    @PostConstruct
    public synchronized void start() {
        timer.start();

        if (log.isLoggable(Level.INFO)) {
            log.info("Started partition manager at " + identity + " "
                     + Anubis.version);
        }
    }

    @PreDestroy
    public synchronized void terminate() {
        if (log.isLoggable(Level.INFO)) {
            log.info("Terminating partition manager at " + identity);
        }
        timer.terminate();
        terminated = true;
    }

}
