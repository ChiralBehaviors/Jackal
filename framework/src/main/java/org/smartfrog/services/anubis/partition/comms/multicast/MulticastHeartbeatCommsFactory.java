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

import java.io.IOException;
import java.net.InetAddress;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionManager;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

public class MulticastHeartbeatCommsFactory implements HeartbeatCommsFactory {

    private final WireSecurity     wireSecurity;
    private final MulticastAddress address;
    private final InetAddress      inf;
    private final String           threadName;
    private final Identity         id;

    public MulticastHeartbeatCommsFactory(WireSecurity wireSecurity,
                                          MulticastAddress address,
                                          InetAddress inf, Identity id) {
        this.wireSecurity = wireSecurity;
        this.address = address;
        this.inf = inf;
        threadName = "Heartbeat Comms (node " + id.id + ")";
        this.id = id;
    }

    /* (non-Javadoc)
     * @see org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory#create(org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver)
     */
    @Override
    public HeartbeatCommsIntf create(ConnectionManager cs) throws IOException {
        return new HeartbeatComms(address, inf, cs, threadName, id,
                                  wireSecurity);
    }
}
