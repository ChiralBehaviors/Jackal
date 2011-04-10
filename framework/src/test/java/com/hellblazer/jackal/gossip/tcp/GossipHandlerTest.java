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
package com.hellblazer.jackal.gossip.tcp;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import junit.framework.TestCase;

import com.hellblazer.jackal.gossip.Digest;
import com.hellblazer.jackal.gossip.Gossip;
import com.hellblazer.jackal.gossip.GossipMessages;
import com.hellblazer.jackal.gossip.HeartbeatState;
import com.hellblazer.jackal.gossip.tcp.GossipHandler;
import com.hellblazer.jackal.nio.AbstractCommunicationsHandler;
import com.hellblazer.jackal.nio.ChannelHandler;

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
        ChannelHandler handler = mock(ChannelHandler.class);

        Digest digest1 = new Digest(new InetSocketAddress("google.com", 0), 0);
        Digest digest2 = new Digest(new InetSocketAddress("google.com", 1), 1);
        Digest digest3 = new Digest(new InetSocketAddress("google.com", 2), 0);
        Digest digest4 = new Digest(new InetSocketAddress("google.com", 3), 3);
        byte[] bytes = new byte[4 * GossipMessages.DIGEST_BYTE_SIZE + 1 + 4];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        msg.put(GossipMessages.GOSSIP);
        msg.putInt(4);
        digest1.writeTo(msg);
        digest2.writeTo(msg);
        digest3.writeTo(msg);
        digest4.writeTo(msg);

        GossipHandler gHandler = new GossipHandler(gossip, handler, channel);

        gHandler.deliver(bytes);

        verify(gossip).gossip(Arrays.asList(digest1, digest2, digest3, digest4),
                              gHandler);
    }

    public void testDeliverReply() throws Exception {
        Gossip gossip = mock(Gossip.class);
        SocketChannel channel = mock(SocketChannel.class);
        ChannelHandler handler = mock(ChannelHandler.class);

        Digest digest1 = new Digest(new InetSocketAddress("google.com", 0), 0);
        Digest digest2 = new Digest(new InetSocketAddress("google.com", 1), 1);
        Digest digest3 = new Digest(new InetSocketAddress("google.com", 2), 0);
        Digest digest4 = new Digest(new InetSocketAddress("google.com", 3), 3);
        HeartbeatState state1 = new HeartbeatState(new InetSocketAddress(0));
        HeartbeatState state2 = new HeartbeatState(new InetSocketAddress(1));
        HeartbeatState state3 = new HeartbeatState(new InetSocketAddress(2));
        HeartbeatState state4 = new HeartbeatState(new InetSocketAddress(3));

        byte[] bytes = new byte[1 + 4 * GossipMessages.DIGEST_BYTE_SIZE + 4
                                * GossipMessages.HEARTBEAT_STATE_BYTE_SIZE + 4];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        msg.put(GossipMessages.REPLY);
        msg.putInt(4);
        msg.putInt(4);
        digest1.writeTo(msg);
        digest2.writeTo(msg);
        digest3.writeTo(msg);
        digest4.writeTo(msg);
        state1.writeTo(msg);
        state2.writeTo(msg);
        state3.writeTo(msg);
        state4.writeTo(msg);

        GossipHandler gHandler = new GossipHandler(gossip, handler, channel);

        gHandler.deliver(bytes);

        verify(gossip).reply(Arrays.asList(digest1, digest2, digest3, digest4),
                             Arrays.asList(state1, state2, state3, state4),
                             gHandler);
    }

    public void testDeliverUpdate() throws Exception {
        Gossip gossip = mock(Gossip.class);
        SocketChannel channel = mock(SocketChannel.class);
        ChannelHandler handler = mock(ChannelHandler.class);
        HeartbeatState state1 = new HeartbeatState(new InetSocketAddress(0));
        HeartbeatState state2 = new HeartbeatState(new InetSocketAddress(1));
        HeartbeatState state3 = new HeartbeatState(new InetSocketAddress(2));
        HeartbeatState state4 = new HeartbeatState(new InetSocketAddress(3));

        byte[] bytes = new byte[1 + 4 * GossipMessages.HEARTBEAT_STATE_BYTE_SIZE + 4];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        msg.put(GossipMessages.UPDATE);
        msg.putInt(4);
        state1.writeTo(msg);
        state2.writeTo(msg);
        state3.writeTo(msg);
        state4.writeTo(msg);

        GossipHandler gHandler = new GossipHandler(gossip, handler, channel);

        gHandler.deliver(bytes);

        verify(gossip).update(Arrays.asList(state1, state2, state3, state4));
    }

    public void testGossip() throws Exception {
        Gossip gossip = mock(Gossip.class);
        SocketChannel channel = mock(SocketChannel.class);
        ChannelHandler handler = mock(ChannelHandler.class);

        Digest digest1 = new Digest(new InetSocketAddress("google.com", 0), 0);
        Digest digest2 = new Digest(new InetSocketAddress("google.com", 1), 1);
        Digest digest3 = new Digest(new InetSocketAddress("google.com", 2), 0);
        Digest digest4 = new Digest(new InetSocketAddress("google.com", 3), 3);
        byte[] bytes = new byte[4 * GossipMessages.DIGEST_BYTE_SIZE + 1 + 4];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        msg.put(GossipMessages.GOSSIP);
        msg.putInt(4);
        digest1.writeTo(msg);
        digest2.writeTo(msg);
        digest3.writeTo(msg);
        digest4.writeTo(msg);

        GossipHandler gHandler = new GossipHandler(gossip, handler, channel);

        gHandler.gossip(Arrays.asList(digest1, digest2, digest3, digest4));

        Field msgBytes = AbstractCommunicationsHandler.class.getDeclaredField("msgOut");
        msgBytes.setAccessible(true);
        ByteBuffer msgOut = (ByteBuffer) msgBytes.get(gHandler);
        int i = 0;
        for (byte b : msgOut.array()) {
            assertEquals("index " + i + " !=", bytes[i++], b);
        }
    }

    public void testReply() throws Exception {
        Gossip gossip = mock(Gossip.class);
        SocketChannel channel = mock(SocketChannel.class);
        ChannelHandler handler = mock(ChannelHandler.class);

        Digest digest1 = new Digest(new InetSocketAddress("google.com", 0), 0);
        Digest digest2 = new Digest(new InetSocketAddress("google.com", 1), 1);
        Digest digest3 = new Digest(new InetSocketAddress("google.com", 2), 0);
        Digest digest4 = new Digest(new InetSocketAddress("google.com", 3), 3);
        HeartbeatState state1 = new HeartbeatState(new InetSocketAddress(0));
        HeartbeatState state2 = new HeartbeatState(new InetSocketAddress(1));
        HeartbeatState state3 = new HeartbeatState(new InetSocketAddress(2));
        HeartbeatState state4 = new HeartbeatState(new InetSocketAddress(3));

        byte[] bytes = new byte[1 + 4 * GossipMessages.DIGEST_BYTE_SIZE + 4
                                * GossipMessages.HEARTBEAT_STATE_BYTE_SIZE + 4
                                + 4];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        msg.put(GossipMessages.REPLY);
        msg.putInt(4);
        msg.putInt(4);
        digest1.writeTo(msg);
        digest2.writeTo(msg);
        digest3.writeTo(msg);
        digest4.writeTo(msg);
        state1.writeTo(msg);
        state2.writeTo(msg);
        state3.writeTo(msg);
        state4.writeTo(msg);

        GossipHandler gHandler = new GossipHandler(gossip, handler, channel);

        gHandler.reply(Arrays.asList(digest1, digest2, digest3, digest4),
                       Arrays.asList(state1, state2, state3, state4));

        Field msgBytes = AbstractCommunicationsHandler.class.getDeclaredField("msgOut");
        msgBytes.setAccessible(true);
        ByteBuffer msgOut = (ByteBuffer) msgBytes.get(gHandler);
        int i = 0;
        byte[] outBytes = msgOut.array();
        for (byte b : outBytes) {
            assertEquals("index " + i + " !=", bytes[i++], b);
        }
    }

    public void testUpdate() throws Exception {
        Gossip gossip = mock(Gossip.class);
        SocketChannel channel = mock(SocketChannel.class);
        ChannelHandler handler = mock(ChannelHandler.class);
        HeartbeatState state1 = new HeartbeatState(new InetSocketAddress(0));
        HeartbeatState state2 = new HeartbeatState(new InetSocketAddress(1));
        HeartbeatState state3 = new HeartbeatState(new InetSocketAddress(2));
        HeartbeatState state4 = new HeartbeatState(new InetSocketAddress(3));

        byte[] bytes = new byte[1 + 4 * GossipMessages.HEARTBEAT_STATE_BYTE_SIZE + 4];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        msg.put(GossipMessages.UPDATE);
        msg.putInt(4);
        state1.writeTo(msg);
        state2.writeTo(msg);
        state3.writeTo(msg);
        state4.writeTo(msg);

        GossipHandler gHandler = new GossipHandler(gossip, handler, channel);

        gHandler.update(Arrays.asList(state1, state2, state3, state4));

        Field msgBytes = AbstractCommunicationsHandler.class.getDeclaredField("msgOut");
        msgBytes.setAccessible(true);
        ByteBuffer msgOut = (ByteBuffer) msgBytes.get(gHandler);
        int i = 0;
        byte[] outBytes = msgOut.array();
        for (byte b : outBytes) {
            assertEquals("index " + i + " !=", bytes[i++], b);
        }
    }
}
