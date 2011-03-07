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

/**
 * The service interface for connecting to new members
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 */

public interface GossipCommunications {

    /**
     * Asynchronously create a new connection to the indicated address. When the
     * connection is established, run the connect action. Note that this is an
     * asynchronous operation, and the handler will not be ready for
     * communications unless and until the connectAction is run.
     * 
     * @param address
     *            - the endpoint to create a connection to
     * @param connectAction
     *            - the action to run when the new connection is fully
     *            established.
     * @return the GossipHandler for this outbound connectioin
     * @throws IOException
     *             - if there is a problem creating a connection to the address
     */
    GossipHandler connect(InetSocketAddress address, Runnable connectAction)
                                                                            throws IOException;

    /**
     * Notification that heartbeat state for a member has changed or is
     * discovered for the first time
     * 
     * @param state
     */
    void notifyUpdate(final HeartbeatState state);
}
