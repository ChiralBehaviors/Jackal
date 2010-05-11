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
package org.smartfrog.services.anubis.basiccomms.multicasttransport;



import java.net.InetAddress;


/**
 * MulticastAddress is a representation of a multicast address used for
 * convienience. The representation includes the multicast ip address
 * and port and a time to live.
 */
public class MulticastAddress {

    /**
     * the multicast ip address
     */
    public   InetAddress   ipaddress;

    /**
     * the multicast address port
     */
    public   int           port;

    /**
     * time to live to use when talking to this address
     */
    public   int           timeToLive;


    /**
     * constructing the multicast address from a given set of parameters.
     */
    public MulticastAddress(InetAddress address, int port, int timeToLive) {
        this.ipaddress    = address;
        this.port       = port;
        this.timeToLive = timeToLive;
    }

}

