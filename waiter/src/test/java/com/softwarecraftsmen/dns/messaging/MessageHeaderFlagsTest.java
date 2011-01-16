/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.messaging.ResponseCode.NoErrorCondition;
import static com.softwarecraftsmen.dns.messaging.serializer.ByteSerializer.serialize;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned3BitInteger.Zero;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class MessageHeaderFlagsTest {
    @Test
    public void serializesQueryAsTwoOctets() {
        assertThat(serialize(MessageHeaderFlags.Query),
                   is(equalTo(new byte[] { 0x01, 0x00 })));
    }

    @Test
    public void serializesSomethingElseCorrectly() {
        assertThat(serialize(new MessageHeaderFlags(true, OperationCode.Query,
                                                    false, false, true, true,
                                                    Zero, NoErrorCondition)),
                   is(equalTo(new byte[] { 65, 0x02 })));
    }
}
