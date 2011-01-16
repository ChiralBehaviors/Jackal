/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.softwarecraftsmen.CanNeverHappenException;

public final class ByteSerializer {
    public static final int MaximumDnsMessageSize = 65535;

    public static byte[] serialize(final Serializable toBeSerialized) {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        toBeSerialized.serialize(new AtomicWriter(byteArrayOutputStream));
        try {
            byteArrayOutputStream.close();
        } catch (final IOException cause) {
            throw new CanNeverHappenException(cause);
        }
        final byte[] bytes = byteArrayOutputStream.toByteArray();
        if (bytes.length > MaximumDnsMessageSize) {
            throw new IllegalStateException(
                                            "Maximum DNS message size is 65535 bytes");
        }
        return bytes;
    }

    private ByteSerializer() {
    }
}
