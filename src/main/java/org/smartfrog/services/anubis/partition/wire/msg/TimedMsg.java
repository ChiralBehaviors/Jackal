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
import org.smartfrog.services.anubis.partition.protocols.Sender;
import org.smartfrog.services.anubis.partition.protocols.Timed;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

public class TimedMsg extends WireMsg implements Timed, Sender {

    protected long              time;
    protected long              order = -1;
    protected Identity          sender;
    protected ConnectionAddress address  = null;

    private boolean addressUnmarshalled = false;

    static final private int timeIdx = WIRE_SIZE;
    static final private int timeSz  = longSz;
    static final private int orderIdx = timeIdx + timeSz;
    static final private int orderSz = longSz;
    static final private int identityIdx = orderIdx + orderSz;
    static final private int identitySz = Identity.identityWireSz;
    static final private int addressIdx = identityIdx + identitySz;
    static final private int addressSz = ConnectionAddress.connectionAddressWireSz;

    public static final int TIMED_MSG_WIRE_SIZE = addressIdx + addressSz;
    public static final int TIMED_MSG_WIRE_TYPE = 200;

    protected int getType() { return TIMED_MSG_WIRE_TYPE; }
    public int getSize() throws WireFormException { return TIMED_MSG_WIRE_SIZE; }

    protected TimedMsg() {
        super();
    }

    /**
     * Constructor - Construct a timed message without setting attributes.
     *               used to construct when reading from wire. Construct in
     *               the fromWire(wire) method, read attributes in the
     *               readWireForm(wire) method.
     *
     */
    public TimedMsg(ByteBuffer wireForm) throws ClassNotFoundException, WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }


    /**
     * @param id - sender id
     * @param addr - sender address
     */
    public TimedMsg(Identity id, ConnectionAddress addr)  {
        sender = id;
        address = addr;
        addressUnmarshalled = true;
    }

    /**
     * Constructor - A timed message may or may not have and address, all
     *               have a time and a sender id.
     * @param id - the sender id
     */
    public TimedMsg(Identity id) {
        sender = id;
        addressUnmarshalled = true;
    }


    /**
     * Timed interface
     * @return  long time
     */
    public long   getTime()       { return time; }
    public void   setTime(long t) { time = t; }
    
    /**
     * Msg order (only used in ordered connections)
     * @return the order (-1 if not set)
     */
    public long   getOrder()      { return order; }
    public void   setOrder(long o){ order = o; }

    /**
     * Sender interface implemenation
     * @return  identity
     */
    public Identity          getSender()        { return sender; }
    public ConnectionAddress getSenderAddress() {
        if( !addressUnmarshalled )
            addressFromWire();
        return address;
    }

    /**
     * toString method for debugging and logging
     *
     * @return String
     */
    public String toString() {
        return "[" + time + ", " + order + ", "  + (addressUnmarshalled ? sender.toString() : "SENDER_ADDRESS_MARSHALLED") + ", " + address + "]";
    }

    private void addressFromWire() {
        addressUnmarshalled = true;
        address = ConnectionAddress.readWireForm(wireForm, addressIdx);
    }

    /**
     * Sets the timed message attributes to the wire form held in a
     * byte array
     * @param buf
     * @throws IOException
     * @throws WireFormException
     * @throws ClassNotFoundException
     */
    protected void readWireForm(ByteBuffer buf) throws IOException, WireFormException, ClassNotFoundException {
        super.readWireForm(buf);
        time = wireForm.getLong(timeIdx);
        order = wireForm.getLong(orderIdx);
        sender = Identity.readWireForm(wireForm, identityIdx);
        address = ConnectionAddress.readWireForm(wireForm, addressIdx);
    }
    /**
     * Writes the timed message attributes to wire form in the given byte
     * array
     *
     */
    protected void writeWireForm() throws WireFormException {
        super.writeWireForm();
        wireForm.putLong(timeIdx, time);
        wireForm.putLong(orderIdx, order);
        sender.writeWireForm(wireForm, identityIdx);
        if( address != null )
            address.writeWireForm(wireForm, addressIdx);
        else
            ConnectionAddress.writeNullWireForm(wireForm, addressIdx);
    }



}
