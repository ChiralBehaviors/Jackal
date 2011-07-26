package com.hellblazer.jackal.gossip;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;

public class Ring {
    private final GossipCommunications                 comms;
    private final AtomicReference<InetSocketAddress[]> neighbors = new AtomicReference<InetSocketAddress[]>();
    private final int                                  id;
    private static final Logger                        log       = Logger.getLogger(Ring.class.getCanonicalName());

    public Ring(int identity, GossipCommunications comms) {
        this.id = identity;
        this.comms = comms;
    }

    /**
     * Update the neighboring members of the id on the ring represented by the
     * members.
     * 
     * @param members
     * @param endpoints
     */
    public void update(NodeIdSet members, Collection<Endpoint> endpoints) {
        int[] n = members.neighborsOf(id);
        if (n == null) {
            if (log.isLoggable(Level.FINE)) {
                log.fine(String.format("id {%s} is not a member of: %s", id,
                                       members));
            }
            return;
        }
        InetSocketAddress l = null;
        InetSocketAddress r = null;
        for (Endpoint endpoint : endpoints) {
            Identity identity = endpoint.getId();
            if (identity != null) {
                int eid = identity.id;
                if (eid == n[0]) {
                    l = endpoint.getState().getHeartbeatAddress();
                } else if (eid == n[1]) {
                    r = endpoint.getState().getHeartbeatAddress();
                }
            }
            if (l != null && r != null) {
                break;
            }
        }
        if (l == null || r == null) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Ring has not been formed");
            }
            neighbors.set(null);
        } else {
            neighbors.set(new InetSocketAddress[] { l, r });
        }
    }

    /**
     * Send the heartbeat around the ring in both directions.
     * 
     * @param state
     */
    public void send(HeartbeatState state) {
        InetSocketAddress[] lr = neighbors.get();
        if (lr == null) {
            if (log.isLoggable(Level.FINE)) {
                log.fine("Ring has not been formed, not forwarding state");
            }
            return;
        }
        assert lr[0] != null && lr[1] != null : String.format("Found a null address: %s, %s",
                                                              lr[0], lr[1]);
        comms.send(state, lr[0], lr[1]);
    }
}
