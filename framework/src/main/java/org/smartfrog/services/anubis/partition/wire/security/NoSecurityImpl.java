package org.smartfrog.services.anubis.partition.wire.security;

import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.wire.Wire;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

import com.hellblazer.jackal.partition.comms.Formable;

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

    @Override
    public Formable toWireForm(final WireMsg msg) {
        return msg;
    }

}
