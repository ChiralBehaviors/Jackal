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

import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

import com.hellblazer.jackal.util.ByteBufferOutputStream;
import com.hellblazer.jackal.util.ByteBufferPool;

public final class SerializedMsg extends WireMsg {
    public static final int SERIALIZED_MSG_WIRE_TYPE = 999;

    protected Object        msg;

    public SerializedMsg(ByteBuffer wireForm) throws ClassNotFoundException,
                                             WireFormException, IOException {
        super();
        readWireForm(wireForm);
    }

    /**
     * constructor
     */
    public SerializedMsg(Object obj) {
        msg = obj;
    }

    protected SerializedMsg() {
        super();
    }

    public Object getObject() {
        return msg;
    }

    @Override
    public int getSize() throws WireFormException {
        throw new WireFormException(
                                    "Cannot predetermine the byte size of this message");
    }

    /**
     * toString method for debugging and logging
     * 
     * @return String
     */
    @Override
    public String toString() {
        return "[" + msg + "]";
    }

    @Override
    protected int getType() {
        return SERIALIZED_MSG_WIRE_TYPE;
    }

    /**
     * read wire message format
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
        ByteArrayInputStream bais = new ByteArrayInputStream(buf.array(),
                                                             buf.arrayOffset(),
                                                             buf.limit());
        bais.skip(intSz);
        ObjectInputStream ois = new ObjectInputStream(bais);
        msg = ois.readObject();
    }

    /* (non-Javadoc)
     * @see org.smartfrog.services.anubis.partition.wire.WireMsg#toWire(com.hellblazer.jackal.util.ByteBufferPool)
     */
    @Override
    public ByteBuffer toWire(ByteBufferPool bufferPool)
                                                       throws WireFormException,
                                                       IOException {

        ByteBufferOutputStream bbos = new ByteBufferOutputStream(bufferPool);
        bbos.write(new byte[intSz]);
        ObjectOutputStream objectOS = new ObjectOutputStream(bbos);
        objectOS.writeObject(msg);
        objectOS.flush();
        bbos.write(new byte[trailerSize]);
        ByteBuffer wireForm = bbos.toByteBuffer();
        wireForm.putInt(0, getType());
        wireForm.rewind();
        return wireForm;
    }
}
