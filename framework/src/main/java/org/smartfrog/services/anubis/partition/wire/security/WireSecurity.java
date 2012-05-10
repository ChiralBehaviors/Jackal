package org.smartfrog.services.anubis.partition.wire.security;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireMsg;

import com.hellblazer.jackal.util.ByteBufferPool;

public interface WireSecurity {

    public WireMsg fromWireForm(ByteBuffer bytes) throws WireSecurityException,
                                                 WireFormException;

    public ByteBuffer toWireForm(WireMsg msg, ByteBufferPool bufferPool)
                                                                        throws WireFormException,
                                                                        IOException;

}
