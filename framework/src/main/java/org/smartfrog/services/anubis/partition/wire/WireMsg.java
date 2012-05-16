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

import com.hellblazer.jackal.util.ByteBufferPool;

abstract public class WireMsg implements WireSizes {

    protected static final int WIRE_SIZE   = intSz;

    protected int              trailerSize = 0;

    /**
     * Construct from the wire form. Each substype should implement a similar
     * constructor.
     * 
     * @param wireForm
     *            ByteBuffer
     * @throws ClassNotFoundException
     * @throws WireFormException
     * @throws IOException
     */
    public WireMsg(ByteBuffer wireForm) throws ClassNotFoundException,
                                       WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }

    /**
     * Default constructor - used when constructing from wire form
     */
    protected WireMsg() {
    }

    /**
     * Each subtype should over-ride getSize() to return its own calculation of
     * size.
     * 
     * @return int
     * @throws WireFormException
     */
    public int getSize() throws WireFormException {
        return WIRE_SIZE;
    }

    /**
     * Set the size of the trailer. When the message is converted to its wire
     * form, the byte array will include a sequence of unused bytes equal to the
     * trailer size at the end. The byte array length will be getSize() +
     * trailerSize.
     * 
     * @param n
     * @throws WireFormException
     */
    public void setTrailerSize(int n) throws WireFormException {
        if (n < 0) {
            throw new WireFormException("Negative trailer size specified: " + n);
        }
        trailerSize = n;
    }

    /**
     * toWire() generates a ByteBuffer from this message. Messages that have an
     * attribute with dynamic size should implement the
     * fixDynamicSizeAttributes() method to prepare these attributes and so that
     * their getSize() method can return the actual desired size. Otherwise this
     * method should be over-ridden completely.
     * 
     * @param bufferPool
     * @return ByteBuffer
     * @throws WireFormException
     * @throws IOException
     */
    abstract public ByteBuffer toWire(ByteBufferPool bufferPool)
                                                                throws WireFormException,
                                                                IOException;

    /**
     * Each subtype should over-ride getType() to return its own type.
     * 
     * @return int
     */
    abstract protected int getType();

    /**
     * top level readWireForm method. All subtypes should implement this method
     * to call their super.readWireForm(ByteBuffer) method and then read their
     * own attributes.
     * 
     * @param buf
     *            ByteBuffer
     * @throws IOException
     * @throws WireFormException
     * @throws ClassNotFoundException
     */
    abstract protected void readWireForm(ByteBuffer buf) throws IOException,
                                                        WireFormException,
                                                        ClassNotFoundException;

}
