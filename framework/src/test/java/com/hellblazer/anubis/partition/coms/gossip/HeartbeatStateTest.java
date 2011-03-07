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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.views.View;

/**
 * Basic testing of the heartbeat state
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class HeartbeatStateTest extends TestCase {
    public void testBasic() throws Exception {
        NodeIdSet msgLinks = new NodeIdSet();
        Identity candidate = new Identity(0x1638, Identity.MAX_ID, 667);
        boolean preferred = true;
        Identity sender = new Identity(0x1638, 23, 22);
        InetSocketAddress senderAddress = new InetSocketAddress("google.com",
                                                                80);
        boolean stable = true;
        InetSocketAddress testInterface = new InetSocketAddress("google.com",
                                                                443);
        NodeIdSet v = new NodeIdSet();
        long viewNumber = 128;
        long viewTimestamp = 990876;

        msgLinks.add(candidate.id);
        msgLinks.add(sender.id);
        v.add(candidate.id);
        HeartbeatState state = new HeartbeatState(candidate, msgLinks,
                                                  preferred, sender,
                                                  senderAddress, stable,
                                                  testInterface, v, viewNumber,
                                                  viewTimestamp);
        assertSame(msgLinks, state.getMsgLinks());
        assertSame(sender, state.getSender());
        assertSame(senderAddress, state.getSenderAddress());
        assertSame(testInterface, state.getTestInterface());
        assertEquals(preferred, state.isPreferred());
        assertTrue(state.getMsgLinks().contains(candidate.id));
        assertTrue(state.getMsgLinks().contains(sender.id));

        View view = state.getView();

        assertNotNull(view);
        assertTrue(view.contains(candidate));
        assertFalse(view.contains(sender));
        assertEquals(viewNumber, state.getViewNumber());
        assertEquals(viewTimestamp, state.getTime());
        assertEquals(viewTimestamp, state.getTime());
        assertEquals(stable, view.isStable());

        byte[] bytes = new byte[GossipMessages.HEARTBEAT_STATE_BYTE_SIZE];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        state.writeTo(msg);
        msg.flip();

        HeartbeatState dState = new HeartbeatState(msg);
        assertEquals(msgLinks, dState.getMsgLinks());
        assertEquals(sender, dState.getSender());
        assertEquals(senderAddress, dState.getSenderAddress());
        assertEquals(testInterface, dState.getTestInterface());
        assertEquals(preferred, dState.isPreferred());
        assertTrue(dState.getMsgLinks().contains(candidate.id));
        assertTrue(dState.getMsgLinks().contains(sender.id));

        view = dState.getView();

        assertNotNull(view);
        assertTrue(view.contains(candidate));
        assertFalse(view.contains(sender));
        assertEquals(viewNumber, dState.getViewNumber());
        assertEquals(viewTimestamp, dState.getTime());
        assertEquals(viewTimestamp, dState.getTime());
        assertEquals(stable, view.isStable());
    }
}
