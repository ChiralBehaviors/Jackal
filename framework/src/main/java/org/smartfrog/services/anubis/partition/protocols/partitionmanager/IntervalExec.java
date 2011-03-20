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
package org.smartfrog.services.anubis.partition.protocols.partitionmanager;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.test.node.TestMgr;
import org.smartfrog.services.anubis.partition.util.Identity;

public class IntervalExec {

    private static final Logger   log           = Logger.getLogger(IntervalExec.class.getCanonicalName()); // Use asynch wrapper...

    private final ConnectionSet   connectionSet;
    private volatile long         heartbeatTime = 0;
    private volatile long         interval      = 0;
    private volatile long         lastCheckTime = 0;
    private final Identity        me;
    private final Random          random;
    private final AtomicBoolean   running       = new AtomicBoolean();
    private final ExecutorService service;
    private volatile boolean      stabalizing   = false;
    private volatile long         stabilityTime = 0;
    private volatile boolean      testable      = false;
    private TestMgr               testManager   = null;
    private final String          threadName;

    public IntervalExec(Identity id, ConnectionSet cs, long i) {
        threadName = "Anubis: Interval Executive (node " + id.id + ")";
        service = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r, threadName);
                daemon.setPriority(Thread.MAX_PRIORITY);
                daemon.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.WARNING, "Uncaught exception", e);
                    }
                });
                daemon.setDaemon(true);
                return daemon;

            }

        });
        me = id;
        connectionSet = cs;
        interval = i;
        random = new Random(System.currentTimeMillis() + 100 * me.id);
    }

    /**
     * prevent the interval executive from checking for stability times
     */
    public void clearStability() {
        stabalizing = false;
        stabilityTime = 0;
    }

    /**
     * returns an string representing the status of this thread
     */
    public String getThreadStatusString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(threadName).append(" ............................ ").setLength(30);
        buffer.append(service.isShutdown() ? ".. is Dead " : ".. is Alive ");
        buffer.append(running.get() ? ".. running    " : ".. terminated ");
        return buffer.toString();
    }

    public void registerTestMgr(TestMgr testManager) {
        this.testManager = testManager;
        testable = true;
    }

    /**
     * Connection set periodically sends heartbeat messages.
     */
    private void heartbeatTask() {
        long timenow = System.currentTimeMillis();
        heartbeatTime = timenow + random.nextInt((int) interval);
        lastCheckTime = timenow;
        synchronized (connectionSet) {
            while (running.get()) {
                try {
                    /**
                     * enter a sleep interval. Normally this is the regular
                     * heartbeat interval as defined by wakeup. If we are
                     * stablizing and the stability period ends before the next
                     * heartbeat interval then wake up at the stability interval
                     * end as defined by stabilityTime.
                     */
                    timenow = stabalizing && heartbeatTime > stabilityTime ? sleepInterval(timenow,
                                                                                           stabilityTime)
                                                                          : sleepInterval(timenow,
                                                                                          heartbeatTime);

                    checkSleepDelays(timenow, stabilityTime, heartbeatTime);

                    /**
                     * Operations performed once per heartbeat period only
                     */
                    if (timenow >= heartbeatTime) {

                        /**
                         * If testing then produce delay info
                         */
                        if (testable) {
                            testManager.schedulingInfo(timenow, timenow
                                                                - heartbeatTime);
                        }

                        /**
                         * check the timeouts and cleanup
                         */
                        connectionSet.checkTimeouts(timenow);

                        /**
                         * viewChangeCheck - this is done before sending the
                         * heartbeat so that any information determined in the
                         * report check (i.e. convergence time for the
                         * partition) can be sent out on the heartbeat
                         */
                        connectionSet.viewChangeCheck(timenow);

                        /**
                         * send a new heartbeat from this node - note comments
                         * about doing the viewChangeCheck first (above).
                         */
                        connectionSet.sendHeartbeat(timenow);

                        /**
                         * set next heartbeatTime time
                         */
                        heartbeatTime = nextHeartbeatTime(timenow,
                                                          heartbeatTime);

                    }

                    /**
                     * check for stability - only done on a stability boundary
                     */
                    if (stabalizing && timenow >= stabilityTime) {

                        /**
                         * If testing then produce delay info
                         */
                        if (testable) {
                            testManager.schedulingInfo(timenow, timenow
                                                                - stabilityTime);
                        }

                        connectionSet.checkStability(timenow);
                    }
                } catch (Throwable e) {
                    log.log(Level.WARNING, "Error during interval maintenance",
                            e);
                }
            }
        }
    }

    public void setInterval(long interval) {
        this.interval = interval;
        heartbeatTime = System.currentTimeMillis()
                        + random.nextInt((int) interval);
        // interrupt();
    }

    /**
     * set the interval executive to wake up and check stability times as well
     * as regular heartbeat intervals.
     * 
     * @param s
     *            - the time that stability is expected.
     */
    public void setStability(long s) {
        stabalizing = true;
        stabilityTime = s;
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    heartbeatTask();
                }
            });
        }
    }

    /**
     * stops the connection set including shutdown on multicast communication
     * and ending the current wait period.
     */
    public void terminate() {
        if (running.compareAndSet(true, false)) {
            synchronized (connectionSet) {
                connectionSet.notify();
            }
        }
    }

    private void checkSleepDelays(long timenow, long stabilityTime,
                                  long heartbeatTime) {
        long earliest;
        long delay;

        if (timenow < lastCheckTime) {
            log.log(Level.SEVERE,
                    "IntervalExec observed a system time adjustment - time has gone backwards during the sleep interval");

        }

        lastCheckTime = timenow;

        if (!stabalizing || stabilityTime > heartbeatTime) {
            earliest = heartbeatTime;
        } else {
            earliest = stabilityTime;
        }

        delay = timenow - earliest;

        if (delay > 15000) {
            log.severe("IntervalExec may have observed a system time adjustment - overslept by "
                       + delay
                       + "ms - this is excessive for a scheduling delay and is most likely to be a system time adjustment");
        } else if (delay > 200) {
            if (log.isLoggable(Level.INFO)) {
                log.info("IntervalExec overslept by " + delay
                         + "ms - this is a scheduling delay or time adjustment");
            }
        }
    }

    /**
     * calculates next heartbeatTime. It is likely that the sleep will actually
     * wake up just after it is supposed to due to scheduling delays. If the
     * delay is particularly long the node may start to miss the heartbeat
     * interval all together. Make sure we go to the next appropriate regular
     * interval boundary, no matter what the delay.
     * 
     * @param timenow
     * @param wakeup
     * @return
     */
    private long nextHeartbeatTime(long timenow, long wakeup) {
        while (wakeup <= timenow) {
            wakeup += interval;
        }
        return wakeup;
    }

    /**
     * goes to sleep for a period of time determined by the difference between
     * the timenow and the wakeup time. returns the time that wait returns (due
     * to completing wait timeout, notify on terminate, or interrupt).
     * 
     * @param timenow
     * @param wakeup
     * @return
     */
    private long sleepInterval(long timenow, long wakeup) {
        try {
            connectionSet.wait(wakeup - timenow);
        } catch (InterruptedException ex) {
        }
        return System.currentTimeMillis();
    }

}
