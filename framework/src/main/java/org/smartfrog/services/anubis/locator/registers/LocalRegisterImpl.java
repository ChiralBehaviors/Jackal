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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.AnubisStability;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.locator.ValueData;
import org.smartfrog.services.anubis.locator.msg.RegisterMsg;
import org.smartfrog.services.anubis.locator.names.ListenerProxy;
import org.smartfrog.services.anubis.locator.names.ProviderInstance;
import org.smartfrog.services.anubis.locator.util.DebugFrame;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;

public class LocalRegisterImpl {

    private class UserListenerRequest {
        public final static int Deregister = 2;
        public final static int Register   = 1;
        @SuppressWarnings("unused")
        public final static int Unknown    = 0;
        public AnubisListener   listener;
        public int              type;

        public UserListenerRequest(int t, AnubisListener l) {
            type = t;
            listener = l;
        }
    }

    private class UserProviderRequest {
        public final static int Deregister = 2;
        public final static int NewValue   = 3;
        public final static int Register   = 1;
        @SuppressWarnings("unused")
        public final static int Unknown    = 0;
        public AnubisProvider   provider;
        public long             time;
        public int              type;
        public ValueData        value;

        public UserProviderRequest(int t, AnubisProvider p, ValueData value,
                                   long time) {
            type = t;
            provider = p;
            this.value = value;
            this.time = time;
        }
    }

    private class UserStabilityRequest {
        public final static int Deregister = 2;
        public final static int Register   = 1;
        @SuppressWarnings("unused")
        public final static int Unknown    = 0;
        public AnubisStability  stability;
        public int              type;

        public UserStabilityRequest(int t, AnubisStability s) {
            type = t;
            stability = s;
        }
    }

    public Set<AnubisStability> stabilityNotifications = new HashSet<AnubisStability>();
    public boolean              stable                 = true;
    public long                 timeRef                = -1;
    private DebugFrame          debug                  = null;
    private LocalListeners      listeners              = null;
    private static final Logger log                    = Logger.getLogger(LocalRegisterImpl.class.getCanonicalName());
    private Identity            me                     = null;
    private Integer             node                   = null;
    private LocalProviders      providers              = null;

    /**
     * RequestServer is required to avoid a potential deadlock between the local
     * and global if they send messages to each other on the local node. Sending
     * a message to the local node results in direct delivery by method call in
     * the same thread. It is possible for a thread that holds the
     * GlobalRegisterImpl monitor to make a call to the LocalRegisterImpl, and
     * vice versa at the same time. So, instead of blocking on a monitor we
     * create a queue of requests for the global and service the queue with a
     * single thread.
     */
    private ExecutorService     requestServer;

    public LocalRegisterImpl(Identity id, Locator locator) {
        me = id;
        node = new Integer(me.id);
        providers = new LocalProviders(locator, node);
        listeners = new LocalListeners(locator, node);
        timeRef = me.epoch;
        requestServer = Executors.newSingleThreadExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "Anubis: Local Register Request Server (node "
                                                   + me.id + ")");
                daemon.setDaemon(true);
                return daemon;
            }
        });
    }

    public void deliverRequest(final RegisterMsg request) {
        requestServer.execute(new Runnable() {
            @Override
            public void run() {
                deliver(request);
            }
        });
    }

    /**
     * deregisterListener: deregister locally. deregister with providers local
     * if appropriate. deregister globally if the listener was pending and there
     * are no more. The global only has pending listeners registered.
     * 
     * @param listener
     */
    public void deregisterListener(final AnubisListener listener) {
        requestServer.execute(new Runnable() {
            @Override
            public void run() {
                deliver(new UserListenerRequest(UserListenerRequest.Deregister,
                                                listener));
            }
        });
    }

    /**
     * deregisterProvider: deregister with global register. local register is
     * responsible for informing listeners that have already contacted this
     * register. deregister with local register.
     * 
     * @param provider
     */
    public void deregisterProvider(final AnubisProvider provider) {
        requestServer.execute(new Runnable() {
            @Override
            public void run() {
                deliver(new UserProviderRequest(UserProviderRequest.Deregister,
                                                provider, null, 0));
            }
        });
    }

    /**
     * deregisterStability: deregister a stability notification object.
     * 
     * @param stability
     *            AnubisStability
     */
    public void deregisterStability(final AnubisStability stability) {
        requestServer.execute(new Runnable() {
            @Override
            public void run() {
                deliver(new UserStabilityRequest(
                                                 UserStabilityRequest.Deregister,
                                                 stability));
            }
        });
    }

    /**
     * indicates that a provider has been assigned a new value.
     * 
     * @param provider
     */
    public void newProviderValue(final AnubisProvider provider) {
        requestServer.execute(new Runnable() {
            @Override
            public void run() {
                deliver(new UserProviderRequest(UserProviderRequest.NewValue,
                                                provider,
                                                provider.getValueData(),
                                                provider.getTime()));
            }
        });
    }

    /**
     * @param listener
     */
    public void registerListener(final AnubisListener listener) {
        requestServer.execute(new Runnable() {
            @Override
            public void run() {
                deliver(new UserListenerRequest(UserListenerRequest.Register,
                                                listener));
            }
        });
    }

    /**
     * registerProvider: add provider to global registry and locally. the global
     * is responsible for telling listeners where the provider is. the listeners
     * are responsible for contacting this local registry to get provider info.
     * 
     * @param provider
     */
    public void registerProvider(final AnubisProvider provider) {
        requestServer.execute(new Runnable() {
            @Override
            public void run() {
                deliver(new UserProviderRequest(UserProviderRequest.Register,
                                                provider,
                                                provider.getValueData(),
                                                provider.getTime()));
            }
        });
    }

    /**
     * registerStability: register a stability notification object.
     * 
     * @param stability
     *            AnubisStability
     */
    public void registerStability(final AnubisStability stability) {
        requestServer.execute(new Runnable() {
            @Override
            public void run() {
                deliver(new UserStabilityRequest(UserStabilityRequest.Register,
                                                 stability));
            }
        });
    }

    public synchronized void removeDebugFrame() {
        if (debug != null) {
            debug.remove();
            debug = null;
        }
    }

    public synchronized void showDebugFrame() {
        if (debug == null) {
            debug = new DebugFrame("Node " + me + " Local Register Contents:");
        }
        debug.makeVisible(this);
    }

    /**
     * When stable: 1) find the global 2) start accessing it 3) register all
     * providers and listeners again Note: we are being dumb and pessimistic
     * here - always recover all registrations - could be more clever.
     * 
     * @param leader
     * @param timeStamp
     */
    public synchronized void stable(int leader, long timeStamp) {
        stable = true;
        timeRef = timeStamp;
        providers.reRegisterAll();
        listeners.reRegisterAll();
        notifyStability();
        updateDebugFrame();
    }

    /**
     * Starts the local register server
     */
    public void start() {
        updateDebugFrame();
    }

    /**
     * Stop the threads associated with the local register. Also clears the
     * queue.
     */
    public void terminate() {
        requestServer.shutdownNow();
    }

    @Override
    public synchronized String toString() {
        return providers.toString() + listeners.toString();
    }

    /**
     * When unstable: 1) stop accessing the global - it needs time to clear up
     * 2) check provider and listener dependencies - I could have lost some
     * 
     * @param view
     */
    public synchronized void unstable(View view) {
        stable = false;
        timeRef = -1;
        providers.checkNodes(view);
        listeners.checkNodes(view);
        notifyStability();
        updateDebugFrame();
    }

    private void deliver(RegisterMsg msg) {

        switch (msg.type) {

            case RegisterMsg.ProviderValue:

                listeners.providerValue((ProviderInstance) msg.data);
                updateDebugFrame();
                break;

            case RegisterMsg.ProviderNotPresent:

                listeners.providerNotPresent((ProviderInstance) msg.data);
                updateDebugFrame();
                break;

            case RegisterMsg.AddListener:

                providers.addListener((ListenerProxy) msg.data);
                updateDebugFrame();
                break;

            case RegisterMsg.RemoveListener:

                providers.removeListener((ListenerProxy) msg.data);
                updateDebugFrame();
                break;

            default:
                log.severe(me + " *** Local received unexpected message " + msg);
        }
    }

    private void deliver(UserListenerRequest request) {

        switch (request.type) {

            case UserListenerRequest.Register:
                listeners.register(request.listener);
                updateDebugFrame();
                break;

            case UserListenerRequest.Deregister:
                listeners.deregister(request.listener);
                updateDebugFrame();
                break;

            default:
                log.severe(me
                           + " *** Local register encountered unknown user stability request type: "
                           + request.type);
        }
    }

    private void deliver(UserProviderRequest request) {

        switch (request.type) {

            case UserProviderRequest.Register:
                providers.register(request.provider, request.value,
                                   request.time);
                updateDebugFrame();
                break;

            case UserProviderRequest.Deregister:

                providers.deregister(request.provider);
                updateDebugFrame();
                break;

            case UserProviderRequest.NewValue:

                providers.newValue(request.provider, request.value,
                                   request.time);
                updateDebugFrame();
                break;

            default:
                log.severe(me
                           + " *** Local register encountered unknown user provider request type: "
                           + request.type);
        }
    }

    private void deliver(UserStabilityRequest request) {

        switch (request.type) {

            case UserStabilityRequest.Register:
                stabilityNotifications.add(request.stability);
                request.stability.notifyStability(stable, timeRef);
                break;

            case UserStabilityRequest.Deregister:
                stabilityNotifications.remove(request.stability);
                break;

            default:
                log.severe(me
                           + " *** Local register encountered unknown user stability request type: "
                           + request.type);
        }
    }

    /**
     * notify all stability notification objects
     */
    private void notifyStability() {
        Iterator<AnubisStability> iter = stabilityNotifications.iterator();
        while (iter.hasNext()) {
            iter.next().notifyStability(stable, timeRef);
        }
    }

    private void updateDebugFrame() {
        if (debug != null) {
            debug.update();
        }
    }
}
