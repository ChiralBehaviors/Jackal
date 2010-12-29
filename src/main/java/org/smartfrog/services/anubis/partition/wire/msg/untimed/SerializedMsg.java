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
package org.smartfrog.services.anubis.partition.wire.msg.untimed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

public final class SerializedMsg extends WireMsg {

    public static final int MESSAGE_MSG_WIRE_SIZE = UNDEFINED_SIZE;

    public static final int MESSAGE_MSG_WIRE_TYPE = 500;
    public static final int SERIALIZED_MSG_WIRE_TYPE = 999;
    private static final int payloadLengthIdx = WIRE_SIZE;
    private static final int payloadIdx = payloadLengthIdx + intSz;

    private byte[] payload = null;

    private int payloadSz = UNDEFINED_SIZE;
    protected Object msg;

    protected SerializedMsg() {
        super();
    }

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

    @Override
    protected void fixDynamicSizedAttributes() {
        try {
            ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
            ObjectOutputStream objectOS = new ObjectOutputStream(byteArrayOS);
            objectOS.writeObject(msg);
            objectOS.flush();
            payload = byteArrayOS.toByteArray();
            payloadSz = payload.length;
        } catch (IOException ex) {
            ex.printStackTrace();
            payload = null;
            payloadSz = UNDEFINED_SIZE;
        }
    }

    public Object getObject() {
        return msg;
    }

    @Override
    public int getSize() throws WireFormException {
        if (payloadSz == UNDEFINED_SIZE) {
            throw new WireFormException(
                                        "Attampt to get size of message when it has not been defined");
        }
        return payloadIdx + payloadSz;
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

        super.readWireForm(buf);

        payloadSz = wireForm.getInt(payloadLengthIdx);
        payload = new byte[payloadSz];

        for (int i = 0, j = payloadIdx; i < payloadSz; i++, j++) {
            payload[i] = wireForm.get(j);
        }

        ByteArrayInputStream byteArrayIS = new ByteArrayInputStream(payload);
        ObjectInputStream objectIS = new ObjectInputStream(byteArrayIS);
        msg = objectIS.readObject();

        //        payload = null;
        //        payloadSz = UNDEFINED_SIZE;
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

    /**
     * Writes the timed message attributes to wire form in the given byte array
     * 
     */
    @Override
    protected void writeWireForm() throws WireFormException {
        super.writeWireForm();

        wireForm.putInt(payloadLengthIdx, payloadSz);
        for (int i = 0, j = payloadIdx; i < payloadSz; i++, j++) {
            wireForm.put(j, payload[i]);
        }

    }

}
