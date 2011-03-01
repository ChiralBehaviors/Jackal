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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;

import com.hellblazer.anubis.util.Pair;

/**
 * The embodiment of the gossip protocol. This protocol replicates the Anubis
 * heartbeat state and forms both a member discovery and failure detection
 * service. Periodically, the protocol chooses a random member from the system
 * view and initiates a round of gossip with it. A round of gossip is push/pull
 * and involves 3 messages. For instance if node A wants to initiate a round of
 * gossip with node B it starts off by sending node B a Syn. Node B on receipt
 * of this message sends node A an Ack. On receipt of this message node A sends
 * node B an Ack2 which completes a round of gossip. When messages are recived,
 * the protocol updates the endpoint's failure detector with the liveness
 * information. If the endpoint's failure detector predicts that the endpoint
 * has failed, the endpoint is marked dead.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Gossip {
    private final static Logger log = Logger.getLogger(Gossip.class.getCanonicalName());

    private final double convictThreshold;
    private final Map<InetSocketAddress, Endpoint> endpointState = new ConcurrentHashMap<InetSocketAddress, Endpoint>();
    private final Random entropy;
    private final Endpoint localState;
    private final SystemView view;
    private final HeartbeatReceiver receiver;
    private final ExecutorService notificationService;

    public Gossip(HeartbeatReceiver heartbeatReceiver, SystemView systemView,
                  Random random, double phiConvictThreshold,
                  HeartbeatState initialState) {
        if (phiConvictThreshold < 5 || phiConvictThreshold > 16) {
            throw new IllegalArgumentException(
                                               "Phi conviction threshold must be between 5 and 16, inclusively");
        }
        receiver = heartbeatReceiver;
        convictThreshold = phiConvictThreshold;
        entropy = random;
        view = systemView;
        localState = new Endpoint(initialState);
        localState.markAlive();
        endpointState.put(view.getLocalAddress(), localState);
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

        for (Entry<InetSocketAddress, Endpoint> entry : endpointState.entrySet()) {
            InetSocketAddress endpoint = entry.getKey();
            if (endpoint.equals(view.getLocalAddress())) {
                continue;
            }

            Endpoint state = entry.getValue();
            if (state.interpret(now, convictThreshold) && state.isAlive()) {
                state.markDead();
                view.markDead(endpoint);
                if (log.isLoggable(Level.INFO)) {
                    log.info(format("InetSocketAddress %s is now dead.",
                                    endpoint));
                }
            }
            if (!state.isAlive()
                && view.cullUnreachable(endpoint, now - state.getUpdate())) {
                endpointState.remove(endpoint);
            }
        }
        view.cullQuarantined(now);
    }

    /**
     * The first message of the gossip protocol.
     * 
     * @param digests
     *            - the list of heartbeat state digests
     * @param from
     *            - the address of the gossiper
     * @return the pair of digests and heartbeat states to return to the sender
     */
    public Pair<List<Digest>, List<HeartbeatState>> gossip(Digest[] digests,
                                                           InetSocketAddress from) { 
        if (log.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (Digest gDigest : digests) {
                sb.append(gDigest);
                sb.append(" ");
            }
            log.finest(format("Gossip digests from %s are : %s", from, sb.toString()));
        }
        long now = System.currentTimeMillis();
        for (Digest gDigest : digests) {
            endpointState.get(gDigest.epAddress).record(now);
        } 
        sort(digests); 
        return examine(digests);
    }

    /**
     * Perform the periodic gossip.
     * 
     * @param communications
     *            - the mechanism to send the gossip message to a peer
     */
    public void gossipWith(GossipCommunications communications) {
        List<Digest> digests = randomDigests();
        if (digests.size() > 0) {
            InetSocketAddress member = view.getRandomLiveMember();
            if (member != null) {
                communications.gossip(digests, member);
            }

            InetSocketAddress unreachableMember = view.getRandomUnreachableMember();
            if (unreachableMember != null) {
                communications.gossip(digests, unreachableMember);
            }

            InetSocketAddress seedMember = view.getRandomSeedMember(member);
            if (seedMember != null) {
                communications.gossip(digests, seedMember);
            }

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Performing status check ...");
            }
            checkStatus();
        }
    }

    /**
     * The second message in the gossip protocol.
     * 
     * @param digests
     *            - the list of digests the gossiper would like to hear about
     * @param remoteStates
     *            - the list of heartbeat states the gossiper things is out of
     *            date on the receiver
     * @param from
     *            - the gossiper's address
     * @return a list of heartbeat states that the gossiper would like to hear
     *         about
     */
    public List<HeartbeatState> reply(Digest[] digests,
                                      HeartbeatState[] remoteStates,
                                      InetSocketAddress from) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received a GossipDigestAckMessage from %s", from));
        }

        if (remoteStates.length > 0) {
            long now = System.currentTimeMillis();
            for (HeartbeatState state : remoteStates) {
                Endpoint endpoint = endpointState.get(state.getSenderAddress());
                if (endpoint != null) {
                    endpoint.record(now);
                }
            }
            applyLocally(now, remoteStates);
        }

        List<HeartbeatState> deltaState = new ArrayList<HeartbeatState>();
        for (Digest digest : digests) {
            InetSocketAddress addr = digest.getEpAddress();
            HeartbeatState localState = getState(addr, digest.getMaxVersion());
            if (localState != null) {
                deltaState.add(localState);
            }
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Sending a GossipDigestAck2Message to %s", from));
        }
        return deltaState;
    }

    /**
     * The third message of the gossip protocol.
     * 
     * @param remoteStates
     *            - the list of updated heartbeat states we requested from our
     *            partner
     * @param from
     *            - the gossiper sending the update
     */
    public void update(HeartbeatState[] remoteStates, InetSocketAddress from) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received a GossipDigestAck2Message from %s",
                              from));
        }
        long now = System.currentTimeMillis();
        for (HeartbeatState state : remoteStates) {
            endpointState.get(state.getSenderAddress()).record(now);
        }
        applyLocally(now, remoteStates);
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

    private void applyLocally(long now, HeartbeatState[] remoteStates) {
        for (HeartbeatState remoteState : remoteStates) {
            InetSocketAddress endpoint = remoteState.getSenderAddress();
            if (endpoint.equals(view.getLocalAddress())) {
                continue;
            }
            if (view.isQuarantined(endpoint)) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest(format("Ignoring gossip for %s because it is quarantined",
                                      endpoint));
                }
                continue;
            }

            Endpoint localState = endpointState.get(endpoint);
            if (localState != null) {
                long localEpoch = localState.getEpoch();
                long remoteEpoch = remoteState.getEpoch();
                if (log.isLoggable(Level.FINEST)) {
                    log.finest(format("%s local epoch %s, remote epoch %s",
                                      endpoint, localEpoch, remoteEpoch));
                }

                if (remoteEpoch > localEpoch) {
                    handleStateChange(remoteState);
                } else if (remoteEpoch == localEpoch) {
                    if (remoteState.getViewNumber() > localState.getViewNumber()) {
                        long oldVersion = localState.getHeartbeatVersion();
                        localState.updateState(now, remoteState);
                        notifyReceiver(localState.getState());
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest(format("Updating heartbeat state version to %s from %s for %s  ...",
                                              localState.getHeartbeatVersion(),
                                              oldVersion, endpoint));
                        }
                    } else if (log.isLoggable(Level.FINEST)) {
                        log.finest(format("Ignoring remote version %s <= %s for %s",
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
                handleStateChange(remoteState);
            }
        }
    }

    private Pair<List<Digest>, List<HeartbeatState>> examine(Digest[] digests) {
        List<Digest> deltaDigests = new ArrayList<Digest>();
        List<HeartbeatState> deltaState = new ArrayList<HeartbeatState>();
        for (Digest digest : digests) {
            long remoteEpoch = digest.getEpoch();
            long maxRemoteVersion = digest.getMaxVersion();
            Endpoint state = endpointState.get(digest.getEpAddress());
            if (state != null) {
                long localEpoch = state.getEpoch();
                long maxLocalVersion = state.getHeartbeatVersion();
                if (remoteEpoch == localEpoch
                    && maxRemoteVersion == maxLocalVersion) {
                    continue;
                }

                if (remoteEpoch > localEpoch) {
                    deltaDigests.add(new Digest(digest.getEpAddress(),
                                                remoteEpoch, 0));
                } else if (remoteEpoch < localEpoch) {
                    HeartbeatState localState = getState(digest.getEpAddress(),
                                                         0);
                    if (localState != null) {
                        deltaState.add(localState);
                    }
                } else if (remoteEpoch == localEpoch) {
                    if (maxRemoteVersion > maxLocalVersion) {
                        deltaDigests.add(new Digest(digest.getEpAddress(),
                                                    remoteEpoch,
                                                    maxLocalVersion));
                    } else if (maxRemoteVersion < maxLocalVersion) {
                        HeartbeatState localState = getState(digest.getEpAddress(),
                                                             maxRemoteVersion);
                        if (localState != null) {
                            deltaState.add(localState);
                        }
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

    private HeartbeatState getState(InetSocketAddress endpoint, long viewNumber) {
        Endpoint epState = endpointState.get(endpoint);
        if (epState != null && epState.getViewNumber() > viewNumber) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest(format("local heartbeat view number %s greater than %s for %s ",
                                  epState.getViewNumber(), viewNumber, endpoint));
            }
            return epState.getState();
        }
        return null;
    }

    private void handleStateChange(HeartbeatState state) {
        if (endpointState.get(state.getSenderAddress()) != null) {
            log.info(format("Node %s has restarted, now up again",
                            state.getSenderAddress()));
        } else {
            log.info(format("Node %s is now part of the cluster",
                            state.getSenderAddress()));
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Adding endpoint state for %s",
                              state.getSenderAddress()));
        }
        Endpoint endpoint = new Endpoint(state);
        endpointState.put(state.getSenderAddress(), endpoint);
        endpoint.markAlive();
        view.markAlive(state.getSenderAddress());
        if (log.isLoggable(Level.INFO)) {
            log.info(format("InetSocketAddress %s is now UP", endpoint));
        }
    }

    private void notifyReceiver(final HeartbeatState state) {
        notificationService.execute(new Runnable() {
            @Override
            public void run() {
                receiver.receiveHeartbeat(state);
            }
        });
    }

    /**
     * The gossip digest is built based on randomization rather than just
     * looping through the collection of live endpoints.
     * 
     * @param digests
     *            list of Gossip Digests.
     */
    private List<Digest> randomDigests() {
        ArrayList<Digest> digests = new ArrayList<Digest>();
        for (Entry<InetSocketAddress, Endpoint> entry : endpointState.entrySet()) {
            digests.add(new Digest(entry.getKey(), entry.getValue()));
        }
        Collections.shuffle(digests, entropy);
        if (log.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (Digest gDigest : digests) {
                sb.append(gDigest);
                sb.append(" ");
            }
            log.finest(format("Gossip Digests are : %s" + sb.toString()));
        }
        return digests;
    }

    /**
     * First construct a map whose key is the endpoint in the GossipDigest and
     * the value is the GossipDigest itself. Then build a list of version
     * differences i.e difference between the version in the GossipDigest and
     * the version in the local state for a given InetSocketAddress. Sort this
     * list. Now loop through the sorted list and retrieve the GossipDigest
     * corresponding to the endpoint from the map that was initially
     * constructed.
     */
    private void sort(Digest[] digests) {
        Map<InetSocketAddress, Digest> endpoint2digest = new HashMap<InetSocketAddress, Digest>();
        for (Digest digest : digests) {
            endpoint2digest.put(digest.getEpAddress(), digest);
        }

        Digest[] diffDigests = new Digest[digests.length];
        int i = 0;
        for (Digest gDigest : digests) {
            InetSocketAddress ep = gDigest.getEpAddress();
            Endpoint epState = endpointState.get(ep);
            long version = epState != null ? epState.getHeartbeatVersion() : 0;
            long diffVersion = Math.abs(version - gDigest.getMaxVersion());
            diffDigests[i++] = new Digest(ep, gDigest.getEpoch(), diffVersion);
        }

        Arrays.sort(diffDigests);
        int j = 0;
        for (i = diffDigests.length - 1; i >= 0; --i) {
            digests[j++] = endpoint2digest.get(diffDigests[i].getEpAddress());
        }
    }
}
