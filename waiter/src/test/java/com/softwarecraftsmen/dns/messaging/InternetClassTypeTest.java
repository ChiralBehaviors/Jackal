/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.messaging.serializer.ByteSerializer.serialize;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class InternetClassTypeTest {
    @Test
    public void serializesAsTwoOctets() {
        assertThat(serialize(InternetClassType.DLV),
                   is(equalTo(new byte[] { -0x80, 0x01, })));
    }
}
