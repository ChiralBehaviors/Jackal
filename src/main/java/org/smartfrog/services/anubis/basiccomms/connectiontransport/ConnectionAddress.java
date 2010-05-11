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
package org.smartfrog.services.anubis.basiccomms.connectiontransport;





import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.wire.WireSizes;



/**
 * ConnectionAddress is a class used to represent an endpoint for a
 * tcp connection. Its for convienience.
 */
public class ConnectionAddress implements Cloneable, WireSizes {

    public InetAddress   ipaddress;
    public int           port;

    static final private int nullAddress = 0;
    static final private int lengthIdx   = 0;
    static final private int addressIdx  = intSz;
    static final private int portIdx     = addressIdx + maxInetAddressSz;
    static final public int connectionAddressWireSz = portIdx + intSz;

    /**
     * constructing from given parameters
     */
    public ConnectionAddress(InetAddress address, int port) {

        this.ipaddress  = address;
        this.port       = port;
    }


    public static ConnectionAddress readWireForm(ByteBuffer bytes, int idx) {

        int length = bytes.getInt(idx + lengthIdx);
        if( length == nullAddress )
            return null;

        byte[] address = new byte[length];
        for(int i = 0; i < address.length; i++)
            address[i] = bytes.get(idx + addressIdx + i);
        InetAddress inetAddress = null;
        try {
            inetAddress = InetAddress.getByAddress(address);
        } catch (UnknownHostException ex) {
            return null;
        }

        int port = bytes.getInt(idx + portIdx);
        return new ConnectionAddress(inetAddress, port);
    }

    public static void writeNullWireForm(ByteBuffer bytes, int idx) {
        bytes.putInt(idx, nullAddress);
    }

    public void writeWireForm(ByteBuffer bytes, int idx) {
        byte[] address = ipaddress.getAddress();
        bytes.putInt(idx + lengthIdx, address.length);
        for(int i = 0; i < address.length; i++)
            bytes.put(idx + addressIdx + i, address[i]);
        bytes.putInt(idx + portIdx, port);
    }



    /**
     * toString for debug purposes
     */
    public String toString() {
        return ipaddress.toString() + ":" + port;
    }



    /**
     * test to see if this is a null address
     */
    public boolean isNullAddress() {
        return ipaddress == null;
    }


    public Object clone() {
        try {
            ConnectionAddress addr = (ConnectionAddress)super.clone();
            return addr;
        }
        catch (CloneNotSupportedException ex) {
            return null;
        }
    }
}
