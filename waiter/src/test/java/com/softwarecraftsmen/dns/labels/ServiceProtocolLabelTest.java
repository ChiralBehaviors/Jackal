/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.labels;

import static com.softwarecraftsmen.dns.labels.ServiceProtocolLabel.TCP;
import static com.softwarecraftsmen.dns.labels.ServiceProtocolLabel.toServiceProtocolLabel;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ServiceProtocolLabelTest {
    @Test
    public void isEmptyIsAlwaysFalse() {
        assertTrue(TCP.isEmpty());
    }

    @Test
    public void length() {
        assertThat(TCP.length(), is(4));
    }

    @Test
    public void toServiceProtocolLabelPresent() {
        assertThat(toServiceProtocolLabel("tcp"), is(TCP));
        assertThat(toServiceProtocolLabel("_tcp"), is(TCP));
    }

    @Test(expected = IllegalArgumentException.class)
    public void toServiceProtocolLabelUnrecognised() {
        toServiceProtocolLabel("notrecognised");
    }

    @Test
    public void toStringRepresentationHasAnUnderscore() {
        assertThat(TCP.toStringRepresentation(), is("_tcp"));
    }
}
