/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.messaging.serializer.ByteSerializer.serialize;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.One;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MessageIdTest {
    @Test
    public void serializesAsTwoOctets() {
        assertThat(serialize(new MessageId(One)),
                   is(equalTo(new byte[] { 0x00, 0x01 })));
    }
}
