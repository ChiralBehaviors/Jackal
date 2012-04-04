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

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.ValueData;
import org.smartfrog.services.anubis.partition.util.Identity;

public class SPLocatorAdapterImpl implements SPLocatorAdapter {

    class LivenessChecker extends PeriodicTimer {
        LivenessChecker(long period) {
            super("Adapter liveness checker", period);
        }

        @Override
        protected void act(long now) {
            checkLiveness(now);
        }
    }

    static class Provider extends AnubisProvider {
        public Provider(String name) {
            super(name);
        }

        public synchronized void setValueData(ValueData value) {
            setValueData(value, System.currentTimeMillis());
        }

        public synchronized void setValueData(ValueData value, long time) {
            setValueObj(value);
            setTime(time);
            update();
        }
    }

    private final AnubisLocator                 locator;
    private final Map<SPLocator, SPLocatorData> subProcessLocators = new HashMap<SPLocator, SPLocatorData>();
    private final long                          timeout;
    private final LivenessChecker               livenessChecker;
    private volatile boolean                    terminated;

    public SPLocatorAdapterImpl(final long heartbeatTimeout,
                                AnubisLocator locator, final long period) {
        timeout = heartbeatTimeout * period;
        livenessChecker = new LivenessChecker(period);
        this.locator = locator;
    }

    @PostConstruct
    public synchronized void deploy() {
        livenessChecker.start();
    }

    @Override
    public synchronized void deregisterListener(SPLocator spLocator,
                                                SPListener spListener)
                                                                      throws RemoteException,
                                                                      UnknownSPLocatorException {

        if (terminated) {
            throw new RemoteException();
        }

        Map<SPListener, SPListenerAdapterImpl> listeners = getSPLocatorData(
                                                                            spLocator).getListeners();
        if (listeners == null) {
            return;
        }

        SPListenerAdapterImpl listener = listeners.remove(spListener);
        if (listener != null) {
            locator.deregisterListener(listener);
        }
    }

    @Override
    public synchronized void deregisterProvider(SPLocator spLocator,
                                                String instance)
                                                                throws RemoteException,
                                                                UnknownSPLocatorException {

        if (terminated) {
            throw new RemoteException("already terminated");
        }

        Map<String, Provider> providers = getSPLocatorData(spLocator).getProviders();
        if (providers == null) {
            return;
        }

        Provider provider = providers.remove(instance);
        if (provider != null) {
            locator.deregisterProvider(provider);
        }
    }

    @Override
    public synchronized void deregisterSPLocator(SPLocator spLocator)
                                                                     throws RemoteException,
                                                                     UnknownSPLocatorException {

        if (terminated) {
            throw new RemoteException("already terminated");
        }

        SPLocatorData spLocatorData = removeSPLocatorData(spLocator);
        clearRegistrations(spLocatorData);
    }

    @Override
    public void deregisterStability(SPLocator spLocator, SPStability spStability)
                                                                                 throws UnknownSPLocatorException,
                                                                                 RemoteException {

        if (terminated) {
            throw new RemoteException("already terminated");
        }

        Map<SPStability, SPStabilityAdapterImpl> stabilities = getSPLocatorData(
                                                                                spLocator).getStabilities();
        if (stabilities == null) {
            return;
        }

        SPStabilityAdapterImpl stability = stabilities.remove(spStability);
        if (stability != null) {
            locator.deregisterStability(stability);
        }
    }

    @Override
    public Identity getIdentity() throws RemoteException {
        return locator.getIdentity();
    }

    @Override
    public synchronized void livenessPing(SPLocator spLocator)
                                                              throws RemoteException,
                                                              UnknownSPLocatorException,
                                                              AdapterTerminatedException {

        if (terminated) {
            throw new AdapterTerminatedException();
        }

        getSPLocatorData(spLocator).getLiveness().ping();
    }

    @Override
    public synchronized void newProviderValue(SPLocator spLocator,
                                              String instance, ValueData value,
                                              long time)
                                                        throws RemoteException,
                                                        UnknownSPLocatorException {

        if (terminated) {
            throw new RemoteException("already terminated");
        }

        Map<String, Provider> providers = getSPLocatorData(spLocator).getProviders();
        Provider provider = providers.get(instance);
        if (provider != null) {
            provider.setValueData(value, time);
        }
    }

    @Override
    public synchronized void registerListener(SPLocator spLocator, String name,
                                              SPListener spListener)
                                                                    throws RemoteException,
                                                                    UnknownSPLocatorException {

        if (terminated) {
            throw new RemoteException("already terminated");
        }

        Map<SPListener, SPListenerAdapterImpl> listeners = getSPLocatorData(
                                                                            spLocator).getListeners();
        SPListenerAdapterImpl listener = new SPListenerAdapterImpl(name,
                                                                   spListener);
        locator.registerListener(listener);
        listeners.put(spListener, listener);
    }

    @Override
    public synchronized SPProviderRegRet registerProvider(SPLocator spLocator,
                                                          String name,
                                                          ValueData value)
                                                                          throws RemoteException,
                                                                          UnknownSPLocatorException {

        if (terminated) {
            throw new RemoteException();
        }

        Map<String, Provider> providers = getSPLocatorData(spLocator).getProviders();
        Provider provider = new Provider(name);
        provider.setValueData(value);
        locator.registerProvider(provider);
        providers.put(provider.getInstance(), provider);
        return new SPProviderRegRet(provider.getInstance(), provider.getTime());
    }

    @Override
    public synchronized void registerSPLocator(SPLocator spLocator)
                                                                   throws RemoteException,
                                                                   DuplicateSPLocatorException {
        if (terminated) {
            throw new RemoteException("already terminated");
        }

        if (subProcessLocators.containsKey(spLocator)) {
            throw new DuplicateSPLocatorException();
        }

        subProcessLocators.put(spLocator, new SPLocatorData(timeout));
    }

    @Override
    public void registerStability(SPLocator spLocator, SPStability spStability)
                                                                               throws UnknownSPLocatorException,
                                                                               RemoteException {

        if (terminated) {
            throw new RemoteException("already terminated");
        }

        Map<SPStability, SPStabilityAdapterImpl> stabilities = getSPLocatorData(
                                                                                spLocator).getStabilities();
        SPStabilityAdapterImpl stability = new SPStabilityAdapterImpl(
                                                                      spStability);
        locator.registerStability(stability);
        stabilities.put(spStability, stability);
    }

    @PreDestroy
    public void terminate() {
        livenessChecker.terminate();
    }

    private void checkLiveness(long now) {
        Map.Entry<SPLocator, SPLocatorData> entry;
        SPLocatorData spLocatorData;
        Iterator<Map.Entry<SPLocator, SPLocatorData>> iter = subProcessLocators.entrySet().iterator();
        while (iter.hasNext()) {
            entry = iter.next();
            spLocatorData = entry.getValue();
            if (spLocatorData == null || spLocatorData.getLiveness() == null) {
                // already dead, jim
                iter.remove();
            } else {
                if (spLocatorData.getLiveness().isNotTimely(now)) {
                    clearRegistrations(spLocatorData);
                    iter.remove();
                }
            }
        }
    }

    private void clearRegistrations(SPLocatorData spLocatorData) {
        Iterator<Provider> iter = spLocatorData.getProviders().values().iterator();
        while (iter.hasNext()) {
            locator.deregisterProvider(iter.next());
        }
        Iterator<SPListenerAdapterImpl> iter2 = spLocatorData.getListeners().values().iterator();
        while (iter2.hasNext()) {
            locator.deregisterListener(iter2.next());
        }
        spLocatorData.clear();
    }

    private SPLocatorData getSPLocatorData(SPLocator spLocator)
                                                               throws UnknownSPLocatorException {
        SPLocatorData spLocatorData = subProcessLocators.get(spLocator);
        if (spLocatorData == null || spLocatorData.getLiveness() == null) {
            throw new UnknownSPLocatorException();
        }
        return spLocatorData;
    }

    private SPLocatorData removeSPLocatorData(SPLocator spLocator)
                                                                  throws UnknownSPLocatorException {
        SPLocatorData spLocatorData = subProcessLocators.remove(spLocator);
        if (spLocatorData == null || spLocatorData.getLiveness() == null) {
            throw new UnknownSPLocatorException();
        }
        return spLocatorData;
    }

}