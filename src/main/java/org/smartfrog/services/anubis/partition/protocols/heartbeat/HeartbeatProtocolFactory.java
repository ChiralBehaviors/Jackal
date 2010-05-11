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
package org.smartfrog.services.anubis.partition.protocols.heartbeat;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.ViewListener;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

/**
 * <p>Description: The HeartbeatProtocolFactory creates a heartbeat protocol
 *                 implementation. A unique instance is created for each
 *                 connection. This class can be specialised to change the
 *                 implementation of the heartbeat protocol that gets created.
 *                 The protocol is created by the HeartbeatComms class
 *                 when a new heartbeat arrives that is from a sender that is
 *                 unknown.</p>
 */

public interface HeartbeatProtocolFactory  {
    public HeartbeatProtocol createProtocol(Heartbeat hb, ViewListener vl, HeartbeatMsg sharedHeartbeat);
    public HeartbeatMsg createMsg(Identity identity, ConnectionAddress address);
}
