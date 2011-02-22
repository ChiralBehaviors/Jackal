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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A gossip protocol to replicate Anubis heartbeat state. Periodically, the
 * protocol chooses a random member from the system view and initiates a round
 * of gossip with it. A round of gossip is push/pull and involves 3 messages.
 * For instance if node A wants to initiate a round of gossip with node B it
 * starts off by sending node B a Syn. Node B on receipt of this message sends
 * node A an Ack. On receipt of this message node A sends node B an Ack2 which
 * completes a round of gossip. When messages are recived, the protocol updates
 * the endpoint's failure detector with the liveness information. If the
 * endpoint's failure detector predicts that the endpoint has failed, the
 * endpoint is marked dead.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Gossip {
    private final static Logger log = Logger.getLogger(Gossip.class.getCanonicalName());

    private final double convictThreshold;
    private final Map<InetAddress, EndpointState> endpointState = new ConcurrentHashMap<InetAddress, EndpointState>();
    private final Random entropy;
    private final EndpointState localState;
    private final AtomicInteger version = new AtomicInteger(0);

    private final SystemView view;

    public Gossip(int generation, SystemView systemView, Random random,
                  double phiConvictThreshold) {
        if (phiConvictThreshold < 5 || phiConvictThreshold > 16) {
            throw new IllegalArgumentException(
                                               "Phi conviction threshold must be between 5 and 16, inclusively");
        }
        convictThreshold = phiConvictThreshold;
        entropy = random;
        view = systemView;
        localState = new EndpointState(generation);
        localState.markAlive();
        endpointState.put(view.getLocalAddress(), localState);
    }

    /**
     * Add an endpoint we knew about previously, but whose state is unknown
     */
    public void addSavedEndpoint(InetAddress endpoint) {
        EndpointState state = endpointState.get(endpoint);
        if (state == null) {
            state = new EndpointState(0);
            state.markDead();
            endpointState.put(endpoint, state);
            view.markUnreachable(endpoint);
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Adding saved endpoint " + endpoint + " "
                           + state.getGeneration());
            }
        }
    }

    public void checkStatus() {
        long now = System.currentTimeMillis();

        for (Entry<InetAddress, EndpointState> entry : endpointState.entrySet()) {
            InetAddress endpoint = entry.getKey();
            if (endpoint.equals(view.getLocalAddress())) {
                continue;
            }

            EndpointState state = entry.getValue();
            if (state.interpret(convictThreshold) && state.isAlive()) {
                state.markDead();
                view.markDead(endpoint);
                if (log.isLoggable(Level.INFO)) {
                    log.info(format("InetAddress %s is now dead.", endpoint));
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
     * Answer a random member of the view's live set.
     * 
     * @return the live member, or null if there are no live members
     */
    public InetAddress getRandomLiveMember() {
        return view.getRandomLiveMember();
    }

    /**
     * Answer a random member of the seed set
     * 
     * @param member
     *            - the member that has just been gossiped with
     * @return a random member of the seed set, if appropriate, or null
     */
    public InetAddress getRandomSeedMember(InetAddress member) {
        return view.getRandomSeedMember(member);
    }

    /**
     * Answer a random member of the view's unreachable set.
     * 
     * @return the unreachable member selected, or null if none selected or
     *         available
     */
    public InetAddress getRandomUnreachableMember() {
        return view.getRandomUnreachableMember();
    }

    public Ack2 handle(Ack ack, InetAddress from) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received a GossipDigestAckMessage from %s", from));
        }
        List<Digest> gDigestList = ack.digests;
        Map<InetAddress, EndpointState> remoteStates = ack.getEndpointStates();

        if (remoteStates.size() > 0) {
            for (Entry<InetAddress, EndpointState> entry : remoteStates.entrySet()) {
                endpointState.get(entry.getKey()).record(entry.getValue());
            }
            applyLocally(remoteStates);
        }

        Map<InetAddress, EndpointState> deltaState = new HashMap<InetAddress, EndpointState>();
        for (Digest gDigest : gDigestList) {
            InetAddress addr = gDigest.getEndpoint();
            EndpointState localState = getState(addr, gDigest.getMaxVersion());
            if (localState != null) {
                deltaState.put(addr, localState);
            }
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Sending a GossipDigestAck2Message to %s", from));
        }
        return new Ack2(deltaState);
    }

    public void handle(Ack2 ack2, InetAddress from) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received a GossipDigestAck2Message from %s",
                              from));
        }
        Map<InetAddress, EndpointState> remoteStates = ack2.getEndpointStates();
        for (Entry<InetAddress, EndpointState> entry : remoteStates.entrySet()) {
            endpointState.get(entry.getKey()).record(entry.getValue());
        }
        applyLocally(remoteStates);
    }

    public Ack handle(Syn syn, InetAddress from) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received a GossipDigestSynMessage from %s", from));
        }

        List<Digest> digests = syn.getGossipDigests();
        if (log.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (Digest gDigest : digests) {
                sb.append(gDigest);
                sb.append(" ");
            }
            log.finest(format("Gossip syn digests are : %s" + sb.toString()));
        }
        for (Digest gDigest : digests) {
            endpointState.get(gDigest.endpoint).record(endpointState.get(gDigest.endpoint));
        }

        sort(digests);

        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Sending a GossipDigestAckMessage to %s", from));
        }

        return examine(digests);
    }

    public boolean isKnown(InetAddress endpoint) {
        return endpointState.containsKey(endpoint);
    }

    public void nextVersion() {
        localState.updateHeartbeatVersion(version.incrementAndGet());
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
        EndpointState epState;
        int generation = 0;
        int maxVersion = 0;

        List<InetAddress> endpoints = new ArrayList<InetAddress>(
                                                                 endpointState.keySet());
        Collections.shuffle(endpoints, entropy);
        for (InetAddress endpoint : endpoints) {
            epState = endpointState.get(endpoint);
            if (epState != null) {
                generation = epState.getGeneration();
                maxVersion = epState.getHeartbeatVersion();
            }
            digests.add(new Digest(endpoint, generation, maxVersion));
        }

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

    private void apply(InetAddress endpoint, EndpointState localState,
                       EndpointState remoteState) {
        int oldVersion = localState.getHeartbeatVersion();
        localState.setHeartBeatState(remoteState.getHeartBeatState());
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Updating heartbeat state version to "
                       + localState.getHeartbeatVersion() + " from "
                       + oldVersion + " for " + endpoint + " ...");
        }
    }

    private void applyLocally(Map<InetAddress, EndpointState> remoteStates) {
        for (Entry<InetAddress, EndpointState> entry : remoteStates.entrySet()) {
            InetAddress endpoint = entry.getKey();
            if (endpoint.equals(view.getLocalAddress())) {
                continue;
            }
            if (view.isQuarantined(endpoint)) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("Ignoring gossip for " + endpoint
                               + " because it is quarantined");
                }
                continue;
            }

            EndpointState localState = endpointState.get(endpoint);
            EndpointState remoteState = entry.getValue();
            if (localState != null) {
                int localGeneration = localState.getGeneration();
                int remoteGeneration = remoteState.getGeneration();
                if (log.isLoggable(Level.FINEST)) {
                    log.finest(endpoint + "local generation " + localGeneration
                               + ", remote generation " + remoteGeneration);
                }

                if (remoteGeneration > localGeneration) {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Updating heartbeat state generation to "
                                   + remoteGeneration + " from "
                                   + localGeneration + " for " + endpoint);
                    }
                    handleMajorStateChange(endpoint, remoteState);
                } else if (remoteGeneration == localGeneration) {
                    int localMaxVersion = localState.getHeartbeatVersion();
                    int remoteMaxVersion = remoteState.getHeartbeatVersion();
                    if (remoteMaxVersion > localMaxVersion) {
                        apply(endpoint, localState, remoteState);
                    } else if (log.isLoggable(Level.FINEST)) {
                        log.finest("Ignoring remote version "
                                   + remoteMaxVersion + " <= "
                                   + localMaxVersion + " for " + endpoint);
                    }
                } else {
                    if (log.isLoggable(Level.FINEST)) {
                        log.finest("Ignoring remote generation "
                                   + remoteGeneration + " < " + localGeneration);
                    }
                }
            } else {
                handleMajorStateChange(endpoint, remoteState);
            }
        }
    }

    private Ack examine(List<Digest> digests) {
        List<Digest> deltaDigests = new ArrayList<Digest>();
        Map<InetAddress, EndpointState> deltaState = new HashMap<InetAddress, EndpointState>();
        for (Digest digest : digests) {
            int remoteGeneration = digest.getGeneration();
            int maxRemoteVersion = digest.getMaxVersion();
            EndpointState state = endpointState.get(digest.getEndpoint());
            if (state != null) {
                int localGeneration = state.getGeneration();
                int maxLocalVersion = state.getHeartbeatVersion();
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
                        deltaDigests.add(new Digest(digest.getEndpoint(),
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
        return new Ack(deltaDigests, deltaState);
    }

    private EndpointState getState(InetAddress endpoint, int version) {
        EndpointState epState = endpointState.get(endpoint);
        EndpointState reqdEndpointState = null;

        if (epState != null && epState.getHeartbeatVersion() > version) {
            reqdEndpointState = new EndpointState(epState.getHeartBeatState());
            if (log.isLoggable(Level.FINEST)) {
                log.finest("local heartbeat version "
                           + epState.getHeartbeatVersion() + " greater than "
                           + version + " for " + endpoint);
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
    private void handleMajorStateChange(InetAddress endpoint,
                                        EndpointState state) {
        if (endpointState.get(endpoint) != null) {
            log.info(format("Node %s has restarted, now UP again", endpoint));
        } else {
            log.info(format("Node %s is now part of the cluster", endpoint));
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Adding endpoint state for " + endpoint);
        }
        endpointState.put(endpoint, state);
        state.markAlive();
        view.markAlive(endpoint);
        if (log.isLoggable(Level.INFO)) {
            log.info(format("InetAddress %s is now UP", endpoint));
        }
    }

    private void requestAll(Digest digest, List<Digest> delta,
                            int remoteGeneration) {
        delta.add(new Digest(digest.getEndpoint(), remoteGeneration, 0));
        if (log.isLoggable(Level.FINEST)) {
            log.finest("requestAll for " + digest.getEndpoint());
        }
    }

    private void sendAll(Digest digest, Map<InetAddress, EndpointState> delta,
                         int maxRemoteVersion) {
        EndpointState localState = getState(digest.getEndpoint(),
                                            maxRemoteVersion);
        if (localState != null) {
            delta.put(digest.getEndpoint(), localState);
        }
    }

    /**
     * First construct a map whose key is the endpoint in the GossipDigest and
     * the value is the GossipDigest itself. Then build a list of version
     * differences i.e difference between the version in the GossipDigest and
     * the version in the local state for a given InetAddress. Sort this list.
     * Now loop through the sorted list and retrieve the GossipDigest
     * corresponding to the endpoint from the map that was initially
     * constructed.
     */
    private void sort(List<Digest> digests) {
        Map<InetAddress, Digest> endpoint2digest = new HashMap<InetAddress, Digest>();
        for (Digest gDigest : digests) {
            endpoint2digest.put(gDigest.getEndpoint(), gDigest);
        }

        List<Digest> diffDigests = new ArrayList<Digest>();
        for (Digest gDigest : digests) {
            InetAddress ep = gDigest.getEndpoint();
            EndpointState epState = endpointState.get(ep);
            int version = epState != null ? epState.getHeartbeatVersion() : 0;
            int diffVersion = Math.abs(version - gDigest.getMaxVersion());
            diffDigests.add(new Digest(ep, gDigest.getGeneration(), diffVersion));
        }

        digests.clear();
        Collections.sort(diffDigests);
        int size = diffDigests.size();
        for (int i = size - 1; i >= 0; --i) {
            digests.add(endpoint2digest.get(diffDigests.get(i).getEndpoint()));
        }
    }
}
