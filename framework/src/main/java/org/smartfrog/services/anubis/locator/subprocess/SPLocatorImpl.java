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
package org.smartfrog.services.anubis.locator.subprocess;

import java.lang.Thread.UncaughtExceptionHandler;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.AnubisStability;
import org.smartfrog.services.anubis.partition.util.Identity;

public class SPLocatorImpl implements AnubisLocator, SPLocator {
    class LivenessChecker extends PeriodicTimer {
        LivenessChecker(long period) {
            super("SPLocator liveness checker", period);
        }

        @Override
        protected void act(long now) {
            checkLiveness(now);
        }
    }

    class Pinger extends PeriodicTimer {
        Pinger(long period) {
            super("SPLocator adapter pinger", period);
            // setPriority(MAX_PRIORITY);
        }

        @Override
        protected void act(long now) {
            pingAdapter();
        }
    }

    private static final Logger               log            = LoggerFactory.getLogger(SPLocatorImpl.class.getCanonicalName());
    private boolean                           isDeployed     = false;
    private boolean                           isRegistered   = false;
    private Object                            adapterMonitor = new Object();
    private SPLocatorAdapter                  adapter;
    private Set<AnubisProvider>               providers      = new HashSet<AnubisProvider>();
    private Map<AnubisListener, SPListener>   listeners      = new HashMap<AnubisListener, SPListener>();
    private Map<AnubisStability, SPStability> stabilities    = new HashMap<AnubisStability, SPStability>();
    private Liveness                          liveness;
    private LivenessChecker                   livenessChecker;
    private Pinger                            pinger;
    private ScheduledExecutorService          timers;
    private long                              maxTransDelay;
    private Logger                            syncLog        = LoggerFactory.getLogger(SPLocatorImpl.class.getCanonicalName());
    private Logger                            asyncLog       = syncLog;                                                        // TO Do: wrap with Async...
    private volatile boolean                  terminated     = false;
    private long                              period;
    private long                              timeout;

    public SPLocatorImpl() {
        timers = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "subprocess locator timers");
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

    public void deploy() throws RemoteException {
        try {
            liveness = new Liveness(timeout);
            // liveness.ping();
            pinger = new Pinger(period);
            livenessChecker = new LivenessChecker(period);
            maxTransDelay = period * timeout;

            synchronized (adapterMonitor) {
                isDeployed = true;
            }
        } catch (Exception ex) {
            RemoteException remoteException = new RemoteException();
            remoteException.initCause(ex);
            throw remoteException;
        }
    }

    @Override
    public void deregisterListener(AnubisListener listener) {

        if (terminated) {
            return;
        }

        if (!registered()) {
            return;
        }

        if (!listeners.containsKey(listener)) {
            return;
        }

        try {
            SPListener spListener = listeners.remove(listener);
            adapter.deregisterListener(this, spListener);
            // don't set timers to null in listener
        } catch (RemoteException ex) {
            syncLog.error("Failed to call adapter", ex);
            terminate();
        } catch (UnknownSPLocatorException ex) {
            syncLog.error("Adapter did not recognize me", ex);
            terminate();
        }
    }

    @Override
    public void deregisterProvider(AnubisProvider provider) {

        if (terminated) {
            return;
        }

        if (!registered()) {
            return;
        }

        if (!providers.contains(provider)) {
            return;
        }

        try {
            adapter.deregisterProvider(this, provider.getInstance());
            providers.remove(provider);
        } catch (RemoteException ex) {
            syncLog.error("Failed to call adapter", ex);
            terminate();
        } catch (UnknownSPLocatorException ex) {
            syncLog.error("Adapter did not recognise me", ex);
            terminate();
        }
    }

    @Override
    public void deregisterStability(AnubisStability stability) {

        if (terminated) {
            return;
        }

        if (!registered()) {
            return;
        }

        if (!stabilities.containsKey(stability)) {
            return;
        }

        try {
            adapter.deregisterStability(this, stabilities.remove(stability));
            // don't set timers to null in stability
        } catch (RemoteException ex) {
            syncLog.error("Failed to call adapter", ex);
            terminate();
        } catch (UnknownSPLocatorException ex) {
            syncLog.error("Adapter did not recognise me", ex);
            terminate();
        }
    }

    @Override
    public Identity getIdentity() {
        try {
            return adapter.getIdentity();
        } catch (RemoteException e) {
            throw new IllegalStateException(e);
        }
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

        if (terminated) {
            return;
        }

        if (!registered()) {
            return;
        }

        if (!providers.contains(provider)) {
            return;
        }

        try {
            adapter.newProviderValue(this, provider.getInstance(),
                                     provider.getValueData(),
                                     provider.getTime());
        } catch (RemoteException ex) {
            syncLog.error("Failed to call adapter", ex);
            terminate();
        } catch (UnknownSPLocatorException ex) {
            syncLog.error("Adapter did not recognise me", ex);
            terminate();
        }
    }

    @Override
    public void registerListener(AnubisListener listener) {

        if (terminated) {
            return;
        }

        if (!registered()) {
            return;
        }

        if (listeners.containsKey(listener)) {
            return;
        }

        try {
            listener.setTimer(timers);
            SPListener spListener = new SPListenerImpl(listener);
            adapter.registerListener(this, listener.getName(), spListener);
            listeners.put(listener, spListener);
        } catch (RemoteException ex) {
            syncLog.error("Failed to call adapter", ex);
            terminate();
        } catch (UnknownSPLocatorException ex) {
            syncLog.error("Adapter did not recognise me", ex);
            terminate();
        }
    }

    @Override
    public void registerProvider(AnubisProvider provider) {

        if (terminated) {
            return;
        }

        if (!registered()) {
            return;
        }

        if (providers.contains(provider)) {
            return;
        }

        try {
            SPProviderRegRet ret = adapter.registerProvider(this,
                                                            provider.getName(),
                                                            provider.getValueData());
            provider.setAnubisData(this, ret.time, ret.instance);
            providers.add(provider);
        } catch (RemoteException ex) {
            syncLog.error("Failed to call adapter", ex);
            terminate();
        } catch (UnknownSPLocatorException ex) {
            syncLog.error("Adapter did not recognise me", ex);
            terminate();
        }
    }

    @Override
    public void registerStability(AnubisStability stability) {

        if (terminated) {
            return;
        }

        if (!registered()) {
            return;
        }

        if (stabilities.containsKey(stability)) {
            return;
        }

        try {
            stability.setTimer(timers);
            SPStability spStability = new SPStabilityImpl(stability);
            adapter.registerStability(this, spStability);
            stabilities.put(stability, spStability);
        } catch (RemoteException ex) {
            syncLog.error("Failed to call adapter", ex);
            terminate();
        } catch (UnknownSPLocatorException ex) {
            syncLog.error("Adapter did not recognise me", ex);
            terminate();
        }
    }

    public void setAdapter(SPLocatorAdapter adapter) {
        this.adapter = adapter;
    }

    public void setPeriod(long period) {
        this.period = period;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @PostConstruct
    public void start() throws RemoteException {
        try {
            registered();
        } catch (Exception ex) {
            throw new RemoteException("Cannot register locator implementation",
                                      ex);
        }
        deploy();
    }

    /**
     * Note that on termination the SPLocator will deregister all the listeners
     * and providers it knows about, but it will not tell those listeners and
     * providers
     * 
     * @param status
     */
    @PreDestroy
    public void terminate() {
        terminated = true;
        deregister();
        if (timers != null) {
            timers.shutdownNow();
        }
    }

    private void checkLiveness(long now) {
        if (liveness.isNotTimely(now)) {
            syncLog.error("Failed timeliness at SPLocator end");
            terminate();
        }
    }

    private void deregister() {

        synchronized (adapterMonitor) {

            if (!isRegistered) {
                return;
            }

            try {

                adapter.deregisterSPLocator(this);

            } catch (RemoteException ex) {
                // failure is ok
            } catch (UnknownSPLocatorException ex) {
                // failure is ok
            }

            pinger.terminate();
            livenessChecker.terminate();
            providers.clear();
            listeners.clear();
        }
    }

    /**
     * Lineness methods. The pinger pings the adapter at regular intervals. The
     * adapter will check it is being pinged regularly and the liveness checker
     * will check the pings are happening.
     */
    private void pingAdapter() {
        try {
            adapter.livenessPing(this);
            liveness.ping();

        } catch (UnknownSPLocatorException ex) {

            if (asyncLog.isWarnEnabled()) {
                asyncLog.warn("Unknown Locator", ex);
            }
            pinger.terminate();

        } catch (RemoteException ex) {

            if (asyncLog.isWarnEnabled()) {
                asyncLog.warn("Exception during ping", ex);
            }
            pinger.terminate();

        } catch (AdapterTerminatedException ex) {
            if (asyncLog.isWarnEnabled()) {
                asyncLog.warn("Adapter has been terminated", ex);
            }
            pinger.terminate();
        }
    }

    private boolean registered() {
        synchronized (adapterMonitor) {
            if (!isDeployed) {
                // this case implies an attempt to register before the component
                // has been fully deployed.
                syncLog.error("Attempt to register with adapter before depoyed");
                terminate();
                return false;
            }

            if (terminated) {
                // this case implies the system is in the process of terminating
                // so just return indicating no registration
                return false;
            }

            if (isRegistered) {
                // this case implies we've already registered so
                // return inidicating success
                return true;
            }

            try {
                adapter.registerSPLocator(this);
                isRegistered = true;
                liveness.ping();
                pinger.start();
                livenessChecker.start();
                return true;
            } catch (DuplicateSPLocatorException ex) {
                syncLog.error("Already registered when trying to register with SPLocatorAdapter",
                              ex);
                terminate();
                return false;

            } catch (RemoteException ex) {
                syncLog.error("Remote exception trying to register with SPLocatorAdapter",
                              ex);
                terminate();
                return false;

            }
        }
    }
}