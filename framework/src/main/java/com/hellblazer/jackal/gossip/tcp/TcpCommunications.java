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
package com.hellblazer.jackal.gossip.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import com.hellblazer.jackal.gossip.Endpoint;
import com.hellblazer.jackal.gossip.Gossip;
import com.hellblazer.jackal.gossip.GossipCommunications;
import com.hellblazer.jackal.nio.CommunicationsHandler;
import com.hellblazer.jackal.nio.ServerSocketChannelHandler;
import com.hellblazer.jackal.nio.SocketOptions;

/**
 * The communciations implementation for the gossip protocol
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 */

public class TcpCommunications extends ServerSocketChannelHandler implements
        GossipCommunications {
    private Gossip gossip;

    public TcpCommunications(String handlerName, SelectableChannel channel,
                      InetSocketAddress endpointAddress,
                      SocketOptions socketOptions, ExecutorService commsExec,
                      ExecutorService dispatchExec) throws IOException {
        super(handlerName, channel, endpointAddress, socketOptions, commsExec,
              dispatchExec);
    }

    @Override
    public void connect(InetSocketAddress address, Endpoint endpoint,
                        Runnable connectAction) throws IOException {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        GossipHandler handler = new GossipHandler(gossip, this, channel);
        addHandler(handler);
        endpoint.setCommunications(handler);
        channel.connect(address);
        if (channel.finishConnect()) {
            dispatch(connectAction);
        } else {
            selectForConnect(handler, connectAction);
        }
    }

    public void setGossip(Gossip gossip) {
        this.gossip = gossip;
    }

    @Override
    protected CommunicationsHandler createHandler(SocketChannel accepted) {
        return new GossipHandler(gossip, this, accepted);
    }
}