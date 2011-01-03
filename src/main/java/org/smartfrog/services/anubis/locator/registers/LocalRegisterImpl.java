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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.AnubisStability;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.locator.ValueData;
import org.smartfrog.services.anubis.locator.msg.RegisterMsg;
import org.smartfrog.services.anubis.locator.names.ListenerProxy;
import org.smartfrog.services.anubis.locator.names.ProviderInstance;
import org.smartfrog.services.anubis.locator.util.BlockingQueue;
import org.smartfrog.services.anubis.locator.util.DebugFrame;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;

public class LocalRegisterImpl {

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
    private class RequestServer extends Thread {
        private boolean running = false;

        public RequestServer() {
            super();
            setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    log.log(Level.WARNING, "Uncaught exception", e);
                }
            });
        }

        @Override
        public void run() {
            /**
             * The thread specific object locator.callingThread is set to this
             * server object for this thread. The locator is then able to check
             * if calls to its interface are made by this thread by checking the
             * value of locator.callingThread and comparing it to this object.
             * 
             * This is the method used to check if the user is making illegal
             * re-entrant calls during an upcall initiated by the local
             * register.
             */
            locator.callingThread.set(this);
            running = true;
            while (running) {
                Object obj = requests.get();
                if (obj instanceof RegisterMsg) {
                    deliver((RegisterMsg) obj);
                } else if (obj instanceof UserProviderRequest) {
                    deliver((UserProviderRequest) obj);
                } else if (obj instanceof UserListenerRequest) {
                    deliver((UserListenerRequest) obj);
                } else if (obj instanceof UserStabilityRequest) {
                    deliver((UserStabilityRequest) obj);
                } else if (obj == null) {
                    continue;
                } else {
                    log.severe(me
                               + " *** Local register encountered unknown request or message type: "
                               + obj);
                }
            }
        }

        public void terminate() {
            running = false;
        }
    }

    private static class UserListenerRequest {
        public final static int Deregister = 2;
        public final static int Register = 1;
        @SuppressWarnings("unused")
        public final static int Unknown = 0;
        public AnubisListener listener;
        public int type;

        public UserListenerRequest(int t, AnubisListener l) {
            type = t;
            listener = l;
        }
    }

    private static class UserProviderRequest {
        public final static int Deregister = 2;
        public final static int NewValue = 3;
        public final static int Register = 1;
        @SuppressWarnings("unused")
        public final static int Unknown = 0;
        public AnubisProvider provider;
        public long time;
        public int type;
        public ValueData value;

        public UserProviderRequest(int t, AnubisProvider p, ValueData value,
                                   long time) {
            type = t;
            provider = p;
            this.value = value;
            this.time = time;
        }
    }

    private static class UserStabilityRequest {
        public final static int Deregister = 2;
        public final static int Register = 1;
        @SuppressWarnings("unused")
        public final static int Unknown = 0;
        public AnubisStability stability;
        public int type;

        public UserStabilityRequest(int t, AnubisStability s) {
            type = t;
            stability = s;
        }
    }

    public RequestServer server = new RequestServer(); // public so Locator can
                                                       // test for re-entry
    public Set<AnubisStability> stabilityNotifications = new HashSet<AnubisStability>();
    public boolean stable = true;
    public long timeRef = -1;
    private DebugFrame debug = null;
    private LocalListeners listeners = null; // name to Listeners + the provider
    private Locator locator = null;
    private static Logger log = Logger.getLogger(LocalRegisterImpl.class.getCanonicalName());
    private Identity me = null;
    private Integer node = null;
    private LocalProviders providers = null; // name to provider + their
                                             // registered listeners
    private BlockingQueue<Object> requests = new BlockingQueue<Object>();

    public LocalRegisterImpl(Identity id, Locator locator) {
        me = id;
        node = Integer.valueOf(me.id);
        providers = new LocalProviders(locator, node);
        listeners = new LocalListeners(locator, node);
        this.locator = locator;
        timeRef = me.epoch;
    }

    public void deliverRequest(RegisterMsg msg) {

        requests.put(msg);
    }

    /**
     * deregisterListener: deregister locally. deregister with providers local
     * if appropriate. deregister globally if the listener was pending and there
     * are no more. The global only has pending listeners registered.
     * 
     * @param listener
     */
    public void deregisterListener(AnubisListener listener) {
        requests.put(new UserListenerRequest(UserListenerRequest.Deregister,
                                             listener));
    }

    /**
     * deregisterProvider: deregister with global register. local register is
     * responsible for informing listeners that have already contacted this
     * register. deregister with local register.
     * 
     * @param provider
     */
    public void deregisterProvider(AnubisProvider provider) {
        requests.put(new UserProviderRequest(UserProviderRequest.Deregister,
                                             provider, null, 0));
    }

    /**
     * deregisterStability: deregister a stability notification object.
     * 
     * @param stability
     *            AnubisStability
     */
    public void deregisterStability(AnubisStability stability) {
        requests.put(new UserStabilityRequest(UserStabilityRequest.Deregister,
                                              stability));
    }

    /**
     * indicates that a provider has been assigned a new value.
     * 
     * @param provider
     */
    public void newProviderValue(AnubisProvider provider) {
        requests.put(new UserProviderRequest(UserProviderRequest.NewValue,
                                             provider, provider.getValueData(),
                                             provider.getTime()));
    }

    /**
     * @param listener
     */
    public void registerListener(AnubisListener listener) {
        requests.put(new UserListenerRequest(UserListenerRequest.Register,
                                             listener));
    }

    /**
     * registerProvider: add provider to global registry and locally. the global
     * is responsible for telling listeners where the provider is. the listeners
     * are responsible for contacting this local registry to get provider info.
     * 
     * @param provider
     */
    public void registerProvider(AnubisProvider provider) {
        requests.put(new UserProviderRequest(UserProviderRequest.Register,
                                             provider, provider.getValueData(),
                                             provider.getTime()));
    }

    /**
     * registerStability: register a stability notification object.
     * 
     * @param stability
     *            AnubisStability
     */
    public void registerStability(AnubisStability stability) {
        requests.put(new UserStabilityRequest(UserStabilityRequest.Register,
                                              stability));
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
        server.setName("Anubis: Local Register Request Server (node " + me.id
                       + ")");
        server.start();
        updateDebugFrame();
    }

    /**
     * Stop the threads associated with the local register. Also clears the
     * queue.
     */
    public void terminate() {
        server.terminate();
        requests.deactivate();
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
        for (AnubisStability notification : stabilityNotifications) {
            notification.notifyStability(stable, timeRef);
        }
    }

    private synchronized void updateDebugFrame() {
        if (debug != null) {
            debug.update();
        }
    }
}
