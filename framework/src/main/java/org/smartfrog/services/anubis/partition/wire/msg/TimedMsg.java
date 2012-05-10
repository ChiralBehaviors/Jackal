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

import org.smartfrog.services.anubis.partition.protocols.Sender;
import org.smartfrog.services.anubis.partition.protocols.Timed;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

abstract public class TimedMsg extends WireMsg implements Timed, Sender {

    static final private int    timeIdx             = WIRE_SIZE;

    static final private int    timeSz              = longSz;
    static final private int    identitySz          = Identity.identityWireSz;
    static final private int    identityIdx         = timeIdx + timeSz;
    static final private int    addressIdx          = identityIdx + identitySz;
    static final private int    addressSz           = AddressMarshalling.connectionAddressWireSz;
    public static final int     TIMED_MSG_WIRE_SIZE = addressIdx + addressSz;
    protected InetSocketAddress address             = null;

    protected Identity          sender;

    protected long              time;

    /**
     * Constructor - Construct a timed message without setting attributes. used
     * to construct when reading from wire. Construct in the fromWire(wire)
     * method, read attributes in the readWireForm(wire) method.
     * 
     */
    public TimedMsg(ByteBuffer wireForm) throws ClassNotFoundException,
                                        WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }

    /**
     * Constructor - A timed message may or may not have and address, all have a
     * time and a sender id.
     * 
     * @param id
     *            - the sender id
     */
    public TimedMsg(Identity id) {
        sender = id;
    }

    /**
     * @param id
     *            - sender id
     * @param addr
     *            - sender address
     */
    public TimedMsg(Identity id, InetSocketAddress addr) {
        sender = id;
        address = addr;
    }

    protected TimedMsg() {
        super();
    }

    /**
     * Sender interface implemenation
     * 
     * @return identity
     */
    @Override
    public Identity getSender() {
        return sender;
    }

    @Override
    public InetSocketAddress getSenderAddress() {
        return address;
    }

    @Override
    public int getSize() throws WireFormException {
        return TIMED_MSG_WIRE_SIZE;
    }

    /**
     * Timed interface
     * 
     * @return long time
     */
    @Override
    public long getTime() {
        return time;
    }

    @Override
    public void setTime(long t) {
        time = t;
    }

    /**
     * toString method for debugging and logging
     * 
     * @return String
     */
    @Override
    public String toString() {
        return "[" + time + ", " + address + "]";
    }

    /**
     * Sets the timed message attributes to the wire form held in a byte array
     * 
     * @param buf
     * @throws IOException
     * @throws WireFormException
     * @throws ClassNotFoundException
     */
    @Override
    protected void readWireForm(ByteBuffer buf) throws IOException,
                                               WireFormException,
                                               ClassNotFoundException {
        time = buf.getLong(timeIdx);
        sender = Identity.readWireForm(buf, identityIdx);
        address = AddressMarshalling.readWireForm(buf, addressIdx);
    }

    protected void writeWireForm(ByteBuffer wireForm) throws WireFormException,
                                                     IOException {
        wireForm.putLong(timeIdx, time);
        sender.writeWireForm(wireForm, identityIdx);
        if (address != null) {
            AddressMarshalling.writeWireForm(address, wireForm, addressIdx);
        } else {
            AddressMarshalling.writeNullWireForm(wireForm, addressIdx);
        }
    }

}
