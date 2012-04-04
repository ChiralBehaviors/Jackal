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

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class AnubisStability {

    private static final Logger      log           = LoggerFactory.getLogger(AnubisStability.class.getCanonicalName());
    private long                     lastTimeRef   = -1;
    private boolean                  lastWasStable = true;
    private ScheduledExecutorService timers        = null;

    public synchronized boolean isStable() {
        return lastWasStable;
    }

    public void notifyStability(boolean isStable, long timeRef) {

        /**
         * if the stability has changed notify the user
         */
        if (isStable != lastWasStable) {
            lastWasStable = isStable;
            lastTimeRef = timeRef;
            safeStability(isStable, timeRef);
            return;
        }

        /**
         * if the stability has not changed and is unstable there is nothing new
         * to tell the user
         */
        if (!isStable) {
            return;
        }

        /**
         * if the stability has not changed but is stable and the time reference
         * has changed, tell the user
         */
        if (lastTimeRef != timeRef) {
            lastWasStable = isStable;
            lastTimeRef = timeRef;
            safeStability(isStable, timeRef);
            return;
        }
    }

    public void safeStability(final boolean isStable, final long timeRef) {

        long timein = System.currentTimeMillis();
        long timeout = 0;
        ScheduledFuture<?> task;
        try {
            task = timers.schedule(new Runnable() {

                @Override
                public void run() {
                    log.warn("User API Upcall took >200ms in stability(s,t) where s="
                             + isStable + ", t=" + timeRef);
                }
            }, 200, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException e) {
            log.trace("Rejecting stability notification due to shutdown");
            return;
        }
        try {
            stability(isStable, timeRef);
        } catch (Throwable ex) {
            log.error("User API Upcall threw Throwable in stability(s,t) where s="
                              + isStable + ", t=" + timeRef, ex);
        }
        timeout = System.currentTimeMillis();
        task.cancel(true);
        if (log.isTraceEnabled()) {
            log.trace("User API Upcall took " + (timeout - timein)
                      + "ms in stability(s,t) where s=" + isStable + ", t="
                      + timeRef);
        }

    }

    public void setTimer(ScheduledExecutorService t) {
        timers = t;
    }

    abstract public void stability(boolean isStabile, long timeRef);

    public synchronized long timeReference() {
        return lastTimeRef;
    }

    @Override
    public String toString() {
        return "[Stability notification interface]";
    }
}
