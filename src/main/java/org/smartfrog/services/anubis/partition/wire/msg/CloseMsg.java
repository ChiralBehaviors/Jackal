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
package org.smartfrog.services.anubis.partition.wire.msg;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.wire.WireFormException;

public class CloseMsg extends HeartbeatMsg implements Close {

    public static final int CLOSE_MSG_WIRE_TYPE = 301;
    public static final int CLOSE_MSG_WIRE_SIZE = HEARTBEAT_MSG_WIRE_SIZE;

    protected int getType() { return CLOSE_MSG_WIRE_TYPE; }
    public int getSize() { return CLOSE_MSG_WIRE_SIZE; }

    /**
     * Construct a close message that matches the heartbeat messsage
     *
     * @param hb HeartbeatMsg
     *
     */
   public CloseMsg(HeartbeatMsg hb) {
       super(hb);
   }

   /**
    * Constructor - used internally when reading from wire
    */
   protected CloseMsg() {
       super();
   }


   public CloseMsg(ByteBuffer wireForm) throws ClassNotFoundException, WireFormException, IOException {
       super();
       readWireForm(wireForm);
   }

}
