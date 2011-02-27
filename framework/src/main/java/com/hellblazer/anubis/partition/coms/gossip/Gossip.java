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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hellblazer.anubis.util.Pair;

/**
 * The embodiment of the gossip protocol. This protocol replicate Anubis
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

    public Gossip(int generation, SystemView systemView, Random random,
                  double phiConvictThreshold, HeartbeatState initialState) {
        if (phiConvictThreshold < 5 || phiConvictThreshold > 16) {
            throw new IllegalArgumentException(
                                               "Phi conviction threshold must be between 5 and 16, inclusively");
        }
        convictThreshold = phiConvictThreshold;
        entropy = random;
        view = systemView;
        localState = new Endpoint(initialState);
        localState.markAlive();
        endpointState.put(view.getLocalAddress(), localState);
    }

    /**
     * The second message of the gossip protocol.
     * 
     * @param remoteStates
     * @param from
     */
    public void ack(Map<InetSocketAddress, Endpoint> remoteStates,
                    InetSocketAddress from) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received a GossipDigestAck2Message from %s",
                              from));
        }
        long now = System.currentTimeMillis();
        for (Entry<InetSocketAddress, Endpoint> entry : remoteStates.entrySet()) {
            endpointState.get(entry.getKey()).record(now);
        }
        applyLocally(now, remoteStates);
    }

    public Map<InetSocketAddress, Endpoint> ack2(List<Digest> digests,
                                                 Map<InetSocketAddress, Endpoint> remoteStates,
                                                 InetSocketAddress from) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received a GossipDigestAckMessage from %s", from));
        }

        if (remoteStates.size() > 0) {
            long now = System.currentTimeMillis();
            for (Entry<InetSocketAddress, Endpoint> entry : remoteStates.entrySet()) {
                endpointState.get(entry.getKey()).record(now);
            }
            applyLocally(now, remoteStates);
        }

        Map<InetSocketAddress, Endpoint> deltaState = new HashMap<InetSocketAddress, Endpoint>();
        for (Digest gDigest : digests) {
            InetSocketAddress addr = gDigest.getEpAddress();
            Endpoint localState = getState(addr, gDigest.getMaxVersion());
            if (localState != null) {
                deltaState.put(addr, localState);
            }
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Sending a GossipDigestAck2Message to %s", from));
        }
        return deltaState;
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

    public void gossip(GossipCommunications communications) {
        List<Digest> digests = randomDigests();
        if (digests.size() > 0) {
            InetSocketAddress member = view.getRandomLiveMember();
            if (member != null) {
                communications.send(digests, member);
            }

            InetSocketAddress unreachableMember = view.getRandomUnreachableMember();
            if (unreachableMember != null) {
                communications.send(digests, unreachableMember);
            }

            InetSocketAddress seedMember = view.getRandomSeedMember(member);
            if (seedMember != null) {
                communications.send(digests, seedMember);
            }

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Performing status check ...");
            }
            checkStatus();
        }
    }

    public boolean isKnown(InetSocketAddress endpoint) {
        return endpointState.containsKey(endpoint);
    }

    /**
     * The gossip digest is built based on randomization rather than just
     * looping through the collection of live endpoints.
     * 
     * @param digests
     *            list of Gossip Digests.
     */
    public List<Digest> randomDigests() {
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
     * The first message of the gossip protocol.
     * 
     * @param digests
     * @param from
     * @return
     */
    public Pair<List<Digest>, Map<InetSocketAddress, Endpoint>> synchronize(Digest[] digests,
                                                                            InetSocketAddress from) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received a GossipDigestSynMessage from %s", from));
        }

        if (log.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (Digest gDigest : digests) {
                sb.append(gDigest);
                sb.append(" ");
            }
            log.finest(format("Gossip syn digests are : %s" + sb.toString()));
        }
        long now = System.currentTimeMillis();
        for (Digest gDigest : digests) {
            endpointState.get(gDigest.epAddress).record(now);
        }

        sort(digests);

        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Sending a GossipDigestAckMessage to %s", from));
        }

        return examine(digests);
    }

    /**
     * Update the local heartbeat state vector.
     * 
     * @param updatedState
     *            - the new heartbeat state
     */
    public void update(HeartbeatState updatedState) {
        localState.updateState(System.currentTimeMillis(), updatedState);
    }

    private void applyLocally(long now,
                              Map<InetSocketAddress, Endpoint> remoteStates) {
        for (Entry<InetSocketAddress, Endpoint> entry : remoteStates.entrySet()) {
            InetSocketAddress endpoint = entry.getKey();
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
            Endpoint remoteState = entry.getValue();
            if (localState != null) {
                long localGeneration = localState.getGeneration();
                long remoteGeneration = remoteState.getGeneration();
                if (log.isLoggable(Level.FINEST)) {
                    log.finest(format("%s local generation %s, remote generation %s",
                                      endpoint, localGeneration,
                                      remoteGeneration));
                }

                if (remoteGeneration > localGeneration) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest(format("Updating heartbeat state generation to %s from %s for %s",
                                          remoteGeneration, localGeneration,
                                          endpoint));
                    }
                    handleMajorStateChange(endpoint, remoteState);
                } else if (remoteGeneration == localGeneration) {
                    if (remoteState.getHeartbeatVersion() > localState.getHeartbeatVersion()) {
                        long oldVersion = localState.getHeartbeatVersion();
                        localState.updateState(now, remoteState.getState());
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest(format("Updating heartbeat state version to %s from %s for %s  ...",
                                              localState.getHeartbeatVersion(),
                                              oldVersion, endpoint));
                        }
                    } else if (log.isLoggable(Level.FINEST)) {
                        log.finest(format("Ignoring remote version %s <= %s for %s",
                                          remoteState.getHeartbeatVersion(),
                                          localState.getHeartbeatVersion(),
                                          endpoint));
                    }
                } else {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest(format("Ignoring remote generation %s < %s"
                                          + remoteGeneration, localGeneration));
                    }
                }
            } else {
                handleMajorStateChange(endpoint, remoteState);
            }
        }
    }

    private Pair<List<Digest>, Map<InetSocketAddress, Endpoint>> examine(Digest[] digests) {
        List<Digest> deltaDigests = new ArrayList<Digest>();
        Map<InetSocketAddress, Endpoint> deltaState = new HashMap<InetSocketAddress, Endpoint>();
        for (Digest digest : digests) {
            long remoteGeneration = digest.getEpoch();
            long maxRemoteVersion = digest.getMaxVersion();
            Endpoint state = endpointState.get(digest.getEpAddress());
            if (state != null) {
                long localGeneration = state.getGeneration();
                long maxLocalVersion = state.getHeartbeatVersion();
                if (remoteGeneration == localGeneration
                    && maxRemoteVersion == maxLocalVersion) {
                    continue;
                }

                if (remoteGeneration > localGeneration) {
                    requestAll(digest, deltaDigests, remoteGeneration);
                } else if (remoteGeneration < localGeneration) {
                    sendAll(digest, deltaState, 0);
                } else if (remoteGeneration == localGeneration) {
                    if (maxRemoteVersion > maxLocalVersion) {
                        deltaDigests.add(new Digest(digest.getEpAddress(),
                                                    remoteGeneration,
                                                    maxLocalVersion));
                    } else if (maxRemoteVersion < maxLocalVersion) {
                        sendAll(digest, deltaState, maxRemoteVersion);
                    }
                }
            } else {
                requestAll(digest, deltaDigests, remoteGeneration);
            }
        }
        return new Pair<List<Digest>, Map<InetSocketAddress, Endpoint>>(
                                                                        deltaDigests,
                                                                        deltaState);
    }

    private Endpoint getState(InetSocketAddress endpoint, long version) {
        Endpoint epState = endpointState.get(endpoint);
        Endpoint reqdEndpointState = null;

        if (epState != null && epState.getHeartbeatVersion() > version) {
            reqdEndpointState = new Endpoint(epState.getState());
            if (log.isLoggable(Level.FINEST)) {
                log.finest(format("local heartbeat version %s greater than %s for %s ",
                                  epState.getHeartbeatVersion(), version,
                                  endpoint));
            }
        }
        return reqdEndpointState;
    }

    /**
     * This method is called whenever there is a "big" change in ep state (a
     * generation change for a known node).
     * 
     * @param endpoint
     *            endpoint
     * @param state
     *            EndpointState for the endpoint
     */
    private void handleMajorStateChange(InetSocketAddress endpoint,
                                        Endpoint state) {
        if (endpointState.get(endpoint) != null) {
            log.info(format("Node %s has restarted, now UP again", endpoint));
        } else {
            log.info(format("Node %s is now part of the cluster", endpoint));
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Adding endpoint state for %s", endpoint));
        }
        endpointState.put(endpoint, state);
        state.markAlive();
        view.markAlive(endpoint);
        if (log.isLoggable(Level.INFO)) {
            log.info(format("InetSocketAddress %s is now UP", endpoint));
        }
    }

    private void requestAll(Digest digest, List<Digest> delta, long remoteEpoch) {
        delta.add(new Digest(digest.getEpAddress(), remoteEpoch, 0));
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("requestAll for %s", digest.getEpAddress()));
        }
    }

    private void sendAll(Digest digest, Map<InetSocketAddress, Endpoint> delta,
                         long maxRemoteVersion) {
        Endpoint localState = getState(digest.getEpAddress(), maxRemoteVersion);
        if (localState != null) {
            delta.put(digest.getEpAddress(), localState);
        }
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
