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
package com.hellblazer.jackal.partition.comms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

/**
 * 
 * @author hhildebrand
 * 
 */
public class ConnectionInitiator {

    private static final Logger     log = LoggerFactory.getLogger(ConnectionInitiator.class);

    private final MessageConnection connection;
    private final HeartbeatMsg      heartbeat;

    public ConnectionInitiator(MessageConnection con, HeartbeatMsg hb) {
        connection = con;
        heartbeat = hb;
    }

    public void handshake(final MessageHandler impl) {
        impl.sendInitial(heartbeat);
        /**
         * If the implementation is successfully assigned then start its thread
         * - otherwise call terminate() to shutdown the connection. The impl
         * will not be accepted if the heartbeat protocol has terminated the
         * connection during the time it took to establish it.
         */
        if (!connection.assignImpl(impl)) {
            log.info(String.format("Impl already assigned for outbound connection: %s",
                                   connection));
            impl.terminate();
        }
        impl.handshakeComplete();
    }
}