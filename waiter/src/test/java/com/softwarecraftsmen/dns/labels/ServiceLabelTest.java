/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.labels;

import static com.softwarecraftsmen.dns.labels.ServiceLabel.serviceLabel;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.softwarecraftsmen.dns.labels.ServiceLabel.ServiceClassLabelMustBeLessThan15CharactersException;

public class ServiceLabelTest {
    @Test(expected = IllegalArgumentException.class)
    public void aServiceClassLabelCanNotBeEmpty() {
        serviceLabel("");
    }

    @Test(expected = ServiceClassLabelMustBeLessThan15CharactersException.class)
    public void aServiceClassLabelMustBeLessThan14Characters() {
        serviceLabel("012345678901234");
    }

    @Test(expected = ServiceClassLabelMustBeLessThan15CharactersException.class)
    public void aServiceClassLabelMustBeLessThan15CharactersIfItStartsWithAnUnderscore() {
        serviceLabel("_012345678901234");
    }

    @Test
    public void isEmptyIsAlwaysFalse() {
        assertFalse(serviceLabel("http").isEmpty());
    }

    @Test
    public void length() {
        assertThat(serviceLabel("http").length(), is(5));
    }

    @Test
    public void toStringRepresentationHasAnUnderscore() {
        assertThat(serviceLabel("http").toStringRepresentation(), is("_http"));
        assertThat(serviceLabel("_http").toStringRepresentation(), is("_http"));
    }
}
