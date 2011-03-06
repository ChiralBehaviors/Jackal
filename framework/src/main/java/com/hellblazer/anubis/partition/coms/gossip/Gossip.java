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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;

import com.hellblazer.anubis.partition.coms.gossip.Digest.DigestComparator;
import com.hellblazer.anubis.util.Pair;

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
public class Gossip {
    private final static Logger log = Logger.getLogger(Gossip.class.getCanonicalName());

    private final ConnectionService connectionService;
    private final double convictThreshold;
    private final ConcurrentMap<InetSocketAddress, Endpoint> endpoints = new ConcurrentHashMap<InetSocketAddress, Endpoint>();
    private final Random entropy;
    private final Endpoint localState;
    private final ExecutorService notificationService;
    private final HeartbeatReceiver receiver;
    private final SystemView view;

    /**
     * 
     * @param gossipConnectionService
     *            - the service which creates outbound connections to other
     *            members
     * @param heartbeatReceiver
     *            - the reciever of newly acquired heartbeat state
     * @param systemView
     *            - the system management view of the member state
     * @param random
     *            - a source of entropy
     * @param phiConvictThreshold
     *            - the threshold required to convict a failed member. The value
     *            should be between 5 and 16, inclusively.
     * @param initialState
     *            - the initial heartbeat state of the local member
     */
    public Gossip(ConnectionService gossipConnectionService,
                  HeartbeatReceiver heartbeatReceiver, SystemView systemView,
                  Random random, double phiConvictThreshold,
                  HeartbeatState initialState) {
        connectionService = gossipConnectionService;
        receiver = heartbeatReceiver;
        convictThreshold = phiConvictThreshold;
        entropy = random;
        view = systemView;
        localState = new Endpoint(initialState);
        localState.markAlive();
        endpoints.put(view.getLocalAddress(), localState);
        notificationService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "Anubis heartbeat notification service");
                daemon.setDaemon(true);
                daemon.setPriority(Thread.MAX_PRIORITY);
                return daemon;
            }
        });
    }

    public void checkStatus() {
        long now = System.currentTimeMillis();
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Checking the status of the living...");
        }
        for (Entry<InetSocketAddress, Endpoint> entry : endpoints.entrySet()) {
            InetSocketAddress endpoint = entry.getKey();
            if (endpoint.equals(view.getLocalAddress())) {
                continue;
            }

            Endpoint state = entry.getValue();
            if (state.isAlive() && state.interpret(now, convictThreshold)) {
                state.markDead();
                view.markDead(endpoint);
                if (log.isLoggable(Level.INFO)) {
                    log.info(format("Endpoint %s is now dead.", endpoint));
                }
            } else if (!state.isAlive()
                       && view.cullUnreachable(endpoint,
                                               now - state.getUpdate())) {
                if (log.isLoggable(Level.INFO)) {
                    log.info(format("Culling unreachable endpoint %s", endpoint));
                }
                endpoints.remove(endpoint);
            }
        }
        if (log.isLoggable(Level.INFO)) {
            log.info("Culling the quarantined...");
        }
        view.cullQuarantined(now);
    }

    /**
     * Perform the periodic gossip.
     * 
     * @param connectionService
     *            - the mechanism to send the gossip message to a peer
     */
    public void gossip() {
        List<Digest> digests = randomDigests();
        if (digests.size() > 0) {
            InetSocketAddress member = gossipWithTheLiving(digests);
            gossipWithTheDead(digests);
            gossipWithSeeds(digests, member);
            checkStatus();
        }
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
     * @return the pair of digests and heartbeat states to return to the sender
     */
    public Pair<List<Digest>, List<HeartbeatState>> gossip(List<Digest> digests) {
        long now = System.currentTimeMillis();
        for (Digest gDigest : digests) {
            Endpoint endpoint = endpoints.get(gDigest.getEpAddress());
            if (endpoint != null) {
                endpoint.record(now);
            }
        }
        sort(digests);
        return examine(digests);
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
     * @return a list of heartbeat states that the gossiper would like updates
     *         for
     */
    public List<HeartbeatState> reply(List<Digest> digests,
                                      List<HeartbeatState> remoteStates) {
        if (remoteStates.size() > 0) {
            long now = System.currentTimeMillis();
            for (HeartbeatState state : remoteStates) {
                Endpoint endpoint = endpoints.get(state.getSenderAddress());
                if (endpoint != null) {
                    endpoint.record(now);
                }
            }
            apply(now, remoteStates);
        }

        List<HeartbeatState> deltaState = new ArrayList<HeartbeatState>();
        for (Digest digest : digests) {
            InetSocketAddress addr = digest.getEpAddress();
            addUpdatedState(deltaState, addr, digest.getViewNumber());
        }
        return deltaState;
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
        long now = System.currentTimeMillis();
        for (HeartbeatState state : remoteStates) {
            Endpoint endpoint = endpoints.get(state.getSenderAddress());
            if (endpoint != null) {
                endpoint.record(now);
            }
        }
        apply(now, remoteStates);
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
        Endpoint endpoint = endpoints.get(address);
        if (endpoint != null) {
            endpoint.getHandler().gossip(digests);
            return address;
        }
        return null;
    }

    private void addUpdatedState(List<HeartbeatState> deltaState,
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

    private void apply(long now, List<HeartbeatState> remoteStates) {
        for (HeartbeatState remoteState : remoteStates) {
            InetSocketAddress endpoint = remoteState.getSenderAddress();
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
                long localEpoch = localState.getEpoch();
                long remoteEpoch = remoteState.getEpoch();
                if (log.isLoggable(Level.FINEST)) {
                    log.finest(format("%s local epoch %s, remote epoch %s",
                                      endpoint, localEpoch, remoteEpoch));
                }

                if (remoteEpoch >= localEpoch) {
                    if (remoteState.getViewNumber() > localState.getViewNumber()) {
                        long oldViewNumber = localState.getViewNumber();
                        localState.updateState(now, remoteState);
                        notifyReceiver(localState.getState());
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest(format("Updating heartbeat state view number to %s from %s for %s  ...",
                                              localState.getViewNumber(),
                                              oldViewNumber, endpoint));
                        }
                    } else if (log.isLoggable(Level.FINEST)) {
                        log.finest(format("Ignoring remote view number %s <= %s for %s",
                                          remoteState.getViewNumber(),
                                          localState.getViewNumber(), endpoint));
                    }
                } else {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest(format("Ignoring remote epoch %s < %s"
                                          + remoteEpoch, localEpoch));
                    }
                }
            } else {
                discover(remoteState);
            }
        }
    }

    private void connect(final InetSocketAddress address,
                         final Endpoint endpoint, Runnable connectAction) {
        try {
            endpoint.setCommunications(connectionService.connect(address,
                                                                 connectAction));
        } catch (IOException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING,
                        format("Cannot connect to endpoint %s", address), e);
            }
        }
    }

    /**
     * Connect and gossip with a member that isn't currently connected.
     * 
     * @param address
     *            - the address to connect to
     * @param digests
     *            - the digests in question
     */
    private void connectAndGossipWith(final InetSocketAddress address,
                                      final List<Digest> digests) {
        final Endpoint newEndpoint = new Endpoint(new HeartbeatState(address));
        Runnable connectAction = new Runnable() {
            @Override
            public void run() {
                Endpoint previous = endpoints.putIfAbsent(address, newEndpoint);
                if (previous != null) {
                    newEndpoint.getHandler().close();
                    if (log.isLoggable(Level.INFO)) {
                        log.info(format("Endpoint already established for %s",
                                        newEndpoint));
                    }
                    return;
                }
                view.markAlive(address);
                if (log.isLoggable(Level.INFO)) {
                    log.info(format("Member %s is now UP", newEndpoint));
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
    private void discover(final HeartbeatState state) {
        final InetSocketAddress address = state.getSenderAddress();
        final Endpoint endpoint = new Endpoint(state);
        Runnable connectAction = new Runnable() {
            @Override
            public void run() {
                Endpoint previous = endpoints.putIfAbsent(address, endpoint);
                if (previous != null) {
                    endpoint.getHandler().close();
                    if (log.isLoggable(Level.INFO)) {
                        log.info(format("Endpoint already established for %s",
                                        endpoint));
                    }
                    return;
                }
                view.markAlive(address);
                if (log.isLoggable(Level.INFO)) {
                    log.info(format("Member %s is now UP", endpoint));
                }
                notifyReceiver(endpoint.getState());
            }

        };
        connect(address, endpoint, connectAction);
    }

    private Pair<List<Digest>, List<HeartbeatState>> examine(List<Digest> digests) {
        List<Digest> deltaDigests = new ArrayList<Digest>();
        List<HeartbeatState> deltaState = new ArrayList<HeartbeatState>();
        for (Digest digest : digests) {
            long remoteEpoch = digest.getEpoch();
            long remoteViewNumber = digest.getViewNumber();
            Endpoint state = endpoints.get(digest.getEpAddress());
            if (state != null) {
                long localEpoch = state.getEpoch();
                long localViewNumber = state.getViewNumber();
                if (remoteEpoch == localEpoch
                    && remoteViewNumber == localViewNumber) {
                    continue;
                }

                if (remoteEpoch > localEpoch) {
                    deltaDigests.add(new Digest(digest.getEpAddress(),
                                                remoteEpoch, 0L));
                } else if (remoteEpoch < localEpoch) {
                    addUpdatedState(deltaState, digest.getEpAddress(), 0L);
                } else if (remoteEpoch == localEpoch) {
                    if (remoteViewNumber > localViewNumber) {
                        deltaDigests.add(new Digest(digest.getEpAddress(),
                                                    remoteEpoch,
                                                    localViewNumber));
                    } else if (remoteViewNumber < localViewNumber) {
                        addUpdatedState(deltaState, digest.getEpAddress(),
                                        remoteViewNumber);
                    }
                }
            } else {
                deltaDigests.add(new Digest(digest.getEpAddress(), remoteEpoch,
                                            0));
            }
        }
        return new Pair<List<Digest>, List<HeartbeatState>>(deltaDigests,
                                                            deltaState);
    }

    private void notifyReceiver(final HeartbeatState state) {
        notificationService.execute(new Runnable() {
            @Override
            public void run() {
                receiver.receiveHeartbeat(state);
            }
        });
    }

    private List<Digest> randomDigests() {
        ArrayList<Digest> digests = new ArrayList<Digest>();
        for (Entry<InetSocketAddress, Endpoint> entry : endpoints.entrySet()) {
            digests.add(new Digest(entry.getKey(), entry.getValue()));
        }
        Collections.shuffle(digests, entropy);
        if (log.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (Digest gDigest : digests) {
                sb.append(gDigest);
                sb.append(" ");
            }
            log.finest(format("Gossip digests are : %s" + sb.toString()));
        }
        return digests;
    }

    private void sort(List<Digest> digests) {
        Map<InetSocketAddress, Digest> endpoint2digest = new HashMap<InetSocketAddress, Digest>();
        for (Digest digest : digests) {
            endpoint2digest.put(digest.getEpAddress(), digest);
        }

        Digest[] diffDigests = new Digest[digests.size()];
        int i = 0;
        for (Digest gDigest : digests) {
            InetSocketAddress ep = gDigest.getEpAddress();
            Endpoint state = endpoints.get(ep);
            long viewNumber = state != null ? state.getViewNumber() : 0;
            long diffViewNumber = Math.abs(viewNumber - gDigest.getViewNumber());
            diffDigests[i++] = new Digest(ep, gDigest.getEpoch(),
                                          diffViewNumber);
        }

        Arrays.sort(diffDigests, new DigestComparator());
        i = 0;
        for (int j = diffDigests.length - 1; j >= 0; --j) {
            digests.set(i++, endpoint2digest.get(diffDigests[j].getEpAddress()));
        }
        if (log.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (Digest gDigest : digests) {
                sb.append(gDigest);
                sb.append(" ");
            }
            log.finest(format("Sorted gossip digests are : %s" + sb.toString()));
        }
    }
}
