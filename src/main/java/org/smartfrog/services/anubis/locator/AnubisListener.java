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

/**
 * <p>Title: </p>
 * <p>Description: This is an abstract base class for the listener object. The
 *                 user extends this class to implement a listener that may then
 *                 be registered with the locator. The name assigned to the
 *                 listener is the name of the associated provider. The locator
 *                 will call the methods of this class to indicate the status of
 *                 the named provider.</p>
 *
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.locator.names.ProviderInstance;
import org.smartfrog.services.anubis.locator.util.ActiveTimeQueue;
import org.smartfrog.services.anubis.locator.util.TimeoutErrorLogger;

abstract public class AnubisListener {
    private long mostRecentChange = -1;
    /**
     * The name of the provider that this listener listens for.
     */
    private String name;
    private Map<String, AnubisValue> values = new HashMap<String, AnubisValue>();
    protected Logger log = Logger.getLogger(this.getClass().toString());;
    protected ActiveTimeQueue timers;

    public AnubisListener(String n) {
        name = n;
    }

    /**
     * This is a factory method for creating AnubisValue objects. An AnubisValue
     * object is an object held by the user that the Anubis locator uses to
     * indicate changes in values.
     * 
     * The user should over-ride this method to create the users own sub-class
     * of the AnubisValue class.
     * 
     * @param i
     * @return AnubisValue
     */
    public AnubisValue createValue(ProviderInstance i) {
        return new AnubisValue(i);
    }

    public synchronized String getName() {
        return name;
    }

    public synchronized long getUpdateTime() {
        return mostRecentChange;
    }

    abstract public void newValue(AnubisValue value);

    public synchronized void newValue(ProviderInstance i) {
        AnubisValue v;
        if (values.containsKey(i.instance)) {
            v = values.get(i.instance);
            v.set(i.time, i.value);
        } else {
            v = createValue(i);
            values.put(i.instance, v);
        }
        setTime(i.time);
        safeNewValue(v);
    }

    abstract public void removeValue(AnubisValue value);

    public synchronized void removeValue(ProviderInstance i) {
        AnubisValue v = values.remove(i.instance);
        if (v != null) {
            setTime(i.time);
            v.set(i.time, ValueData.nullValue());
            safeRemoveValue(v);
        }
    }

    public synchronized void removeValue(ProviderInstance i, long time) {
        AnubisValue v = values.remove(i.instance);
        if (v != null) {
            setTime(time);
            v.set(time, ValueData.nullValue());
            safeRemoveValue(v);
        }
    }

    /**
     * This method will invoke user code in the listener. It is timed, logs
     * timeliness errors and catches Throwables.
     * 
     * @param listener
     */
    private void safeNewValue(AnubisValue v) {
        long timein = System.currentTimeMillis();
        long timeout = 0;

        TimeoutErrorLogger timeoutErrorLogger = new TimeoutErrorLogger(
                                                                       log,
                                                                       "User API Upcall took >200ms in newValue(p) where p="
                                                                               + v);
        timers.add(timeoutErrorLogger, (timein + 200));
        try {
            newValue(v);
        } catch (Throwable ex) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE,
                        "User API Upcall threw Throwable in newValue(p) where p="
                                + v, ex);
            }
        }
        timeout = System.currentTimeMillis();
        timers.remove(timeoutErrorLogger);
        if (log.isLoggable(Level.FINER)) {
            log.finer("User API Upcall took " + (timeout - timein)
                      + "ms in newValue(p) where p=" + v);
        }
    }

    /**
     * This method will invoke user code in the listener. It is timed, logs
     * timeliness errors and catches Throwables.
     * 
     * @param listener
     */
    private void safeRemoveValue(AnubisValue v) {
        long timein = System.currentTimeMillis();
        long timeout = 0;

        TimeoutErrorLogger timeoutErrorLogger = new TimeoutErrorLogger(
                                                                       log,
                                                                       "User API Upcall took >200ms in removeValue(p) where p="
                                                                               + v);
        timers.add(timeoutErrorLogger, (timein + 200));
        try {
            removeValue(v);
        } catch (Throwable ex) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE,
                        "User API Upcall threw Throwable in removeValue(p) where p="
                                + v, ex);
            }
        }
        timeout = System.currentTimeMillis();
        timers.remove(timeoutErrorLogger);
        if (log.isLoggable(Level.FINER)) {
            log.finer("User API Upcall took " + (timeout - timein)
                      + "ms in removeValue(p) where p=" + v);
        }
    }

    private void setTime(long t) {
        if (mostRecentChange < t) {
            mostRecentChange = t;
        }
    }

    public void setTimerQueue(ActiveTimeQueue queue) {
        timers = queue;
    }

    public synchronized int size() {
        return values.size();
    }

    @Override
    public String toString() {
        String ret = "Listener " + getName() + "=[size=" + size()
                     + ", mostRecentUpdate=" + getUpdateTime() + ", values=[";
        for (AnubisValue value : values.values()) {
            ret += value.toString();
        }
        ret += "]";
        return ret;
    }

    public synchronized Collection<AnubisValue> values() {
        return values.values();
    }

}
