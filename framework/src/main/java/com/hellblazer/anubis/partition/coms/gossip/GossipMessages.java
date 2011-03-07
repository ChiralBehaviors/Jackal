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

import java.util.List;

import com.hellblazer.anubis.util.Pair;

/**
 * The communications interface used by the gossip protocol
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public interface GossipMessages {
    int INET_ADDRESS_V6_BYTE_SIZE  = 16;
    int INET_ADDRESS_MAX_BYTE_SIZE = INET_ADDRESS_V6_BYTE_SIZE // address
                                   + 1 // addressLength
                                   + 2;  // port
    int NODE_ID_SET_MAX_BYTE_SIZE  = 257;
    int IDENTITY_BYTE_SIZE         = 16;
    int HEARTBEAT_STATE_BYTE_SIZE  = IDENTITY_BYTE_SIZE // candidate
                                     + NODE_ID_SET_MAX_BYTE_SIZE // msgLinks
                                     + 1 // preferred
                                     + IDENTITY_BYTE_SIZE // sender
                                     + INET_ADDRESS_MAX_BYTE_SIZE // senderAddress
                                     + 1 // stable
                                     + INET_ADDRESS_MAX_BYTE_SIZE // testInterface
                                     + NODE_ID_SET_MAX_BYTE_SIZE // view
                                     + 4 // viewNumber
                                     + 4; // viewTimeStamp 
    int DIGEST_BYTE_SIZE           = INET_ADDRESS_MAX_BYTE_SIZE // address
                                   + 4 // epoch
                                   + 4;  // viewNumber

    /**
     * Close the communications connection
     */
    void close();

    /**
     * The first message of the gossip protocol. Send a list of the shuffled
     * digests of the receiver's view of the endpoint state
     * 
     * @param digests
     *            - the list of heartbeat digests the receiver knows about
     */
    void gossip(List<Digest> digests);

    /**
     * The second message in the gossip protocol. Send a list of digests the
     * node this handler represents, that would like heartbeat state updates
     * for, along with the list of heartbeat state this node believes is out of
     * date on the node this handler represents.
     * 
     * @param reply
     *            - the pair of state updates and requested state
     */
    void reply(Pair<List<Digest>, List<HeartbeatState>> reply);

    /**
     * The third message of the gossip protocol. Send a list of updated
     * heartbeat states to the node this handler represents, which is requesting
     * the updates.
     * 
     * @param deltaState
     *            - the list of heartbeat states requested.
     */
    void update(List<HeartbeatState> deltaState);

}