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

import static java.lang.String.format;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hellblazer.anubis.basiccomms.nio.AbstractCommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;
import com.hellblazer.anubis.util.Pair;

/**
 * The communications handler imlementing the gossip wire protocol
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class GossipHandler extends AbstractCommunicationsHandler implements
        GossipCommunications {
    private static final byte REPLY = 1;
    private static final byte UPDATE = 2;
    private static final byte GOSSIP = 0;
    private static final Logger log = Logger.getLogger(GossipHandler.class.getCanonicalName());

    private final Gossip gossip;

    public GossipHandler(Gossip gossip, ServerChannelHandler handler,
                         SocketChannel channel) {
        super(handler, channel);
        this.gossip = gossip;
    }

    public InetSocketAddress getAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void gossip(List<Digest> digests) {
        // TODO Auto-generated method stub

    }

    @Override
    public void reply(Pair<List<Digest>, List<HeartbeatState>> ack) {
        // TODO Auto-generated method stub

    }

    @Override
    public void update(List<HeartbeatState> deltaState) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void closing() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void deliver(byte[] msg) {
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        buffer.position(HEADER_SIZE);
        byte msgType = buffer.get();
        switch (msgType) {
            case GOSSIP: {
                handleGossip(buffer);
                break;
            }
            case REPLY: {
                handleReply(buffer);
                break;
            }
            case UPDATE: {
                handleUpdate(buffer);
                break;
            }
            default: {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest(format("invalid message type: %s from: %s",
                                      msgType, this));
                }
            }
        }
    }

    private void handleGossip(ByteBuffer msg) {
        Digest[] digests = new Digest[msg.getInt()];
        if (log.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (Digest gDigest : digests) {
                sb.append(gDigest);
                sb.append(" ");
            }
            log.finest(format("Gossip digests from %s are : %s", this,
                              sb.toString()));
        }
        Pair<List<Digest>, List<HeartbeatState>> ack = gossip.gossip(digests);
        if (ack != null) {
            reply(ack);
        }
    }

    private void handleReply(ByteBuffer msg) {
        Digest[] digests = new Digest[msg.getInt()];
        HeartbeatState[] remoteStates = new HeartbeatState[msg.getInt()];
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received reply from %s", this));
        }
        List<HeartbeatState> deltaState = gossip.reply(digests, remoteStates);
        if (deltaState != null) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest(format("Sending an update to %s", this));
            }
            update(deltaState);
        }
    }

    private void handleUpdate(ByteBuffer msg) {
        HeartbeatState[] remoteStates = new HeartbeatState[msg.getInt()];
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Received a GossipDigestAck2Message from %s",
                              this));
        }
        gossip.update(remoteStates);
    }

}
