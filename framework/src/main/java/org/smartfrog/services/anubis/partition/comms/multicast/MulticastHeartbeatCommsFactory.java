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
import java.net.InetSocketAddress;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

public class MulticastHeartbeatCommsFactory implements HeartbeatCommsFactory {

    private WireSecurity wireSecurity = null;

    public MulticastHeartbeatCommsFactory() {
        super();
    }

    /* (non-Javadoc)
     * @see org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory#create(org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress, org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress, org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver, java.lang.String, org.smartfrog.services.anubis.partition.util.Identity)
     */
    @Override
    public HeartbeatCommsIntf create(MulticastAddress address,
                                     InetSocketAddress inf,
                                     HeartbeatReceiver cs, String threadName,
                                     Identity id) throws IOException {
        return new HeartbeatComms(address, inf, cs, threadName, id,
                                  wireSecurity);
    }

    /* (non-Javadoc)
     * @see org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory#create(org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress, org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver, java.lang.String, org.smartfrog.services.anubis.partition.util.Identity)
     */
    @Override
    public HeartbeatCommsIntf create(MulticastAddress address,
                                     HeartbeatReceiver cs, String threadName,
                                     Identity id) throws IOException {
        return new HeartbeatComms(address, cs, threadName, id, wireSecurity);
    }

    public WireSecurity getWireSecurity() {
        return wireSecurity;
    }

    public void setWireSecurity(WireSecurity wireSecurity) {
        this.wireSecurity = wireSecurity;
    }
}
