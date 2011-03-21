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

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.PartitionProtocol;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;

public class PartitionManager implements Partition {

    static final int                         UNDEFINED_LEADER = -1;
    private static final Logger              log              = Logger.getLogger(PartitionManager.class.getCanonicalName()); //TODO Need to wrap Async
    
    private final Identity                   identity;
    private final Set<PartitionNotification> notificationSet  = new HashSet<PartitionNotification>();
    private int                              notifiedLeader   = UNDEFINED_LEADER;
    private View                             notifiedView;
    private PartitionProtocol                partitionProtocol;
    private boolean                          terminated       = false;
    private final ScheduledExecutorService   timer;

    public PartitionManager(Identity id) {
        identity = id;
        notifiedView = BitView.create(identity, identity.epoch);
        notifiedLeader = identity.id;
        timer = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "Anubis: Partition Manager timers (node "
                                                   + identity.id + ")");
                daemon.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.WARNING, "Uncaught exceptiion", e);
                    }
                });
                daemon.setDaemon(true);
                daemon.setPriority(Thread.MAX_PRIORITY);
                return daemon;
            }
        });
    }

    @Override
    public MessageConnection connect(int node) {
        return partitionProtocol.connect(node);
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
        @SuppressWarnings("unchecked")
        Iterator<PartitionNotification> iter = ((Set<PartitionNotification>) ((HashSet<PartitionNotification>) notificationSet).clone()).iterator();
        while (iter.hasNext()) {
            safePartitionNotification(iter.next(), notifiedView, notifiedLeader);
        }
    }

    public synchronized void receiveObject(Object obj, int sender, long time) {
        if (terminated) {
            return;
        }
        @SuppressWarnings("unchecked")
        Iterator<PartitionNotification> iter = ((Set<PartitionNotification>) ((HashSet<PartitionNotification>) notificationSet).clone()).iterator();
        while (iter.hasNext()) {
            safeObjectNotification(iter.next(), obj, sender, time);
        }
    }

    @Override
    public synchronized void register(PartitionNotification pn) {
        notificationSet.add(pn);
    }

    public void setPartitionProtocol(PartitionProtocol partitionProtocol) {
        this.partitionProtocol = partitionProtocol;
    }

    @PreDestroy
    public synchronized void terminate() {
        if (log.isLoggable(Level.INFO)) {
            log.info("Terminating partition manager at " + identity);
        }
        timer.shutdownNow();
        terminated = true;
    }

    /**
     * This method will invoke user code in the listener. It is timed, logs
     * timeliness severes and catches Throwables.
     * 
     * @param listener
     */
    private void safeObjectNotification(PartitionNotification pn,
                                        final Object obj, final int sender,
                                        final long time) {
        long timein = System.currentTimeMillis();
        long timeout = 0;
        ScheduledFuture<?> task = timer.schedule(new Runnable() {

            @Override
            public void run() {
                log.severe("User API Upcall took >200ms in "
                           + "objectNotification(obj, sender, time) where obj="
                           + obj + ", sender=" + sender + ", time=" + time);
            }

        }, 200, TimeUnit.MILLISECONDS);

        try {
            pn.objectNotification(obj, sender, time);
        } catch (Throwable ex) {
            log.log(Level.SEVERE,
                    "User API Upcall threw Throwable in "
                            + "objectNotification(obj, sender, time) where obj="
                            + obj + ", sender=" + sender + ", time=" + time, ex);
        }
        timeout = System.currentTimeMillis();
        task.cancel(true);
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
    private void safePartitionNotification(PartitionNotification pn,
                                           final View view, final int leader) {
        long timein = System.currentTimeMillis();
        long timeout = 0;
        ScheduledFuture<?> task = timer.schedule(new Runnable() {

            @Override
            public void run() {
                log.severe("User API Upcall took >200ms in "
                           + "partitionNotification(view, leader) where view="
                           + view + ", leader=" + leader);
            }

        }, 200, TimeUnit.MILLISECONDS);

        try {
            pn.partitionNotification(view, leader);
        } catch (Throwable ex) {
            log.log(Level.SEVERE,
                    "User API Upcall threw Throwable in "
                            + "partitionNotification(view, leader) where view="
                            + view + ", leader=" + leader, ex);
        }
        timeout = System.currentTimeMillis();
        task.cancel(true);
        if (log.isLoggable(Level.FINER)) {
            log.finer("User API Upcall took " + (timeout - timein)
                      + "ms in partitionNotification(view, leader) where view="
                      + view + ", leader=" + leader);
        }
    }
}
