package com.hellblazer.anubis.partition.coms.gossip;

import java.net.InetSocketAddress;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;

import junit.framework.TestCase;

public class EndpointTest extends TestCase {
    public void testBasic() throws Exception {
        NodeIdSet v = new NodeIdSet();
        HeartbeatState state = new HeartbeatState(
                                                  new Identity(0x1638,
                                                               Identity.MAX_ID,
                                                               667),
                                                  new NodeIdSet(),
                                                  true,
                                                  new Identity(0x1638, 23, 22),
                                                  new InetSocketAddress(
                                                                        "google.com",
                                                                        80),
                                                  true,
                                                  new InetSocketAddress(
                                                                        "google.com",
                                                                        443),
                                                  v, (long) 128, (long) 990876);
        
        Endpoint ep = new Endpoint(state); 
        long now = 100;
        ep.record(now);
        assertFalse(ep.interpret(now, 1));
        now = now + 10000;
        assertFalse(ep.interpret(now, 1));
    }
}
