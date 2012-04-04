package com.hellblazer.jackal.gossip;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;

public class Ring {
    private final GossipCommunications               comms;
    private final AtomicReference<InetSocketAddress> neighbor = new AtomicReference<InetSocketAddress>();
    private final int                                id;
    private static final Logger                      log      = LoggerFactory.getLogger(Ring.class.getCanonicalName());

    public Ring(int identity, GossipCommunications comms) {
        id = identity;
        this.comms = comms;
    }

    /**
     * Send the heartbeat around the ring in both directions.
     * 
     * @param state
     */
    public void send(HeartbeatState state) {
        InetSocketAddress l = neighbor.get();
        if (l == null) {
            if (log.isTraceEnabled()) {
                log.trace("Ring has not been formed, not forwarding state");
            }
            return;
        }
        comms.send(state, l);
    }

    /**
     * Update the neighboring members of the id on the ring represented by the
     * members.
     * 
     * @param members
     * @param endpoints
     */
    public void update(NodeIdSet members, Collection<Endpoint> endpoints) {
        int n = members.leftNeighborOf(id);
        if (n == -1) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("id {%s} does not have a left neighbor in: %s",
                                        id, members));
            }
            return;
        }
        InetSocketAddress l = null;
        for (Endpoint endpoint : endpoints) {
            Identity identity = endpoint.getId();
            if (identity != null) {
                int eid = identity.id;
                if (eid == n) {
                    l = endpoint.getState().getHeartbeatAddress();
                    break;
                }
            }
        }
        if (l == null) {
            if (log.isTraceEnabled()) {
                log.trace("Ring has not been formed");
            }
            neighbor.set(null);
        } else {
            neighbor.set(l);
        }
    }
}
