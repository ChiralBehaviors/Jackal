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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import junit.framework.TestCase;

import com.hellblazer.anubis.basiccomms.nio.AbstractCommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;

/**
 * Basic testing of the gossip handler
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class GossipHandlerTest extends TestCase {
    public void testDeliverGossip() throws Exception {
        Gossip gossip = mock(Gossip.class);
        SocketChannel channel = mock(SocketChannel.class);
        ServerChannelHandler handler = mock(ServerChannelHandler.class);

        Digest digest1 = new Digest(new InetSocketAddress("google.com", 0), 0,
                                    0);
        Digest digest2 = new Digest(new InetSocketAddress("google.com", 1), 0,
                                    1);
        Digest digest3 = new Digest(new InetSocketAddress("google.com", 2), 2,
                                    0);
        Digest digest4 = new Digest(new InetSocketAddress("google.com", 3), 0,
                                    3);
        byte[] bytes = new byte[4 * GossipMessages.DIGEST_BYTE_SIZE
                                + AbstractCommunicationsHandler.HEADER_SIZE + 1
                                + 4];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        msg.position(AbstractCommunicationsHandler.HEADER_SIZE);
        msg.put(GossipMessages.GOSSIP);
        msg.putInt(4);
        digest1.writeTo(msg);
        digest2.writeTo(msg);
        digest3.writeTo(msg);
        digest4.writeTo(msg);

        GossipHandler gHandler = new GossipHandler(gossip, handler, channel);

        gHandler.deliver(bytes);

        verify(gossip).gossip(Arrays.asList(digest1, digest2, digest3, digest4));
    }

    public void testDeliverReply() throws Exception {
        Gossip gossip = mock(Gossip.class);
        SocketChannel channel = mock(SocketChannel.class);
        ServerChannelHandler handler = mock(ServerChannelHandler.class);

        Digest digest1 = new Digest(new InetSocketAddress("google.com", 0), 0,
                                    0);
        Digest digest2 = new Digest(new InetSocketAddress("google.com", 1), 0,
                                    1);
        Digest digest3 = new Digest(new InetSocketAddress("google.com", 2), 2,
                                    0);
        Digest digest4 = new Digest(new InetSocketAddress("google.com", 3), 0,
                                    3);
        byte[] bytes = new byte[4 * GossipMessages.DIGEST_BYTE_SIZE
                                + AbstractCommunicationsHandler.HEADER_SIZE + 1
                                + 4];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        msg.position(AbstractCommunicationsHandler.HEADER_SIZE);
        msg.put(GossipMessages.GOSSIP);
        msg.putInt(4);
        digest1.writeTo(msg);
        digest2.writeTo(msg);
        digest3.writeTo(msg);
        digest4.writeTo(msg);

        GossipHandler gHandler = new GossipHandler(gossip, handler, channel);

        gHandler.deliver(bytes);

        verify(gossip).gossip(Arrays.asList(digest1, digest2, digest3, digest4));
    }

    public void testDeliverUpdate() throws Exception {

    }
}
