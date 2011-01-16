/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.names;

import static com.softwarecraftsmen.dns.labels.SimpleLabel.Empty;
import static com.softwarecraftsmen.dns.labels.SimpleLabel.simpleLabel;
import static com.softwarecraftsmen.dns.names.HostName.hostName;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.softwarecraftsmen.ConvenientArrayList;
import com.softwarecraftsmen.dns.labels.SimpleLabel;
import com.softwarecraftsmen.dns.names.AbstractName.EmptyLabelsAreNotAllowedInNamesExceptAtTheEnd;
import com.softwarecraftsmen.dns.names.AbstractName.NameIncludingPeriodsAndFinalEmptyLabelCanNotBeMoreThan255Characters;
import com.softwarecraftsmen.dns.names.AbstractName.TooManyLabelsException;

public class AbstractNameTest {
    private final static class AbstractNameForTest extends AbstractName {
        public AbstractNameForTest() {
            super(simpleLabel("www"), simpleLabel("google"), simpleLabel("com"));
        }
    }

    @Test
    public void constructsWithTerminalEmptyLabel() {
        assertThat(new HostName(simpleLabel("www"),
                                simpleLabel("softwarecraftsmen"),
                                simpleLabel("com")),
                   hasToString(equalTo("HostName(www.softwarecraftsmen.com.)")));
    }

    @Test
    public void doesNotConstructsWithEmptyTerminalLabelWhenSpecified() {
        assertThat(new HostName(simpleLabel("www"),
                                simpleLabel("softwarecraftsmen"),
                                simpleLabel("com"), Empty),
                   hasToString(equalTo("HostName(www.softwarecraftsmen.com.)")));
    }

    @Test(expected = EmptyLabelsAreNotAllowedInNamesExceptAtTheEnd.class)
    public void emptyLabelsAreNotAllowedExceptTerminally() {
        new HostName(simpleLabel("www"), Empty, simpleLabel("com"), Empty);
    }

    @Test
    public void nameHelperStaticMethodWorks() {
        assertThat(hostName("www.softwarecraftsmen.com"),
                   hasToString(equalTo("HostName(www.softwarecraftsmen.com.)")));
    }

    @Test
    public void nameHelperStaticMethodWorksWithRootDomain() {
        assertThat(hostName("www.softwarecraftsmen.com."),
                   hasToString(equalTo("HostName(www.softwarecraftsmen.com.)")));
    }

    @Test(expected = NameIncludingPeriodsAndFinalEmptyLabelCanNotBeMoreThan255Characters.class)
    public void nameLongerThan255Characters() {
        hostName("123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.123456789.12345.");
    }

    @Test(expected = IllegalArgumentException.class)
    public void noLabels() {
        new HostName();
    }

    @Test
    public void toLabelsMatchesExactStructure() {
        final List<SimpleLabel> labels = new AbstractNameForTest().toLabels();
        assertThat(new ConvenientArrayList<SimpleLabel>(simpleLabel("www"),
                                                        simpleLabel("google"),
                                                        simpleLabel("com"),
                                                        Empty), equalTo(labels));
    }

    @Test(expected = TooManyLabelsException.class)
    public void tooManyLabels() {
        final List<SimpleLabel> tooManyLabels = new ArrayList<SimpleLabel>();
        for (int index = 0; index < 129; index++) {
            tooManyLabels.add(simpleLabel("somelabel"));
        }
        new HostName(tooManyLabels);
    }
}
