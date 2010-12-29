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

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.test.node.TestMgr;
import org.smartfrog.services.anubis.partition.util.Identity;

public class IntervalExec extends Thread {

    private ConnectionSet connectionSet = null;
    private long heartbeatTime = 0;

    private long interval = 0;
    private long lastCheckTime = 0;
    private Logger log = Logger.getLogger(this.getClass().toString()); // Use
                                                                       // asynch
                                                                       // wrapper...

    private Identity me = null;
    private Random random = null;

    private boolean running = false;
    private boolean stabalizing = false;

    private long stabilityTime = 0;
    private boolean testable = false;
    private TestMgr testManager = null;

    public IntervalExec(Identity id, ConnectionSet cs, long i) {
        super("Anubis: Interval Executive (node " + id.id + ")");
        me = id;
        connectionSet = cs;
        interval = i;
        setPriority(MAX_PRIORITY);
        random = new Random(System.currentTimeMillis() + 100 * me.id);
    }

    private void checkSleepDelays(long timenow, long stabilityTime,
                                  long heartbeatTime) {
        long earliest;
        long delay;

        if (timenow < lastCheckTime) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE,
                        "IntervalExec observed a system time adjustment - time has gone backwards during the sleep interval");
            }
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
        buffer.append(super.getName()).append(" ............................ ").setLength(30);
        buffer.append(super.isAlive() ? ".. is Alive " : ".. is Dead ");
        buffer.append(running ? ".. running    " : ".. terminated ");
        return buffer.toString();
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

    public void registerTestMgr(TestMgr testManager) {
        this.testManager = testManager;
        testable = true;
    }

    /**
     * Connection set periodically sends heartbeat messages.
     */
    @Override
    public void run() {
        running = true;

        long timenow = System.currentTimeMillis();
        heartbeatTime = timenow + random.nextInt((int) interval);
        lastCheckTime = timenow;
        synchronized (connectionSet) {

            while (running) {

                /**
                 * enter a sleep interval. Normally this is the regular
                 * heartbeat interval as defined by wakeup. If we are stablizing
                 * and the stability period ends before the next heartbeat
                 * interval then wake up at the stability interval end as
                 * defined by stabilityTime.
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
                     * report check (i.e. convergence time for the partition)
                     * can be sent out on the heartbeat
                     */
                    connectionSet.viewChangeCheck(timenow);

                    /**
                     * send a new heartbeat from this node - note comments about
                     * doing the viewChangeCheck first (above).
                     */
                    connectionSet.sendHeartbeat(timenow);

                    /**
                     * set next heartbeatTime time
                     */
                    heartbeatTime = nextHeartbeatTime(timenow, heartbeatTime);

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

            }
        }
    }

    public void setInterval(long interval) {
        this.interval = interval;
        heartbeatTime = System.currentTimeMillis()
                        + random.nextInt((int) interval);
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
        stabalizing = true;
        stabilityTime = s;
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

    /**
     * stops the connection set including shutdown on multicast communication
     * and ending the current wait period.
     */
    public void terminate() {
        synchronized (connectionSet) {
            running = false;
            connectionSet.notify();
        }
    }

}
