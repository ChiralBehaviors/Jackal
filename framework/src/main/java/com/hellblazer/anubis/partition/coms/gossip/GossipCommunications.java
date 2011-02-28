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
import java.util.List;
import java.util.Map;

import com.hellblazer.anubis.util.Pair;

/**
 * The communications interface used by the gossip protocol
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public interface GossipCommunications {

    /**
     * The first message (SYN) of the gossip protocol. Send a list of the
     * shuffled digests of the receiver's view of the endpoint state
     * 
     * @param digests
     * @param to
     */
    void send(List<Digest> digests, InetSocketAddress to);

    /**
     * Send the required delta state to the gossip member. This is the 2nd
     * message (ACK) in the gossip protocol
     * 
     * @param deltaState
     * @param to
     */
    void send(Map<InetSocketAddress, Endpoint> deltaState, InetSocketAddress to);

    /**
     * The 3rd message (ACK2) in the gossip protocol
     * 
     * @param ack
     * @param to
     */
    void send(Pair<List<Digest>, Map<InetSocketAddress, Endpoint>> ack,
              InetSocketAddress to);

}