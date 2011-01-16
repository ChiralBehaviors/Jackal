/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.names;

import static com.softwarecraftsmen.dns.labels.ServiceLabel.serviceLabel;
import static com.softwarecraftsmen.dns.labels.ServiceProtocolLabel.TCP;
import static com.softwarecraftsmen.dns.labels.SimpleLabel.Empty;
import static com.softwarecraftsmen.dns.labels.SimpleLabel.simpleLabel;
import static com.softwarecraftsmen.dns.names.DomainName.domainName;
import static com.softwarecraftsmen.dns.names.ServiceName.serviceName;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.softwarecraftsmen.ConvenientArrayList;
import com.softwarecraftsmen.dns.labels.Label;

import static org.hamcrest.Matchers.*;

public class ServiceNameTest {
    @Test
    public void toLabelsMatchesExactStructure() {
        final List<Label> labels = serviceName(
                                               serviceLabel("http"),
                                               TCP,
                                               domainName("softwarecraftsmen.com")).toLabels();
        assertThat(new ConvenientArrayList<Label>(
                                                  serviceLabel("http"),
                                                  TCP,
                                                  simpleLabel("softwarecraftsmen"),
                                                  simpleLabel("com"), Empty),
                   equalTo(labels));
    }
}
