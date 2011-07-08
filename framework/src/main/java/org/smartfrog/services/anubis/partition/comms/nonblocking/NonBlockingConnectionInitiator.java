/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

 */
package org.smartfrog.services.anubis.partition.comms.nonblocking;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.IOConnection;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

public class NonBlockingConnectionInitiator {

    private MessageConnection   connection   = null;
    private HeartbeatMsg        heartbeat    = null;
    private static final Logger log          = Logger.getLogger(NonBlockingConnectionInitiator.class.getCanonicalName());
    private WireSecurity        wireSecurity = null;

    public NonBlockingConnectionInitiator(MessageConnection con,
                                          HeartbeatMsg hb, WireSecurity sec) {
        connection = con;
        heartbeat = hb;
        wireSecurity = sec;
    }

    public void finishNioConnect(MessageNioHandler impl) {

        if (impl.isReadyForWriting() && impl.connected()) {
            try {
                heartbeat.setOrder(IOConnection.INITIAL_MSG_ORDER);
                impl.send(wireSecurity.toWireForm(heartbeat));
            } catch (WireFormException e) {
                log.log(Level.SEVERE, "MCI: failed to marshall timed message: "
                                      + heartbeat, e);
                return;
            }
        } else {
            log.severe("MCI: can't send first heartbeat!!!");
        }

        /**
         * If the implementation is successfully assigned then start its thread
         * - otherwise call terminate() to shutdown the connection. The impl
         * will not be accepted if the heartbeat protocol has terminated the
         * connection during the time it took to establish it.
         */
        if (connection.assignImpl(impl)) {
            impl.start();
        } else {
            log.info(String.format("Impl already assigned for outbound connection: %s",
                                   connection));
            impl.terminate();
        }
    }

}
