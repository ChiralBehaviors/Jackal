package com.hellblazer.anubis.partition.coms.udp;

import java.net.InetAddress;
import java.util.Map;

/**
 * This message gets sent out as a result of the receipt of a
 * Ack. This the last stage of the 3 way messaging of the
 * Gossip protocol.
 */

public class Ack2 extends Message{ 
    final Map<InetAddress, EndpointState> state;

    Ack2(Map<InetAddress, EndpointState> state) {
        this.state = state;
    }

    Map<InetAddress, EndpointState> getEndpointStateMap() {
        return state;
    }

    @Override
    public byte[] getMessageBody() {
        // TODO Auto-generated method stub
        return null;
    }
}