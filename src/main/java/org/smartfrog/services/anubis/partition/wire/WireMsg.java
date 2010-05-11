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

public class WireMsg implements WireSizes {

    protected ByteBuffer wireForm    = null;
    protected byte[]     bytes       = null;
    protected int        trailerSize = 0;

    public static final int WIRE_TYPE = 100;
    protected static final int WIRE_SIZE = intSz;

    /**
     * Each subtype should over-ride getType() to return its own type.
     * @return int
     */
    protected int getType() { return WIRE_TYPE; }
    /**
     * Each subtype should over-ride getSize() to return its own
     * calculation of size.
     *
     * @return int
     * @throws WireFormException
     */
    public int getSize() throws WireFormException { return WIRE_SIZE; }
    
    /**
     * Set the size of the trailer. When the message is converted to its
     * wire form, the byte array will include a sequence of unused 
     * bytes equal to the trailer size at the end. The byte array length will
     * be getSize() + trailerSize.
     * 
     * @param n
     * @throws WireFormException
     */
    public void setTrailerSize(int n) throws WireFormException { 
        if( n < 0 ) {
            throw new WireFormException("Negative trailer size specified: " + n);
        } else {
            trailerSize = n;
        }
    }

    /**
     * Default constructor - used when constructing from wire form
     */
    protected WireMsg() {
        super();
    }

    /**
     * Construct from the wire form. Each substype should implement a similar
     * constructor.
     *
     * @param wireForm ByteBuffer
     * @throws ClassNotFoundException
     * @throws WireFormException
     * @throws IOException
     */
    public WireMsg(ByteBuffer wireForm) throws ClassNotFoundException, WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }

    /**
     * toWire() generates a byte array from this message. Messages that have
     * an attribute with dynamic size should implement the
     * fixDynamicSizeAttributes() method to prepare these attributes and so
     * that their getSize() method can return the actual desired size. Otherwise
     * this method should be over-ridden completely.
     *
     * their size in
     * @return byte[]
     * @throws WireFormException
     * @throws IOException
     */
    public byte[] toWire() throws WireFormException, IOException {

        fixDynamicSizedAttributes();

        bytes = new byte[getSize() + trailerSize];
        wireForm = ByteBuffer.wrap(bytes);

        writeWireForm();

        return bytes;
    }

    /**
     * This method should be over-ridden if the subtype has dynamic sized
     * attributes to determine their size and prepare them for writing.
     */
    protected void fixDynamicSizedAttributes() {}

    /**
     * top level readWireForm method. All subtypes should implement this method
     * to call their super.readWireForm(ByteBuffer) method and then read their
     * own attributes.
     *
     * @param buf ByteBuffer
     * @throws IOException
     * @throws WireFormException
     * @throws ClassNotFoundException
     */
    protected void readWireForm(ByteBuffer buf) throws IOException, WireFormException, ClassNotFoundException {

        this.wireForm = buf;
        this.bytes = buf.array();

        if( wireForm.getInt(0) != getType() )
            throw new WireFormException("Incorrect type in wire form - can not read as " + this.getClass());
    }

    /**
     * Top level writeWireForm() method.
     * subtypes should implement this method
     * to call their super.writeWireForm and then write their own attributes.
     *
     * @throws WireFormException on trouble
     */
    protected void writeWireForm() throws WireFormException {
        wireForm.putInt(0, getType());
    }

}
