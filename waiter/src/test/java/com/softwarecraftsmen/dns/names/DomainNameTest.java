/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.names;

import static com.softwarecraftsmen.dns.messaging.serializer.ByteSerializer.serialize;
import static com.softwarecraftsmen.dns.names.DomainName.domainName;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.junit.Test;

public class DomainNameTest {
    @Test
    public void serializeProducesTheExpectedBytes() throws IOException {
        assertThat(serialize(domainName("mydomain.com")),
                   is(equalTo(new byte[] { 0x08, 0x6D, 0x79, 0x64, 0x6F, 0x6D,
                                          0x61, 0x69, 0x6E, 0x03, 0x63, 0x6F,
                                          0x6D, 0x00 })));
    }
}