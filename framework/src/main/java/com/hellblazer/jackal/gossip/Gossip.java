/** 
 * (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.jackal.gossip;

import static java.lang.String.format;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsIntf;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

import com.hellblazer.jackal.gossip.Digest.DigestComparator;

/**
 * The embodiment of the gossip protocol. This protocol replicates the Anubis
 * heartbeat state and forms both a member discovery and failure detection
 * service. Periodically, the protocol chooses a random member from the system
 * view and initiates a round of gossip with it. A round of gossip is push/pull
 * and involves 3 messages. For example, if node A wants to initiate a round of
 * gossip with node B it starts off by sending node B a gossip message
 * containing a digest of the view number state of the local view of the
 * heartbeat state. Node B on receipt of this message sends node A a reply
 * containing a list of digests representing the updated heartbeat state
 * required, based on the received digests. In addition, the node also sends
 * along a list of updated heartbeat state that is more recent, based on the
 * initial list of digests. On receipt of this message node A sends node B the
 * requested heartbeat state that completes a round of gossip. When messages are
 * received, the protocol updates the endpoint's failure detector with the
 * liveness information. If the endpoint's failure detector predicts that the
 * endpoint has failed, the endpoint is marked dead.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Gossip implements HeartbeatCommsIntf, HeartbeatCommsFactory {
    private final static Logger                              log       = Logger.getLogger(Gossip.class.getCanonicalName());

    private final GossipCommunications                       communications;
    private final ConcurrentMap<InetSocketAddress, Endpoint> endpoints = new ConcurrentHashMap<InetSocketAddress, Endpoint>();
    private final Random                                     entropy;
    private final Endpoint                                   localState;
    private final SystemView                                 view;
    private ScheduledFuture<?>                               gossipTask;
    private final int                                        interval;
    private final TimeUnit                                   intervalUnit;
    private final ScheduledExecutorService                   scheduler;
    private final ExecutorService                            dispatcher;
    private HeartbeatReceiver                                receiver;
    private final AtomicReference<View>                      ignoring  = new AtomicReference<View>();
    private final AtomicBoolean                              running   = new AtomicBoolean();
    private final FailureDetectorFactory                     fdFactory;

    /**
     * 
     * @param heartbeatReceiver
     *            - the reciever of newly acquired heartbeat state
     * @param systemView
     *            - the system management view of the member state
     * @param random
     *            - a source of entropy
     * @param communicationsService
     *            - the service which creates outbound connections to other
     *            members
     * @param gossipInterval
     *            - the period of the random gossiping
     * @param unit
     *            - time unit for the gossip interval
     * @param failureDetectorFactory
     *            - the factory producing instances of the failure detector
     */
    public Gossip(SystemView systemView, Random random,
                  GossipCommunications communicationsService,
                  int gossipInterval, TimeUnit unit,
                  FailureDetectorFactory failureDetectorFactory) {
        communications = communicationsService;
        communications.setGossip(this);
        entropy = random;
        view = systemView;
        interval = gossipInterval;
        intervalUnit = unit;
        fdFactory = failureDetectorFactory;
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "Anubis: Gossip heartbeat servicing thread");
                daemon.setDaemon(true);
                daemon.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.WARNING, "Uncaught exceptiion", e);
                    }
                });
                return daemon;
            }
        });
        dispatcher = Executors.newFixedThreadPool(3, new ThreadFactory() {
            volatile int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "Anubis: Gossip dispatching thread "
                                                   + count++);
                daemon.setDaemon(true);
                daemon.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.WARNING, "Uncaught exceptiion", e);
                    }
                });
                return daemon;
            }
        });
        localState = new Endpoint();
    }

    public void checkStatus() {
        long now = System.currentTimeMillis();
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Checking the status of the living...");
        }
        for (Iterator<Entry<InetSocketAddress, Endpoint>> iterator = endpoints.entrySet().iterator(); iterator.hasNext();) {
            Entry<InetSocketAddress, Endpoint> entry = iterator.next();
            InetSocketAddress endpoint = entry.getKey();
            if (endpoint.equals(view.getLocalAddress())) {
                continue;
            }

            Endpoint state = entry.getValue();
            if (state.isAlive() && state.shouldConvict(now)) {
                iterator.remove();
                state.markDead();
                view.markDead(endpoint, now);
                if (log.isLoggable(Level.FINER)) {
                    log.finer(format("Endpoint %s is now DEAD on node: %s",
                                     state.getMemberString(),
                                     localState.getMemberString()));
                }
            }
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Culling the quarantined and unreachable...");
        }
        view.cullQuarantined(now);
        view.cullUnreachable(now);
    }

    @Override
    public HeartbeatCommsIntf create(HeartbeatReceiver hbReceiver) {
        receiver = hbReceiver;
        return this;
    }

    public InetSocketAddress getLocalAddress() {
        return view.getLocalAddress();
    }

    @Override
    public String getThreadStatusString() {
        return "Anubis: Gossip heartbeat/discovery, running: " + running.get();
    }

    /**
     * Perform the periodic gossip.
     * 
     * @param communications
     *            - the mechanism to send the gossip message to a peer
     */
    public void gossip() {
        List<Digest> digests = randomDigests();
        if (digests.size() > 0) {
            InetSocketAddress member = gossipWithTheLiving(digests);
            gossipWithTheDead(digests);
            gossipWithSeeds(digests, member);
        }
        checkStatus();
    }

    /**
     * The first message of the gossip protocol. The gossiping node sends a set
     * of digests of it's view of the heartbeat state. The receiver replies with
     * a list of digests indicating the heartbeat state that needs to be updated
     * on the receiver. The receiver of the gossip also sends along any
     * heartbeat states which are more recent than what the gossiper sent, based
     * on the digests provided by the gossiper.
     * 
     * @param digests
     *            - the list of heartbeat state digests
     * @param gossipHandler
     *            - the handler to send the reply of digests and heartbeat
     *            states
     */
    public void gossip(List<Digest> digests, GossipMessages gossipHandler) {
        sort(digests);
        examine(digests, gossipHandler);
    }

    @Override
    public boolean isIgnoring(Identity id) {
        final View theShunned = ignoring.get();
        if (theShunned == null) {
            return false;
        }
        return theShunned.contains(id);
    }

    public boolean isIgnoring(InetSocketAddress address) {
        Endpoint endpoint = endpoints.get(address);
        if (endpoint == null) {
            return false;
        }
        return isIgnoring(endpoint.getState().getSender());
    }

    /**
     * The second message in the gossip protocol. This message is sent in reply
     * to the initial gossip message sent by this node. The response is a list
     * of digests that represent the heartbeat state that is out of date on the
     * sender. In addition, the sender also supplies heartbeat state that is
     * more recent than the digests supplied in the initial gossip message.
     * 
     * @param digests
     *            - the list of digests the gossiper would like to hear about
     * @param remoteStates
     *            - the list of heartbeat states the gossiper thinks is out of
     *            date on the receiver
     * @param gossipHandler
     *            - the handler to send a list of heartbeat states that the
     *            gossiper would like updates for
     */
    public void reply(List<Digest> digests, List<HeartbeatState> remoteStates,
                      GossipMessages gossipHandler) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(String.format("Member: %s receiving reply digests: %s states: %s",
                                     getId(), digests, remoteStates));
        }
        apply(remoteStates);

        List<HeartbeatState> deltaState = new ArrayList<HeartbeatState>();
        for (Digest digest : digests) {
            InetSocketAddress addr = digest.getAddress();
            addUpdatedState(deltaState, addr, digest.getTime());
        }
        if (!deltaState.isEmpty()) {
            if (log.isLoggable(Level.INFO)) {
                log.finest(String.format("Member: %s sending update states: %s",
                                         getId(), deltaState));
            }
            gossipHandler.update(deltaState);
        }
    }

    @Override
    public void sendHeartbeat(Heartbeat heartbeat) {
        assert heartbeat.getSender().id >= 0;
        boolean initial = false;
        if (localState.getState() == null) {
            initial = true;
        }
        localState.updateState(HeartbeatState.toHeartbeatState(heartbeat,
                                                               view.getLocalAddress()).clone());
        if (initial) {
            endpoints.put(view.getLocalAddress(), localState);
        }
    }

    @Override
    public void setIgnoring(View ignoringUpdate) {
        ignoring.set(ignoringUpdate);
    }

    public boolean shouldConvict(InetSocketAddress address, long now) {
        Endpoint endpoint = endpoints.get(address);
        return endpoint == null || isIgnoring(endpoint.getState().getSender())
               || endpoint.shouldConvict(now);
    }

    @Override
    @PostConstruct
    public void start() {
        if (running.compareAndSet(false, true)) {
            communications.start();
            gossipTask = scheduler.scheduleWithFixedDelay(gossipTask(),
                                                          interval, interval,
                                                          intervalUnit);
        }
    }

    @Override
    @PreDestroy
    public void terminate() {
        if (running.compareAndSet(true, false)) {
            communications.terminate();
            scheduler.shutdownNow();
            gossipTask.cancel(true);
            gossipTask = null;
        }
    }

    /**
     * The third message of the gossip protocol. This is the final message in
     * the gossip protocol. The supplied heartbeat state is the updated state
     * requested by the receiver in response to the digests in the original
     * gossip message.
     * 
     * @param remoteStates
     *            - the list of updated heartbeat states we requested from our
     *            partner
     */
    public void update(List<HeartbeatState> remoteStates) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(String.format("Member: %s receiving update states: %s",
                                     getId(), remoteStates));
        }
        apply(remoteStates);
    }

    protected void addUpdatedState(List<HeartbeatState> deltaState,
                                   InetSocketAddress endpoint, long time) {
        Endpoint state = endpoints.get(endpoint);
        if (state != null && state.getTime() > time) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest(format("local heartbeat time stamp %s greater than %s for %s ",
                                  state.getTime(), time, endpoint));
            }
            deltaState.add(state.getState());
        }
    }

    protected void apply(List<HeartbeatState> remoteStates) {
        for (HeartbeatState remoteState : remoteStates) {
            InetSocketAddress endpoint = remoteState.getHeartbeatAddress();
            if (endpoint.equals(view.getLocalAddress())) {
                continue;
            }
            if (view.isQuarantined(endpoint)) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest(format("Ignoring gossip for %s because it is a quarantined endpoint",
                                      remoteState));
                }
                continue;
            }
            Endpoint localState = endpoints.get(endpoint);
            if (localState != null) {
                if (remoteState.getTime() > localState.getTime()) {
                    long oldTime = localState.getTime();
                    localState.record(remoteState);
                    notifyUpdate(localState.getState());
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest(format("Updating heartbeat state time stamp to %s from %s for %s",
                                          localState.getTime(), oldTime,
                                          endpoint));
                    }
                }
            } else {
                discover(remoteState);
            }
        }
    }

    /**
     * Connect with a member
     * 
     * @param address
     *            - the address of the member
     * @param endpoint
     *            - the endpoint representing the member
     * @param connectAction
     *            - the action to take when the connection with the member is
     *            established
     */
    protected void connect(final InetSocketAddress address,
                           final Endpoint endpoint, Runnable connectAction) {
        try {
            communications.connect(address, endpoint, connectAction);
        } catch (IOException e) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE,
                        format("Cannot connect to endpoint %s", address), e);
            }
        }
    }

    /**
     * Connect and gossip with a member that isn't currently connected. As we
     * have no idea what state this member is in, we need to add a digest to the
     * list that is manifestly out of date so that the member, if it responds,
     * will update us with its state.
     * 
     * @param address
     *            - the address to connect to
     * @param digests
     *            - the digests in question
     */
    protected void connectAndGossipWith(final InetSocketAddress address,
                                        final List<Digest> digests) {
        final Endpoint newEndpoint = new Endpoint(new HeartbeatState(address),
                                                  fdFactory.create());
        Runnable connectAction = new Runnable() {
            @Override
            public void run() {
                Endpoint previous = endpoints.putIfAbsent(address, newEndpoint);
                if (previous != null) {
                    newEndpoint.getHandler().close();
                    if (log.isLoggable(Level.FINE)) {
                        log.fine(format("Endpoint already established for %s",
                                        newEndpoint.getMemberString()));
                    }
                    return;
                }
                view.markAlive(address);
                if (log.isLoggable(Level.FINE)) {
                    log.fine(format("Member %s is now CONNECTED",
                                    newEndpoint.getMemberString()));
                }
                List<Digest> newDigests = new ArrayList<Digest>(digests);
                newDigests.add(new Digest(address, -1));
                newEndpoint.getHandler().gossip(newDigests);
            }
        };
        connect(address, newEndpoint, connectAction);
    }

    /**
     * Discover a connection with a previously unconnected member
     * 
     * @param state
     *            - the heartbeat state from a previously unconnected member of
     *            the system view
     */
    protected void discover(final HeartbeatState state) {
        final InetSocketAddress address = state.getHeartbeatAddress();
        final Endpoint endpoint = new Endpoint(state, fdFactory.create());
        Runnable connectAction = new Runnable() {
            @Override
            public void run() {
                Endpoint previous = endpoints.putIfAbsent(address, endpoint);
                if (previous != null) {
                    endpoint.getHandler().close();
                    if (log.isLoggable(Level.FINE)) {
                        log.fine(format("Endpoint already established for %s",
                                        endpoint.getMemberString()));
                    }
                    return;
                }
                view.markAlive(address);
                if (log.isLoggable(Level.FINE)) {
                    log.fine(format("Member %s is now UP",
                                    endpoint.getMemberString()));
                }
                notifyUpdate(endpoint.getState());
            }

        };
        connect(address, endpoint, connectAction);
    }

    protected void examine(List<Digest> digests, GossipMessages gossipHandler) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(String.format("Member: %s receiving gossip digests: %s",
                                     getId(), digests));
        }
        List<Digest> deltaDigests = new ArrayList<Digest>();
        List<HeartbeatState> deltaState = new ArrayList<HeartbeatState>();
        for (Digest digest : digests) {
            long remoteTime = digest.getTime();
            Endpoint state = endpoints.get(digest.getAddress());
            if (state != null) {
                long localTime = state.getTime();
                if (remoteTime == localTime) {
                    continue;
                }
                if (remoteTime > localTime) {
                    deltaDigests.add(new Digest(digest.getAddress(), localTime));
                } else if (remoteTime < localTime) {
                    addUpdatedState(deltaState, digest.getAddress(), remoteTime);
                }
            } else {
                deltaDigests.add(new Digest(digest.getAddress(), -1));
            }
        }
        if (!deltaDigests.isEmpty() || !deltaState.isEmpty()) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest(String.format("Member: %s replying with digests: %s state: %s",
                                         getId(), deltaDigests, deltaState));
            }
            gossipHandler.reply(deltaDigests, deltaState);
        }
    }

    protected Identity getId() {
        return localState.getState().getSender();
    }

    protected Runnable gossipTask() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    gossip();
                } catch (Throwable e) {
                    log.log(Level.WARNING, "Exception while performing gossip",
                            e);
                }
            }
        };
    }

    /**
     * Gossip with one of the kernel members of the system view with some
     * probability. If the live member that we gossiped with is a seed member,
     * then don't worry about it.
     * 
     * @param digests
     *            - the digests to gossip.
     * @param member
     *            - the live member we've gossiped with.
     */
    protected void gossipWithSeeds(final List<Digest> digests,
                                   InetSocketAddress member) {
        InetSocketAddress address = view.getRandomSeedMember(member);
        if (address == null) {
            return;
        }
        Endpoint endpoint = endpoints.get(address);
        if (endpoint != null) {
            endpoint.getHandler().gossip(digests);
        } else {
            connectAndGossipWith(address, digests);
        }
    }

    /**
     * Gossip with a member who is currently considered dead, with some
     * probability.
     * 
     * @param digests
     *            - the digests of interest
     */
    protected void gossipWithTheDead(List<Digest> digests) {
        InetSocketAddress address = view.getRandomUnreachableMember();
        if (address == null) {
            return;
        }
        connectAndGossipWith(address, digests);
    }

    /**
     * Gossip with a live member of the view.
     * 
     * @param digests
     *            - the digests of interest
     * @return the address of the member contacted
     */
    protected InetSocketAddress gossipWithTheLiving(List<Digest> digests) {
        InetSocketAddress address = view.getRandomLiveMember();
        if (address == null) {
            return null;
        }
        Endpoint endpoint = endpoints.get(address);
        if (endpoint != null) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest(format("%s gossiping with: %s, #digests: %s",
                                  getId(), endpoint.getState().getSender(),
                                  digests.size()));
            }
            endpoint.getHandler().gossip(digests);
            return address;
        }
        if (log.isLoggable(Level.WARNING)) {
            log.warning(format("Inconsistent state!  View thinks %s is alive, but service has no endpoint!",
                               address));
        }
        view.markDead(address, System.currentTimeMillis());
        return null;
    }

    protected void notifyUpdate(final HeartbeatState state) {
        assert state != null;
        if (state.isDiscoveryOnly() || isIgnoring(state.getSender())) {
            return;
        }
        dispatcher.execute(new Runnable() {
            @Override
            public void run() {
                receiver.receiveHeartbeat(state);
            }
        });
    }

    protected List<Digest> randomDigests() {
        ArrayList<Digest> digests = new ArrayList<Digest>();
        for (Entry<InetSocketAddress, Endpoint> entry : endpoints.entrySet()) {
            digests.add(new Digest(entry.getKey(), entry.getValue()));
        }
        Collections.shuffle(digests, entropy);
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Gossip digests are : %s", digests));
        }
        return digests;
    }

    protected void sort(List<Digest> digests) {
        Map<InetSocketAddress, Digest> endpoint2digest = new HashMap<InetSocketAddress, Digest>();
        for (Digest digest : digests) {
            endpoint2digest.put(digest.getAddress(), digest);
        }

        Digest[] diffDigests = new Digest[digests.size()];
        int i = 0;
        for (Digest gDigest : digests) {
            InetSocketAddress ep = gDigest.getAddress();
            Endpoint state = endpoints.get(ep);
            long time = state != null ? state.getTime() : -1;
            long diffTime = Math.abs(time - gDigest.getTime());
            diffDigests[i++] = new Digest(ep, diffTime);
        }

        Arrays.sort(diffDigests, new DigestComparator());
        i = 0;
        for (int j = diffDigests.length - 1; j >= 0; --j) {
            digests.set(i++, endpoint2digest.get(diffDigests[j].getAddress()));
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Sorted gossip digests are : %s", digests));
        }
    }

    public void record(long t, InetSocketAddress address) {
        Endpoint endpoint = endpoints.get(address);
        if (endpoint != null) {
            endpoint.record(t);
        }
    }

    public void update(Heartbeat hb, InetSocketAddress address) {
        Endpoint endpoint = endpoints.get(address);
        if (endpoint != null) {
            endpoint.updateState(HeartbeatState.toHeartbeatState(hb, address));
        }
    }
}
