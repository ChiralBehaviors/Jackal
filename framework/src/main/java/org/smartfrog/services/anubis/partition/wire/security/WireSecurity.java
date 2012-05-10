package org.smartfrog.services.anubis.partition.wire.security;

import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

import com.hellblazer.jackal.partition.comms.Formable;

public interface WireSecurity {

    public WireMsg fromWireForm(ByteBuffer bytes) throws WireSecurityException,
                                                 WireFormException;

    public Formable toWireForm(WireMsg msg);

}
