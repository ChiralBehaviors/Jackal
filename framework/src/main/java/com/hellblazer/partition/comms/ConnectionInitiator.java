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
package com.hellblazer.partition.comms;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.IOConnection;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

/**
 * 
 * @author hhildebrand
 * 
 */
public class ConnectionInitiator {

    private static final Logger     log = Logger.getLogger(ConnectionInitiator.class.getCanonicalName());

    private final MessageConnection connection;
    private final HeartbeatMsg      heartbeat;
    private final WireSecurity      wireSecurity;

    public ConnectionInitiator(MessageConnection con, HeartbeatMsg hb,
                               WireSecurity sec) {
        connection = con;
        heartbeat = hb;
        wireSecurity = sec;
    }

    public void handshake(final MessageHandler impl) {
        try {
            heartbeat.setOrder(IOConnection.INITIAL_MSG_ORDER);
            impl.sendObject(wireSecurity.toWireForm(heartbeat));
        } catch (WireFormException e) {
            log.log(Level.SEVERE, "failed to marshall timed message: "
                                  + heartbeat, e);
            impl.terminate();
            return;
        }
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