/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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

import java.net.InetSocketAddress;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;

import junit.framework.TestCase;

/**
 * Basic testing of the endpoint
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
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
        assertFalse(ep.shouldConvict(now, 1));
        now = now + 10000;
    }
}
