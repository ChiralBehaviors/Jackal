package com.hellblazer.anubis.partition.coms.gossip;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

import com.hellblazer.anubis.util.Pair;

public interface GossipCommunications {

    /**
     * The first message of the gossip protocol. Send a list of the shuffled
     * digests of the receiver's view of the endpoint state
     * 
     * @param digests
     * @param member
     */
    void send(List<Digest> digests, InetSocketAddress member);

    /**
     * Send the required delta state to the gossip member. This is the 3rd
     * message in the gossip protocol
     * 
     * @param deltaState
     * @param to
     */
    void send(Map<InetSocketAddress, Endpoint> deltaState, InetSocketAddress to);

    /**
     * The 3rd message in the gossip protocol
     * 
     * @param ack
     * @param from
     */
    void send(Pair<List<Digest>, Map<InetSocketAddress, Endpoint>> ack,
              InetSocketAddress from);

}