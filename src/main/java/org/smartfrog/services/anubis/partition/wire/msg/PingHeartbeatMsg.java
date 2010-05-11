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

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.WireFormException;

public class PingHeartbeatMsg extends HeartbeatMsg {

    private NodeIdSet pings;

    static final private int pingBitIdx = HEARTBEAT_MSG_WIRE_SIZE;
    static final private int pingBitSz = MAX_BIT_SIZE + intSz;

    public static final int PING_HEARTBEAT_MSG_WIRE_TYPE = 310;
    public static final int PING_HEARTBEAT_MSG_WIRE_SIZE =
        HEARTBEAT_MSG_WIRE_SIZE + pingBitSz;

    protected int getType() {
        return PING_HEARTBEAT_MSG_WIRE_TYPE;
    }

    public int getSize() {
        return PING_HEARTBEAT_MSG_WIRE_SIZE;
    }

    /**
     * Construct a ping heartbeat message
     *
     * @param identity
     * @param address
     */
    public PingHeartbeatMsg(Identity identity, ConnectionAddress address) {
        super(identity, address);
        pings = new NodeIdSet();
    }

    /**
     * Constructor - used internally when reading from wire
     */
    protected PingHeartbeatMsg() {
        super();
    }

    public PingHeartbeatMsg(PingHeartbeatMsg pinghb) {
        super(pinghb);
        pings = pinghb.pings;
    }

    public PingHeartbeatMsg(ByteBuffer wireForm) throws ClassNotFoundException,
        WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }

    /**
     * generate close message from this heartbeat
     */
    public HeartbeatMsg toClose() {
        return new PingCloseMsg(this);
    }

    /**
     * pings accessors
     * @param id Identity
     */
    public synchronized void setPingBit(Identity id) {
        pings.add(id.id);
    }

    public synchronized void clearPingBit(Identity id) {
        pings.remove(id.id);
    }

    public synchronized void flipPingBit(Identity id) {
        pings.flip(id.id);
    }

    public synchronized boolean getPingBit(Identity id) {
        return pings.contains(id.id);
    }

    /**
     * Write the message attributes to the
     */
    protected void writeWireForm() throws WireFormException {
        super.writeWireForm();
        pings.writeWireForm(wireForm, pingBitIdx, pingBitSz);
    }

    /**
     * Read the attributes from the wire format.
     *
     * @param buf byte[]
     */
    protected void readWireForm(ByteBuffer buf) throws IOException,
        WireFormException, ClassNotFoundException {
        super.readWireForm(buf);
        pings = NodeIdSet.readWireForm(wireForm, pingBitIdx, pingBitSz);
    }

    public String toString() {
        return "[" + super.toString() + ", pings=" + pings.toString() + "]";
    }

}
