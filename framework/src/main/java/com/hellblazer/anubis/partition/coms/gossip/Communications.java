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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import com.hellblazer.anubis.basiccomms.nio.CommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;
import com.hellblazer.anubis.basiccomms.nio.SocketOptions;

/**
 * The communciations implementation for the gossip protocol
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 */

public class Communications extends ServerChannelHandler implements
        GossipCommunications {
    private Gossip gossip;

    public Communications(String handlerName,
                          InetSocketAddress endpointAddress,
                          SocketOptions socketOptions,
                          ExecutorService commsExec,
                          ExecutorService dispatchExec) throws IOException {
        super(handlerName, endpointAddress, socketOptions, commsExec,
              dispatchExec);
    }

    @Override
    public GossipHandler connect(InetSocketAddress address, Endpoint endpoint,
                                 Runnable connectAction) throws IOException {
        SocketChannel channel = SocketChannel.open(address);
        channel.configureBlocking(false);
        GossipHandler handler = new GossipHandler(gossip, this, channel);
        addHandler(handler);
        endpoint.setCommunications(handler);
        if (channel.finishConnect()) {
            dispatch(connectAction);
        } else {
            selectForConnect(handler, connectAction);
        }
        return handler;
    }

    public void setGossip(Gossip gossip) {
        this.gossip = gossip;
    }

    @Override
    protected CommunicationsHandler createHandler(SocketChannel accepted) {
        return new GossipHandler(gossip, this, accepted);
    }
}