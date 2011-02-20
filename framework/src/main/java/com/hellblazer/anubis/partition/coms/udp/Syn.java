package com.hellblazer.anubis.partition.coms.udp;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the first message that gets sent out as a start of the Gossip
 * protocol in a round.
 */

public class Syn extends Message {
    final String id;
    final List<GossipDigest> digests;

    public Syn(String id, List<GossipDigest> digests) {
        this.id = id;
        this.digests = digests;
    }

    List<GossipDigest> getGossipDigests() {
        return digests;
    }

    @Override
    public byte[] getMessageBody() {
        // TODO Auto-generated method stub
        return null;
    }
}