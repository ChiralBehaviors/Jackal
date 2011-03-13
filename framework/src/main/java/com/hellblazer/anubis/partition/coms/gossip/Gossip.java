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
package com.hellblazer.anubis.partition.coms.gossip;

import static java.lang.String.format;

import java.io.IOException;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsIntf;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

import com.hellblazer.anubis.partition.coms.gossip.Digest.DigestComparator;

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
    private final double                                     convictThreshold;
    private final ConcurrentMap<InetSocketAddress, Endpoint> endpoints = new ConcurrentHashMap<InetSocketAddress, Endpoint>();
    private final Random                                     entropy;
    private final Endpoint                                   localState;
    private final SystemView                                 view;
    private ScheduledFuture<?>                               gossipTask;
    private final int                                        interval;
    private final TimeUnit                                   intervalUnit;
    private final ScheduledExecutorService                   scheduler;
    private HeartbeatReceiver                                receiver;
    private final AtomicReference<View>                      ignoring  = new AtomicReference<View>();
    private final AtomicBoolean                              running   = new AtomicBoolean();

    /**
     * 
     * @param communicationsService
     *            - the service which creates outbound connections to other
     *            members and notifies interested parties of updates
     * @param heartbeatReceiver
     *            - the reciever of newly acquired heartbeat state
     * @param systemView
     *            - the system management view of the member state
     * @param random
     *            - a source of entropy
     * @param phiConvictThreshold
     *            - the threshold required to convict a failed member. The value
     *            should be between 5 and 16, inclusively.
     * @param localIdentity
     *            - the identity of the local member
     */
    public Gossip(SystemView systemView, Random random,
                  double phiConvictThreshold, Identity localIdentity,
                  GossipCommunications communicationsService,
                  int gossipInterval, TimeUnit unit) {
        communications = communicationsService;
        communications.setGossip(this);
        convictThreshold = phiConvictThreshold;
        entropy = random;
        view = systemView;
        interval = gossipInterval;
        intervalUnit = unit;
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "Anubis: Gossip heartbeat servicing thread");
                daemon.setDaemon(true);
                return daemon;
            }
        });
        localState = new Endpoint(new HeartbeatState(null, localIdentity,
                                                     view.getLocalAddress()));
        localState.markAlive();
        endpoints.put(view.getLocalAddress(), localState);
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
            if (state.isAlive() && state.shouldConvict(now, convictThreshold)) {
                iterator.remove();
                state.markDead();
                view.markDead(endpoint, now);
                if (log.isLoggable(Level.INFO)) {
                    log.info(format("Endpoint %s is now DEAD.", endpoint));
                }
            }
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Culling the quarantined...");
        }
        view.cullQuarantined(now);
        view.cullUnreachable(now);
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
    public void gossip(List<Digest> digests, GossipHandler gossipHandler) {
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
                      GossipHandler gossipHandler) {
        apply(remoteStates);

        List<HeartbeatState> deltaState = new ArrayList<HeartbeatState>();
        for (Digest digest : digests) {
            InetSocketAddress addr = digest.getAddress();
            addUpdatedState(deltaState, addr, digest.getViewNumber());
        }
        if (!deltaState.isEmpty()) {
            gossipHandler.update(deltaState);
        }
    }

    @Override
    public void sendHeartbeat(Heartbeat heartbeat) {
        updateLocalState(HeartbeatState.toHeartbeatState(heartbeat,
                                                         communications.getLocalAddress()));
    }

    @Override
    public void setIgnoring(View ignoringUpdate) {
        ignoring.set(ignoringUpdate);
    }

    public HeartbeatCommsIntf create(HeartbeatReceiver hbReceiver) {
        receiver = hbReceiver;
        return this;
    }

    public boolean shouldConvict(InetSocketAddress address, long now) {
        Endpoint endpoint = endpoints.get(address);
        if (endpoint == null) {
            return true; // not a live member if it ain't in the endpoint set
        }
        return endpoint.shouldConvict(now, convictThreshold);
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            communications.start();
            gossipTask = scheduler.scheduleWithFixedDelay(gossipTask(),
                                                          interval, interval,
                                                          intervalUnit);
        }
    }

    @Override
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
        apply(remoteStates);
    }

    /**
     * Update the local heartbeat state vector.
     * 
     * @param updatedState
     *            - the new heartbeat state
     */
    public void updateLocalState(HeartbeatState updatedState) {
        localState.updateState(System.currentTimeMillis(), updatedState);
    }

    protected void addUpdatedState(List<HeartbeatState> deltaState,
                                   InetSocketAddress endpoint, long viewNumber) {
        Endpoint state = endpoints.get(endpoint);
        if (state != null && state.getViewNumber() > viewNumber) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest(format("local heartbeat view number %s greater than %s for %s ",
                                  state.getViewNumber(), viewNumber, endpoint));
            }
            deltaState.add(state.getState());
        }
    }

    protected void apply(List<HeartbeatState> remoteStates) {
        long now = System.currentTimeMillis();
        for (HeartbeatState remoteState : remoteStates) {
            InetSocketAddress endpoint = remoteState.getHeartbeatAddress();
            if (endpoint.equals(view.getLocalAddress())) {
                continue;
            }
            if (view.isQuarantined(endpoint)) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest(format("Ignoring gossip for %s because it is a quarantined endpoint",
                                      endpoint));
                }
                continue;
            }

            Endpoint localState = endpoints.get(endpoint);
            if (localState != null) {
                if (remoteState.getEpoch() >= localState.getEpoch()) {
                    if (remoteState.getViewNumber() > localState.getViewNumber()) {
                        long oldViewNumber = localState.getViewNumber();
                        localState.updateState(now, remoteState);
                        notifyUpdate(localState.getState());
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest(format("Updating heartbeat state view number to %s from %s for %s",
                                              localState.getViewNumber(),
                                              oldViewNumber, endpoint));
                        }
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
        final Endpoint newEndpoint = new Endpoint(new HeartbeatState(address));
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
                    log.fine(format("Member %s is now connected",
                                    newEndpoint.getMemberString()));
                }
                List<Digest> newDigests = new ArrayList<Digest>(digests);
                newDigests.add(new Digest(address, newEndpoint));
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
        final Endpoint endpoint = new Endpoint(state);
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

    protected void examine(List<Digest> digests, GossipHandler gossipHandler) {
        List<Digest> deltaDigests = new ArrayList<Digest>();
        List<HeartbeatState> deltaState = new ArrayList<HeartbeatState>();
        for (Digest digest : digests) {
            long remoteEpoch = digest.getEpoch();
            long remoteViewNumber = digest.getViewNumber();
            Endpoint state = endpoints.get(digest.getAddress());
            if (state != null) {
                long localEpoch = state.getEpoch();
                long localViewNumber = state.getViewNumber();
                if (remoteEpoch == localEpoch
                    && remoteViewNumber == localViewNumber) {
                    continue;
                }

                if (remoteEpoch > localEpoch) {
                    deltaDigests.add(new Digest(digest.getAddress(),
                                                remoteEpoch, 0L));
                } else if (remoteEpoch < localEpoch) {
                    addUpdatedState(deltaState, digest.getAddress(), -1L);
                } else if (remoteEpoch == localEpoch) {
                    if (remoteViewNumber > localViewNumber) {
                        deltaDigests.add(new Digest(digest.getAddress(),
                                                    remoteEpoch,
                                                    localViewNumber));
                    } else if (remoteViewNumber < localViewNumber) {
                        addUpdatedState(deltaState, digest.getAddress(),
                                        remoteViewNumber);
                    }
                }
            } else {
                deltaDigests.add(new Digest(digest.getAddress(), remoteEpoch,
                                            -1));
            }
        }
        gossipHandler.reply(deltaDigests, deltaState);
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
            endpoint.getHandler().gossip(digests);
            return address;
        } else {
            if (log.isLoggable(Level.WARNING)) {
                log.warning(format("Inconsistent state!  View thinks %s is alive, but service has no endpoint!",
                                   address));
            }
            view.markDead(address, System.currentTimeMillis());
        }
        return null;
    }

    protected void notifyUpdate(final HeartbeatState state) {
        if (state == null || isIgnoring(state.getSender())) {
            return;
        }
        communications.dispatch(new Runnable() {
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
            long viewNumber = state != null ? state.getViewNumber() : 0;
            long diffViewNumber = Math.abs(viewNumber - gDigest.getViewNumber());
            diffDigests[i++] = new Digest(ep, gDigest.getEpoch(),
                                          diffViewNumber);
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
}
