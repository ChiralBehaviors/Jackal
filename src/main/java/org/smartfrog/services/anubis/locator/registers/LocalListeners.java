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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.locator.msg.RegisterMsg;
import org.smartfrog.services.anubis.locator.names.ListenerProxy;
import org.smartfrog.services.anubis.locator.names.ProviderInstance;
import org.smartfrog.services.anubis.locator.names.ProviderProxy;
import org.smartfrog.services.anubis.locator.util.SetMap;
import org.smartfrog.services.anubis.partition.views.View;

public class LocalListeners {

    /**
     * The listener info class is a data structure that records the information
     * relating to a single name for which this node holds listeners. This
     * includes: a listener proxy record; the set of listeners at this node that
     * are listening to this name; the set of provider instances at any node
     * that are providing this name.
     */
    private class ListenerInfo {

        public Set<AnubisListener> listeners = new HashSet<AnubisListener>();
        public Map<ProviderProxy, ProviderInstance> providers = new HashMap<ProviderProxy, ProviderInstance>(); // instance
                                                                                                                // -->
                                                                                                                // name
        public ListenerProxy proxy = null;

        public ListenerInfo(String name, AnubisListener l) {
            proxy = new ListenerProxy(name, me, uniqueRegId++);
            listeners.add(l);
        }

        @Override
        public String toString() {
            String str = proxy.toString() + " has " + listeners.size()
                         + " listeners and " + providers.size()
                         + " providers:\n";
            for (ProviderInstance provider : providers.values()) {
                str += "        " + provider.toString() + "\n";
            }
            return str;
        }
    }

    /**
     * listeners maps names-->ListenerInfo records.
     */
    private Map<String, ListenerInfo> listeners = new HashMap<String, ListenerInfo>();
    private Locator locator = null;

    private static Logger log = Logger.getLogger(LocalListeners.class.getCanonicalName());

    private Integer me = null;
    /**
     * providersByNode maps nodes-->Set of names. This data structure keeps a
     * record of the names being provided at each node. When a partition occurs
     * we pick out absent nodes and use the names to find providers that should
     * be removed from the listenerInfo records.
     */
    private SetMap<Integer, ProviderProxy> providersByNode = new SetMap<Integer, ProviderProxy>();
    private long uniqueRegId = 0;;

    public LocalListeners(Locator l, Integer id) {
        locator = l;
        me = id;
    }

    /**
     * This method will search the listeners for bindings to providers that are
     * on absent nodes.
     * 
     * @param view
     */
    public synchronized void checkNodes(View view) {

        /**
         * time of partitioning
         */
        long timeNow = System.currentTimeMillis();

        /**
         * Iterate through the providersByNode mapSet looking for nodes that are
         * not in the view.
         */
        Iterator<Integer> nodeIter = providersByNode.keySet().iterator();
        while (nodeIter.hasNext()) {

            /**
             * Get the next node - step past it if it is still in the view.
             */
            Integer node = nodeIter.next();
            if (view.contains(node.intValue())) {
                continue;
            }

            /**
             * Iterate through the provider names associated with the current
             * node, get the info for each name and remove provider instances
             * that are from that node. Then remove the entire record for that
             * node.
             */

            for (ProviderProxy proxy : providersByNode.getSet(node)) {
                ListenerInfo info = listeners.get(proxy.name);

                for (ProviderInstance instance = info.providers.remove(proxy); instance != null; instance = info.providers.remove(proxy)) {
                    for (AnubisListener listener : info.listeners) {
                        listener.removeValue(instance, timeNow);
                    }
                }
            }

            /**
             * Remove the node from the providersByNode data
             */
            nodeIter.remove();
        }
    }

    /**
     * deregister a listener.
     * 
     * @param listener
     */
    public synchronized void deregister(AnubisListener listener) {

        /**
         * if there is no corresponding listener info then just drop
         */
        if (!listeners.containsKey(listener.getName())) {
            return;
        }

        /**
         * Remove this listener from the set of listeners
         */
        ListenerInfo info = listeners.get(listener.getName());
        info.listeners.remove(listener);

        /**
         * If there are no other listeners interested in the same provider then
         * deregister from the global, deregister from each node that contains a
         * provider, and clean up all information associated with the listener.
         */
        if (info.listeners.isEmpty()) {

            /**
             * Inform the global register
             */
            RegisterMsg deregMsg = RegisterMsg.deregisterListener(info.proxy);
            locator.sendToGlobal(deregMsg);

            /**
             * Inform all nodes from which we have received values
             */
            Set<Integer> informedNodes = new HashSet<Integer>();
            Iterator<ProviderInstance> providerIter = info.providers.values().iterator();

            while (providerIter.hasNext()) {

                /**
                 * get the next known provider and remove it from the nodes
                 * information.
                 */
                ProviderProxy providerProxy = providerIter.next().proxy();
                if (providersByNode.remove(providerProxy.node, providerProxy)) {

                    /**
                     * If the node that just had a provider removed has not been
                     * informed of the deregistration then inform it now.
                     */
                    if (!informedNodes.contains(providerProxy.node)) {
                        RegisterMsg removeMsg = RegisterMsg.removeListener(info.proxy);
                        locator.sendToLocal(removeMsg, providerProxy.node);
                        informedNodes.add(providerProxy.node);
                    }
                }
            }

            listeners.remove(listener.getName());
        }
    }

    public synchronized void providerNotPresent(ProviderInstance provider) {

        /**
         * if there is no corresponding listener info then just drop
         */
        if (!listeners.containsKey(provider.name)) {
            if (log.isLoggable(Level.FINER)) {
                log.finer(me + ": there are no listeners for removed "
                          + provider);
            }
            return;
        }

        /**
         * Get the info, if it does not contain this instance just return now.
         */
        ListenerInfo info = listeners.get(provider.name);
        if (info.providers.remove(provider) == null) {
            if (log.isLoggable(Level.FINER)) {
                log.finer(me + ": listeners did not know removed " + provider);
            }
            return;
        }

        /**
         * If the info contains no provider instances for with the given name
         * and node then remove the mapping from the providers by node map.
         * NOTE: NameData matches ProviderInstance on name and node only, so if
         * the set contains ANY instance with the given name and node contains()
         * will return true.
         */
        if (!info.providers.containsKey(provider.proxy())) {

            providersByNode.remove(provider.node, provider.proxy());

        }

        /**
         * Inform all the listeners that this value has now gone
         */
        for (AnubisListener listener : info.listeners) {
            listener.removeValue(provider);
        }
    }

    public synchronized void providerValue(ProviderInstance provider) {

        /**
         * if there is no corresponding listener info then ignore. Implies that
         * the deregister crossed with this message during transmission. The
         * deregister will happen.
         */
        if (!listeners.containsKey(provider.name)) {
            if (log.isLoggable(Level.FINER)) {
                log.finer(me + ": no listener info matching reported "
                          + provider);
            }
            return;
        }

        /**
         * get the listener info for this provider name
         */
        ListenerInfo info = listeners.get(provider.name);

        /**
         * put the provider instance in the set of providers. If the provider
         * instance was not already known then make sure the providersByNode map
         * contains a record for the source node.
         */
        if (info.providers.put(provider.proxy(), provider) == null) {
            if (log.isLoggable(Level.FINER)) {
                log.finer(me
                          + ": listeners received state of previously unknown "
                          + provider);
            }
            providersByNode.put(provider.node, provider.proxy());
        }

        /**
         * Inform all the listeners of this new value
         */
        for (AnubisListener listener : info.listeners) {
            listener.newValue(provider);
        }
    }

    /**
     * comments please
     * 
     * @param listener
     */
    public synchronized void register(AnubisListener listener) {

        /**
         * if there are any provider instances registered then notify the
         * listener.
         */
        ListenerInfo info;

        /**
         * If this is not the first listener for this name then get then get the
         * listener info and add the new listener.
         */
        if (listeners.containsKey(listener.getName())) {
            info = listeners.get(listener.getName());
            info.listeners.add(listener);
        }

        /**
         * If this is the first listener then create the listener info and add
         * the new listener, then register with the global registry
         */
        else {
            info = new ListenerInfo(listener.getName(), listener);
            listeners.put(listener.getName(), info);
            locator.sendToGlobal(RegisterMsg.registerListener(info.proxy));
        }

        /**
         * if there are any provider instances then notify the listener. (if
         * there are none then the listener will stabalise in its initial state
         * - i.e. with none).
         */
        for (ProviderInstance provider : info.providers.values()) {
            listener.newValue(provider);
        }
    }

    /**
     * Register all the listeners known to the local register that are not
     * bound.
     */
    public synchronized void reRegisterAll() {
        for (ListenerInfo info : listeners.values()) {
            locator.sendToGlobal(RegisterMsg.registerListener(info.proxy));
        }
    }

    @Override
    public synchronized String toString() {
        String str = "Remote Providers by node:\n";
        for (Entry<Integer, Set<ProviderProxy>> entry : providersByNode.entrySet()) {
            str += "    " + entry.getKey();
            for (ProviderProxy provider : entry.getValue()) {
                str += " " + provider.name;
            }
            str += "\n";
        }

        str += "\nListeners:\n";
        for (ListenerInfo listener : listeners.values()) {
            str += "    " + listener + "\n";
        }
        str += "\n";
        return str;
    }
}
