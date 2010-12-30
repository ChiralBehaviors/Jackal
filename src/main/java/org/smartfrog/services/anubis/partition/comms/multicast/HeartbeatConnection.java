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
package org.smartfrog.services.anubis.partition.comms.multicast;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.Connection;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocol;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolAdapter;
import org.smartfrog.services.anubis.partition.protocols.leader.Candidate;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

public class HeartbeatConnection extends HeartbeatProtocolAdapter implements
        Connection, HeartbeatProtocol {

    private static Logger log = Logger.getLogger(HeartbeatConnection.class.getCanonicalName());
    /**
     * Others for this implementation
     */
    private ConnectionSet connectionSet = null;

    private Identity me = null;
    /**
     * Connection - includes Sender
     */
    private boolean terminated = false;

    /**
     * Constructor - creates and instance of a HeartbeatConnection using an
     * existing heartbeat protocol and candidate.
     * 
     * @param id
     *            - local id
     * @param cs
     *            - connection set
     * @param hbp
     *            - existing heartbeat protocol
     * @param can
     *            - existing candidate
     */
    public HeartbeatConnection(Identity id, ConnectionSet cs,
                               HeartbeatProtocol hbp, Candidate can) {
        super(hbp, can);
        me = id;
        connectionSet = cs;
    }

    /**
     * HeartbeatProtocol interface 1) extend receiveHeartbeat call by
     * over-riding it, and calling the super.receiveHeartbeat(). Adds functions
     * specific to the HeartbeatConnection - i.e. checking to convert to a
     * MessagingConnection.
     */
    @Override
    public boolean receiveHeartbeat(Heartbeat hb) {
        /**
         * ignore if the epoch is wrong or if this connection has terminated
         */
        if (!getSender().equalEpoch(hb.getSender()) || terminated) {
            if (!terminated && log.isLoggable(Level.FINEST)) {
                log.finest("Ignoring heart beat from wrong epoch: " + hb);
            }
            return false;
        }

        if (super.receiveHeartbeat(hb)) {
            /**
             * Extract piggy-backed messaging information to see if this
             * connection should be converted to a messaging connection
             */
            if (hb.getMsgLinks().contains(me.id)) {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("converting heart beat connection to message connection");
                }
                connectionSet.convertToMessageConnection(this);
            } else {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("not converting heart beat connection to message connection as connection does not contain my identity: "
                               + me.id);
                }
            }
            return true;
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("heartbeat rejected");
        }
        return false;
    }

    /**
     * Connection interface
     */
    @Override
    public void terminate() {
        super.terminate();
        terminated = true;
    }

    @Override
    public String toString() {
        return "HeartbeatConnection [from: " + me + " to:" + getId() + "]";
    }

}
