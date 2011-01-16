/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.messaging.serializer.ByteSerializer.serialize;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ClassTest {
    @Test
    public void serializesAsTwoOctets() {
        assertThat(serialize(QClass.Internet), is(equalTo(new byte[] { 0x00,
                                                                      0x01 })));
    }
}
