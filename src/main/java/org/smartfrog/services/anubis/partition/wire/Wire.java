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
package org.smartfrog.services.anubis.partition.wire;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.wire.msg.CloseMsg;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.msg.MessageMsg;
import org.smartfrog.services.anubis.partition.wire.msg.TimedMsg;
import org.smartfrog.services.anubis.partition.wire.msg.untimed.SerializedMsg;
import org.smartfrog.services.anubis.partition.wire.msg.*;

/**
 * Static class with wire form message utilities
 *
 * @author not attributable
 * @version 1.0
 */
public class Wire {

    /**
     * fromWire(ByteBuffer) constructs a message from its wire form
     *
     * @param wireForm ByteBuffer
     * @return Object
     * @throws IOException
     * @throws WireFormException
     * @throws ClassNotFoundException
     */
    static public WireMsg fromWire(ByteBuffer wireForm) throws IOException, WireFormException, ClassNotFoundException {

        int type = getWireType(wireForm);

        switch( type ) {
            case HeartbeatMsg.HEARTBEAT_MSG_WIRE_TYPE:
                return new HeartbeatMsg(wireForm);

            case PingHeartbeatMsg.PING_HEARTBEAT_MSG_WIRE_TYPE:
                return new PingHeartbeatMsg(wireForm);

            case MessageMsg.MESSAGE_MSG_WIRE_TYPE:
                return new MessageMsg(wireForm);

            case CloseMsg.CLOSE_MSG_WIRE_TYPE:
                return new CloseMsg(wireForm);



            case SerializedMsg.SERIALIZED_MSG_WIRE_TYPE:
                return new SerializedMsg(wireForm);

            case TimedMsg.TIMED_MSG_WIRE_TYPE:
                return new TimedMsg(wireForm);

            case WireMsg.WIRE_TYPE:
                return new WireMsg(wireForm);

            default:
                throw new WireFormException("Unknown message type (" + type + ")");
        }
    }

    /**
     * fromWire(byte[]) constructs a message from its wire form
     *
     * @param wire byte[]
     * @return Object
     * @throws IOException
     * @throws WireFormException
     * @throws ClassNotFoundException
     */
    static public WireMsg fromWire(byte[] wire) throws IOException, WireFormException, ClassNotFoundException {

        if( wire == null )
            throw new WireFormException("wire is a null pointer");

        return fromWire( ByteBuffer.wrap(wire) );
    }

    /**
     * Gets the type of a message from its wire form
     *
     * @param wire byte[]
     * @return int
     */
    public static int getWireType(byte[] wire) {
        return ByteBuffer.wrap(wire).getInt(0);
    }

    /**
     * Gets the type of a message from its wire form
     *
     * @param wireForm ByteBuffer
     * @return int
     */
    public static int getWireType(ByteBuffer wireForm) {
        return wireForm.getInt(0);
    }
}
