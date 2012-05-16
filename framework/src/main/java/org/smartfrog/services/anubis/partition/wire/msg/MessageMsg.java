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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.WireFormException;

import com.hellblazer.jackal.util.ByteBufferOutputStream;
import com.hellblazer.jackal.util.ByteBufferPool;

public final class MessageMsg extends TimedMsg {

    public static final int     MESSAGE_MSG_WIRE_SIZE = UNDEFINED_SIZE;
    public static final int     MESSAGE_MSG_WIRE_TYPE = 400;
    private static final byte[] headerPadding         = new byte[TIMED_MSG_WIRE_SIZE];

    private Object              message               = null;

    public MessageMsg(ByteBuffer wireForm) throws ClassNotFoundException,
                                          WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }

    public MessageMsg(Identity id, Object msg) {
        super(id);
        message = msg;
    }

    protected MessageMsg() {
        super();
    }

    public Object getMessageObject() {
        return message;
    }

    @Override
    public int getSize() throws WireFormException {
        throw new WireFormException(
                                    "Cannot predefine the size of this message type");
    }

    @Override
    public String toString() {
        return "[message " + super.toString()
               + (message == null ? "null" : message.toString()) + "]";
    }

    @Override
    protected int getType() {
        return MESSAGE_MSG_WIRE_TYPE;
    }

    /**
     * Sets the timed message attributes to the wire form held in a byte array
     * 
     * @param buf
     *            byte[]
     * @throws IOException
     * @throws WireFormException
     * @throws ClassNotFoundException
     */
    @Override
    protected void readWireForm(ByteBuffer buf) throws IOException,
                                               WireFormException,
                                               ClassNotFoundException {
        super.readWireForm(buf);
        ByteArrayInputStream bais = new ByteArrayInputStream(buf.array(),
                                                             buf.arrayOffset(),
                                                             buf.limit());
        bais.skip(TIMED_MSG_WIRE_SIZE);
        ObjectInputStream ois = new ObjectInputStream(bais);
        message = ois.readObject();
    }

    /**
     * Writes the timed message attributes to wire form in the given byte array
     * 
     * @param bufferPool
     * 
     * @throws IOException
     */
    protected ByteBuffer writeWireForm(ByteBufferPool bufferPool)
                                                                 throws WireFormException,
                                                                 IOException {
        ByteBufferOutputStream bbos = new ByteBufferOutputStream(bufferPool);
        bbos.write(headerPadding);
        ObjectOutputStream objectOS = new ObjectOutputStream(bbos);
        objectOS.writeObject(message);
        objectOS.flush();
        bbos.write(new byte[trailerSize]);
        ByteBuffer wireForm = bbos.toByteBuffer();
        writeWireForm(wireForm);
        return wireForm;
    }

    /* (non-Javadoc)
     * @see org.smartfrog.services.anubis.partition.wire.WireMsg#toWire(com.hellblazer.jackal.util.ByteBufferPool)
     */
    @Override
    public ByteBuffer toWire(ByteBufferPool bufferPool)
                                                       throws WireFormException,
                                                       IOException {
        ByteBuffer wireForm = writeWireForm(bufferPool);
        wireForm.putInt(0, getType());
        wireForm.rewind();
        return wireForm;
    }
}
