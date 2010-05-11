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
package org.smartfrog.services.anubis.partition.test.mainconsole;



import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastComms;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.Wire;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;


public class Snoop extends MulticastComms {

    private Controller controller;
    private Identity   id;

    public Snoop(String threadName, MulticastAddress address, Identity id, Controller controller) throws Exception {
        super(threadName, address);
        this.controller = controller;
        this.id         = id;
    }

    protected void deliverBytes(byte[] bytes) {

        Object obj = null;
        try {
            obj = Wire.fromWire(bytes);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if( obj instanceof HeartbeatMsg ) {
            HeartbeatMsg msg = (HeartbeatMsg)obj;
            if( id.equalMagic(msg.getSender()) )
                controller.deliverHeartbeat((HeartbeatMsg)obj);
        }
    }

}
