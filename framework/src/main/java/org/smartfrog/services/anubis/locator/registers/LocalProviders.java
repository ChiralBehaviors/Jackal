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
package org.smartfrog.services.anubis.locator.registers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.locator.ValueData;
import org.smartfrog.services.anubis.locator.msg.RegisterMsg;
import org.smartfrog.services.anubis.locator.names.ListenerProxy;
import org.smartfrog.services.anubis.locator.names.ProviderInstance;
import org.smartfrog.services.anubis.locator.names.ProviderProxy;
import org.smartfrog.services.anubis.locator.util.SetMap;
import org.smartfrog.services.anubis.partition.views.View;

public class LocalProviders {
    private class ProviderInfo {
        public Map<String, ProviderInstance>     instances = new HashMap<String, ProviderInstance>();
        public Map<ListenerProxy, ListenerProxy> listeners = new HashMap<ListenerProxy, ListenerProxy>();
        public Map<String, AnubisProvider>       providers = new HashMap<String, AnubisProvider>();
        public ProviderProxy                     proxy;

        public ProviderInfo(AnubisProvider provider) {
            proxy = new ProviderProxy(provider.getName(), me);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("    " + proxy + " has " + instances.size()
                           + " instances " + providers.size() + " providers "
                           + listeners.size() + " listeners \n");
            builder.append("        at nodes: ");

            Iterator<ListenerProxy> iter = listeners.values().iterator();
            while (iter.hasNext()) {
                builder.append(iter.next().node).append(" ");
            }

            builder.append("\n");
            return builder.toString();
        }
    }

    private SetMap<Integer, ListenerProxy> listenersByNode = new SetMap<Integer, ListenerProxy>();                            // node-->Set of listeners

    private Locator                        locator         = null;
    private static final Logger            log             = LoggerFactory.getLogger(LocalProviders.class.getCanonicalName());

    private Integer                        me              = null;
    private Map<String, ProviderInfo>      providers       = new HashMap<String, ProviderInfo>();                             // name-->record

    /**
     * Constructor
     * 
     * @param l
     * @param id
     */
    public LocalProviders(Locator l, Integer id) {
        locator = l;
        me = id;
    }

    public synchronized void addListener(ListenerProxy listener) {

        /**
         * If there is no info associated with the listener's name then just
         * return now - the global must have notified us of the listener after
         * we have deregistered, but before it knew about that.
         */
        if (!providers.containsKey(listener.name)) {
            if (log.isTraceEnabled()) {
                log.trace(me + ": no provider info matching reported "
                          + listener);
            }
            return;
        }

        /**
         * Get the info associated with this provider's name and the matching
         * existing registration (if it exists).
         */
        ProviderInfo info = providers.get(listener.name);
        ListenerProxy existingReg = info.listeners.get(listener);

        /**
         * If there is no existing registration for this listener then add it.
         */
        if (existingReg == null) {
            if (log.isTraceEnabled()) {
                log.trace(me + ": new registration " + listener);
            }

            info.listeners.put(listener, listener);
            listenersByNode.put(listener.node, listener);
        }

        /**
         * If there is an existing registration but the new one superceeds it
         * then remove the old registration and add the new one. This can happen
         * if a new registration via the global register overtakes the
         * deregistration that comes directly from the listener node.
         */
        else if (existingReg.uridPreceeds(listener)) {
            if (log.isTraceEnabled()) {
                log.trace(me + ": new reg superceeds existing registration "
                          + listener);
            }

            info.listeners.remove(existingReg);
            listenersByNode.remove(existingReg.node, existingReg);

            info.listeners.put(listener, listener);
            listenersByNode.put(listener.node, listener);
        }

        /**
         * If there is an existing registration that superceeds the new one then
         * ignore the new one and return. This suggests that the global register
         * has re-issued the registration after recovering from a
         * re-partitioning event. In this case we do not re-send values.
         */
        else if (existingReg.uridEquals(listener)) {
            if (log.isTraceEnabled()) {
                log.trace(me
                          + ": new reg does not superceed existing registration "
                          + listener);
            }
            return;
        }

        /**
         * If we get to this point then we have a new listener registration so
         * inform the listener of all the provider instances.
         */
        Iterator<ProviderInstance> iter = info.instances.values().iterator();
        if (log.isTraceEnabled()) {
            log.trace(me + ": sending states of "
                      + info.instances.values().size()
                      + " registered providers to added " + listener);
        }
        while (iter.hasNext()) {

            RegisterMsg msg = RegisterMsg.providerValue(iter.next());
            if (log.isTraceEnabled()) {
                log.trace(me + ": sending " + msg + " to node " + listener.node);
            }
            locator.sendToLocal(msg, listener.node);
        }
    }

    /**
     * For each node that is not in the view remove all the bindings to
     * listeners on that node.
     * 
     * @param view
     */
    public synchronized void checkNodes(View view) {

        /**
         * Iterate over all the nodes
         */
        Iterator<Integer> nodeIter = listenersByNode.keySet().iterator();
        while (nodeIter.hasNext()) {

            /**
             * If the node is in the view skip over it
             */
            Integer node = nodeIter.next();
            if (view.contains(node.intValue())) {
                continue;
            }

            /**
             * Iterate over the listeners records associated with the node and
             * remove the listeners from the provider info
             */
            Iterator<ListenerProxy> listenerIter = listenersByNode.getSet(node).iterator();
            while (listenerIter.hasNext()) {
                ListenerProxy listener = listenerIter.next();
                ProviderInfo info = providers.get(listener.name);
                // the following line is unchanged with maps
                info.listeners.remove(listener);
            }

            /**
             * Remove the node from the listenersByNode structure
             */
            nodeIter.remove();
        }
    }

    public synchronized void deregister(AnubisProvider provider) {

        /**
         * If there is no info associated with the provider's name then just
         * return now
         */
        if (!providers.containsKey(provider.getName())) {
            return;
        }

        /**
         * Get the info associated with this provider's name. Remove the records
         * for the instance data and the provider from the info.
         */
        ProviderInfo info = providers.get(provider.getName());
        ProviderInstance instance = info.instances.remove(provider.getInstance());
        /**
         * If there was no instance record (and hance no provider record) then
         * return now - it means the provider was not know
         */
        if (instance == null) {
            return;
        }

        info.providers.remove(instance.instance);

        /**
         * Set the instance time to the time now and inorm any listners that the
         * provider instance has gone away. Setting the time tells them when it
         * went away.
         */
        instance.time = System.currentTimeMillis();

        Iterator<ListenerProxy> iter = info.listeners.values().iterator();
        while (iter.hasNext()) {
            locator.sendToLocal(RegisterMsg.providerNotPresent(instance),
                                iter.next().node);
        }

        /**
         * If there are no more providers in this info then there are no more
         * providers on this node. Drop the info record.
         */
        if (info.instances.isEmpty()) {
            providers.remove(provider.getName());
            locator.sendToGlobal(RegisterMsg.deregisterProvider(info.proxy));

            Iterator<ListenerProxy> listenerIter = info.listeners.values().iterator();
            while (listenerIter.hasNext()) {
                ListenerProxy proxy = listenerIter.next();
                listenersByNode.remove(proxy.node, proxy);
            }
        }
    }

    public synchronized void newValue(AnubisProvider provider, ValueData value,
                                      long time) {
        /**
         * If there is no info associated with the provider's name then just
         * return now
         */
        if (!providers.containsKey(provider.getName())) {
            return;
        }

        /**
         * Get the info associated with this provider's name. If there is no
         * instance then return now.
         */
        ProviderInfo info = providers.get(provider.getName());
        ProviderInstance instance = info.instances.get(provider.getInstance());
        if (instance == null) {
            return;
        }

        /**
         * modify the value
         */
        instance.value = value;
        instance.time = time;

        /**
         * Inform any listners that there is a new value
         */
        for (ListenerProxy listener : info.listeners.values()) {
            locator.sendToLocal(RegisterMsg.providerValue(instance),
                                listener.node);
        }
    }

    /**
     * register a provider. This updates the local register with info about the
     * provider and issues a registration with the global register.
     * 
     * FIX ME: does not check for uniqueness in the partition.
     * 
     * @param provider
     *            - the provider interface
     * @param value
     * @param time
     */
    public synchronized void register(AnubisProvider provider, ValueData value,
                                      long time) {

        /**
         * Setting the locator in the AnubisProvider indicates that it is
         * registered, now all value updates will be indicated to the locator.
         * This also sets the provider's initial time (registered == exists).
         */
        ProviderInstance instance = new ProviderInstance(
                                                         provider.getName(),
                                                         provider.getInstance(),
                                                         me, time, value);

        /**
         * If there is already provider info for the providers name, then add
         * the new instance to the info and send the instance info to all the
         * registered listeners (if any).
         */
        if (providers.containsKey(provider.getName())) {

            ProviderInfo info = providers.get(provider.getName());
            info.providers.put(instance.instance, provider);
            info.instances.put(instance.instance, instance);

            for (ListenerProxy listener : info.listeners.values()) {
                locator.sendToLocal(RegisterMsg.providerValue(instance),
                                    listener.node);
            }
        }

        /**
         * If there is no provider info for this name there will be no
         * registered listeners yet either. Create the new info record, add this
         * instance and register with the global proxy.
         */
        else {
            ProviderInfo info = new ProviderInfo(provider);
            info.providers.put(instance.instance, provider);
            info.instances.put(instance.instance, instance);
            providers.put(provider.getName(), info);
            locator.sendToGlobal(RegisterMsg.registerProvider(info.proxy));
        }
    }

    public synchronized void removeListener(ListenerProxy listener) {

        /**
         * If there is no info associated with the listener's name then just
         * return now
         */
        if (!providers.containsKey(listener.name)) {
            if (log.isTraceEnabled()) {
                log.trace(me + ": no provider info matching removed "
                          + listener);
            }
            return;
        }

        /**
         * Get the info associated with this provider's name and the existing
         * registration.
         */
        ProviderInfo info = providers.get(listener.name);
        ListenerProxy existingReg = info.listeners.get(listener);

        /**
         * If the existing registration superceeds the deregistration then
         * ignore it and return now. This implies that the listener's node has
         * re-registered and the registration via the global has overtaken this
         * direct deregister.
         */
        if (listener.uridPreceeds(existingReg)) {
            if (log.isTraceEnabled()) {
                log.trace(me
                          + " existing registration superceeds deregistration "
                          + listener);
            }
            return;
        }

        /**
         * remove the binding of node to listener in the listenersByNode
         * structure
         */
        info.listeners.remove(listener);
        listenersByNode.remove(listener.node, listener);
        if (log.isTraceEnabled()) {
            log.trace(me + ": provider info removed " + listener);
        }
    }

    /**
     * Register all the providers known to the local register.
     */
    public synchronized void reRegisterAll() {
        if (log.isTraceEnabled()) {
            log.trace("Reregistering all providers");
        }
        for (ProviderInfo info : providers.values()) {
            locator.sendToGlobal(RegisterMsg.registerProvider(info.proxy));
        }
    }

    @Override
    public synchronized String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Remote Listeners By Node:\n");
        Iterator<Integer> iter = listenersByNode.keySet().iterator();
        while (iter.hasNext()) {
            Integer node = iter.next();
            builder.append("    " + node);
            for (Iterator<ListenerProxy> iter2 = listenersByNode.getSet(node).iterator(); iter2.hasNext(); builder.append(" ").append(iter2.next().name)) {
                ;
            }
            builder.append("\n");
        }

        builder.append("\nProviders:\n");
        for (ProviderInfo info : providers.values()) {
            builder.append(info);
        }
        builder.append("\n");
        return builder.toString();
    }

}
