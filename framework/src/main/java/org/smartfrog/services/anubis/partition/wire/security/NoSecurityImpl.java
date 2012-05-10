package org.smartfrog.services.anubis.partition.wire.security;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.wire.Wire;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

import com.hellblazer.jackal.util.ByteBufferPool;

public class NoSecurityImpl implements WireSecurity {

    @Override
    public WireMsg fromWireForm(ByteBuffer wireFrom)
                                                    throws WireSecurityException,
                                                    WireFormException {
        try {
            return Wire.fromWire(wireFrom);
        } catch (Exception e) {
            throw new WireFormException("Unable to unmarshall message", e);
        }
    }

    /* (non-Javadoc)
     * @see org.smartfrog.services.anubis.partition.wire.security.WireSecurity#toWireForm(org.smartfrog.services.anubis.partition.wire.WireMsg, com.hellblazer.jackal.util.ByteBufferPool)
     */
    @Override
    public ByteBuffer toWireForm(WireMsg msg, ByteBufferPool bufferPool)
                                                                        throws WireFormException, IOException {
        return msg.toWire(bufferPool);
    }

}
