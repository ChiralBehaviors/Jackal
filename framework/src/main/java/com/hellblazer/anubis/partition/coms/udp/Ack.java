package com.hellblazer.anubis.partition.coms.udp;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;

/**
 * This message gets sent out as a result of the receipt of a
 * Syn by an endpoint. This is the 2 stage of the 3 way
 * messaging in the Gossip protocol.
 */

public class Ack extends Message {
    final List<GossipDigest> digests;
    final Map<InetAddress, EndpointState> states;

    Ack(List<GossipDigest> digests,
                           Map<InetAddress, EndpointState> states) {
        this.digests = digests;
        this.states = states;
    }

    Map<InetAddress, EndpointState> getEndpointStateMap() {
        return states;
    }

    @Override
    public byte[] getMessageBody() {
        // TODO Auto-generated method stub
        return null;
    }
}