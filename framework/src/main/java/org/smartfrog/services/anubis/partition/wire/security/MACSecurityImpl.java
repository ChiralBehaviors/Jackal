package org.smartfrog.services.anubis.partition.wire.security;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.crypto.ShortBufferException;

import org.smartfrog.services.anubis.partition.wire.Wire;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

import com.hellblazer.jackal.util.ByteBufferPool;

public class MACSecurityImpl implements WireSecurity {

    private MACData macData;

    @Override
    public WireMsg fromWireForm(ByteBuffer wireForm)
                                                    throws WireSecurityException,
                                                    WireFormException {
        try {

            WireMsg msg = Wire.fromWire(wireForm);
            macData.checkMAC(wireForm.array(), 0,
                             wireForm.capacity() - macData.getMacSize() - 1);

            return msg;
        } catch (ClassNotFoundException e) {
            throw new WireFormException("Unable to unmarshall message", e);
        } catch (IOException e) {
            throw new WireFormException("Unable to unmarshall message", e);
        } catch (SecurityException e) {
            throw new WireSecurityException(
                                            "Security violation unmarshalling message",
                                            e);
        }
    }

    public MACData getMacData() {
        return macData;
    }

    public void setMacData(MACData macData) {
        this.macData = macData;
    }

    /* (non-Javadoc)
     * @see org.smartfrog.services.anubis.partition.wire.security.WireSecurity#toWireForm(org.smartfrog.services.anubis.partition.wire.WireMsg, com.hellblazer.jackal.util.ByteBufferPool)
     */
    @Override
    public ByteBuffer toWireForm(WireMsg msg, ByteBufferPool bufferPool)
                                                                        throws WireFormException {
        try {
            msg.setTrailerSize(macData.getMacSize());
            ByteBuffer wireForm = msg.toWire(bufferPool);
            macData.addMAC(wireForm.array(), 0,
                           wireForm.capacity() - macData.getMacSize() - 1);
            return wireForm;

        } catch (IOException e) {
            throw new WireFormException("Unable to marshall message", e);
        } catch (ShortBufferException e) {
            throw new WireFormException(
                                        "Unable to marshall message - buffer not large enough for MAC security data",
                                        e);
        } catch (SecurityException e) {
            throw new WireFormException(
                                        "Unable to marshall message - security issue",
                                        e);
        }
    }
}
