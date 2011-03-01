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

import com.hellblazer.anubis.util.Pair;

/**
 * The communications interface used by the gossip protocol
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public interface GossipCommunications {

    /**
     * The first message of the gossip protocol. Send a list of the shuffled
     * digests of the receiver's view of the endpoint state
     * 
     * @param digests
     *            - the list of heartbeat digests the receiver knows about
     * @param to
     *            - the partner we're gossiping with
     */
    void gossip(List<Digest> digests, InetSocketAddress to);

    /**
     * The third message of the gossip protocol. Send a list of updated
     * heartbeat states to the node requesting the updates.
     * 
     * @param deltaState
     *            - the list of heartbeat states requested.
     * @param to
     *            - the requesting node
     */
    void reply(List<HeartbeatState> deltaState, InetSocketAddress to);

    /**
     * The second message in the gossip protocol. Send a list of digests the
     * receiver would like heartbeat state updates for, along with the list of
     * heartbeat state the receiver believes is out of date on the gossiping
     * node.
     * 
     * @param ack
     *            - the pair of state updates and requested state
     * @param to
     *            - the gossiping node to reply to
     */
    void reply(Pair<List<Digest>, List<HeartbeatState>> ack,
               InetSocketAddress to);

}