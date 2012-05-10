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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.wire.WireFormException;

import com.hellblazer.jackal.util.ByteBufferPool;

public class PingHeartbeatMsg extends HeartbeatMsg {

    static final private int pingBitSz                    = MAX_BIT_SIZE
                                                            + intSz;
    public static final int  PING_HEARTBEAT_MSG_WIRE_SIZE = HEARTBEAT_MSG_WIRE_SIZE
                                                            + pingBitSz;

    public static final int  PING_HEARTBEAT_MSG_WIRE_TYPE = 310;
    static final private int pingBitIdx                   = HEARTBEAT_MSG_WIRE_SIZE;

    private NodeIdSet        pings;

    public PingHeartbeatMsg(ByteBuffer wireForm) throws ClassNotFoundException,
                                                WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }

    /**
     * Construct a ping heartbeat message
     * 
     * @param identity
     * @param address
     */
    public PingHeartbeatMsg(Identity identity, InetSocketAddress address) {
        super(identity, address);
        pings = new NodeIdSet();
    }

    public PingHeartbeatMsg(PingHeartbeatMsg pinghb) {
        super(pinghb);
        synchronized (this) {
            pings = pinghb.pings;
        }
    }

    /**
     * Constructor - used internally when reading from wire
     */
    protected PingHeartbeatMsg() {
        super();
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

    @Override
    public int getSize() {
        return PING_HEARTBEAT_MSG_WIRE_SIZE;
    }

    /**
     * pings accessors
     * 
     * @param id
     *            Identity
     */
    public synchronized void setPingBit(Identity id) {
        pings.add(id.id);
    }

    /**
     * generate close message from this heartbeat
     */
    @Override
    public HeartbeatMsg toClose() {
        return new PingCloseMsg(this);
    }

    @Override
    public synchronized String toString() {
        return "[" + super.toString() + ", pings=" + pings.toString() + "]";
    }

    @Override
    protected int getType() {
        return PING_HEARTBEAT_MSG_WIRE_TYPE;
    }

    /**
     * Read the attributes from the wire format.
     * 
     * @param buf
     *            byte[]
     */
    @Override
    protected synchronized void readWireForm(ByteBuffer buf)
                                                            throws IOException,
                                                            WireFormException,
                                                            ClassNotFoundException {
        super.readWireForm(buf);
        pings = NodeIdSet.readWireForm(buf, pingBitIdx, pingBitSz);
    }

    /* (non-Javadoc)
     * @see org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg#writeWireForm(com.hellblazer.jackal.util.ByteBufferPool)
     */
    @Override
    protected ByteBuffer writeWireForm(ByteBufferPool bufferPool)
                                                                 throws WireFormException,
                                                                 IOException {
        ByteBuffer wireForm = super.writeWireForm(bufferPool);
        pings.writeWireForm(wireForm, pingBitIdx, pingBitSz);
        return wireForm;
    }

}
