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
package com.hellblazer.jackal.gossip;

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
     * connection is established, run the connect action.
     * <p>
     * Note that this is an asynchronous operation, and the handler will not be
     * ready for communications unless and until the connectAction is run.
     * 
     * @param address
     *            - the address to create a connection to
     * @param endpoint
     *            - the endpoint to connect
     * @param connectAction
     *            - the action to run when the new connection is fully
     *            established.
     * @throws IOException
     *             - if there is a problem creating a connection to the address
     */
    void connect(InetSocketAddress address, Endpoint endpoint,
                 Runnable connectAction) throws IOException;

    /**
     * Answer the local address of the communcations endpoint
     * 
     * @return the socket address
     */
    InetSocketAddress getLocalAddress();

    /**
     * Set the gossip service
     * 
     * @param gossip
     */
    void setGossip(Gossip gossip);

    /**
     * Start the communications service
     */
    void start();

    /**
     * Tereminate the communications service
     */
    void terminate();
}
