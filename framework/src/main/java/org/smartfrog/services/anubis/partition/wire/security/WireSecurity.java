package org.smartfrog.services.anubis.partition.wire.security;

import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

public interface WireSecurity {

    public WireMsg fromWireForm(byte[] bytes) throws WireSecurityException,
                                             WireFormException;

    public byte[] toWireForm(WireMsg msg) throws WireFormException;

}
