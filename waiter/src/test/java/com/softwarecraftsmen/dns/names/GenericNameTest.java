/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.names;

import static com.softwarecraftsmen.ConvenientArrayList.toList;
import static com.softwarecraftsmen.dns.labels.SimpleLabel.Empty;
import static com.softwarecraftsmen.dns.labels.SimpleLabel.simpleLabel;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

import com.softwarecraftsmen.ConvenientArrayList;
import com.softwarecraftsmen.dns.labels.Label;
import com.softwarecraftsmen.dns.messaging.GenericName;

import static org.hamcrest.Matchers.*;

public class GenericNameTest {
    @Test
    public void toLabelsMatchesExactStructure() {
        final List<Label> labels = new GenericName(
                                                   toList(simpleLabel("www"),
                                                          simpleLabel("google"),
                                                          simpleLabel("com"),
                                                          Empty)).toLabels();
        final List<Label> actual = new ConvenientArrayList<Label>(
                                                                  simpleLabel("www"),
                                                                  simpleLabel("google"),
                                                                  simpleLabel("com"),
                                                                  Empty);
        assertThat(actual, equalTo(labels));
    }
}
