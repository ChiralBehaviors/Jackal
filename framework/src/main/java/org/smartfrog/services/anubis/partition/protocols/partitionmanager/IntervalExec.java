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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.util.Identity;

import com.hellblazer.jackal.partition.test.node.Controller;

public class IntervalExec extends Thread {

    private static final Logger log           = Logger.getLogger(IntervalExec.class.getCanonicalName());

    private final ConnectionSet connectionSet;
    private long                heartbeatTime = 0;
    private volatile long       interval      = 0;
    private long                lastCheckTime = 0;
    private volatile boolean    running       = false;
    private volatile long       stabilityTime = 0;
    private boolean             stabilizing   = false;
    private volatile Controller controller    = null;

    public IntervalExec(final Identity id, ConnectionSet cs, long i) {
        super("Anubis: Interval Executive (node " + id.id + ")");
        connectionSet = cs;
        interval = i;
        setPriority(MAX_PRIORITY);
        setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                log.log(Level.WARNING,
                        "Uncaught exception on interval exec node " + id.id, e);
            }
        });
    }

    /**
     * prevent the interval executive from checking for stability times
     */
    public void clearStability() {
        stabilizing = false;
        stabilityTime = 0;
    }

    /**
     * returns an string representing the status of this thread
     */
    public String getThreadStatusString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.getName()).append(" ............................ ").setLength(30);
        buffer.append(super.isAlive() ? ".. is Alive " : ".. is Dead ");
        buffer.append(running ? ".. running    " : ".. terminated ");
        return buffer.toString();
    }

    public void registerController(Controller controller) {
        this.controller = controller;
    }

    /**
     * Connection set periodically sends heartbeat messages.
     */
    @Override
    public void run() {
        running = true;

        long timenow = System.currentTimeMillis();
        heartbeatTime = timenow + interval; // random.nextInt((int) interval);
        lastCheckTime = timenow;
        synchronized (connectionSet) {
            while (running) {
                try {
                    /**
                     * enter a sleep interval. Normally this is the regular
                     * heartbeat interval as defined by wakeup. If we are
                     * stablizing and the stability period ends before the next
                     * heartbeat interval then wake up at the stability interval
                     * end as defined by stabilityTime.
                     */
                    timenow = stabilizing && heartbeatTime > stabilityTime ? sleepInterval(timenow,
                                                                                           stabilityTime)
                                                                          : sleepInterval(timenow,
                                                                                          heartbeatTime);

                    checkSleepDelays(timenow, stabilityTime, heartbeatTime);

                    /**
                     * Operations performed once per heartbeat period only
                     */
                    if (timenow >= heartbeatTime) {

                        /**
                         * If controlled then produce delay info
                         */
                        if (controller != null) {
                            controller.schedulingInfo(timenow, timenow
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
                    if (stabilizing && timenow >= stabilityTime) {

                        /**
                         * If controlled then produce delay info
                         */
                        if (controller != null) {
                            controller.schedulingInfo(timenow, timenow
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
        heartbeatTime = System.currentTimeMillis() + interval; // random.nextInt((int) interval);
        interrupt();
    }

    /**
     * set the interval executive to wake up and check stability times as well
     * as regular heartbeat intervals.
     * 
     * @param s
     *            - the time that stability is expected.
     */
    public void setStability(long s) {
        stabilizing = true;
        stabilityTime = s;
    }

    /**
     * stops the connection set including shutdown on multicast communication
     * and ending the current wait period.
     */
    public void terminate() {
        running = false;
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

        if (!stabilizing || stabilityTime > heartbeatTime) {
            earliest = heartbeatTime;
        } else {
            earliest = stabilityTime;
        }

        delay = timenow - earliest;

        if (delay > 15000) {
            log.severe("IntervalExec may have observed a system time adjustment - overslept by "
                       + delay
                       + "ms - this is excessive for a scheduling delay and is most likely to be a system time adjustment");
            if (log.isLoggable(Level.FINE)) {
                log.fine(String.format("timenow: %s, delay:%s, stabilityTime: %s, heartbeatTime: %s",
                                       timenow, delay, stabilityTime,
                                       heartbeatTime));
            }
        } else if (delay > 200) {
            if (log.isLoggable(Level.INFO)) {
                log.info("IntervalExec overslept by " + delay
                         + "ms - this is a scheduling delay or time adjustment");
            }
            if (log.isLoggable(Level.FINE)) {
                log.fine(String.format("timenow: %s, delay:%s, stabilityTime: %s, heartbeatTime: %s",
                                       timenow, delay, stabilityTime,
                                       heartbeatTime));
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
            long delay = wakeup - timenow;
            if (delay > 0) {
                connectionSet.wait(delay);
            }
        } catch (InterruptedException ex) {
        }
        return System.currentTimeMillis();
    }

}
