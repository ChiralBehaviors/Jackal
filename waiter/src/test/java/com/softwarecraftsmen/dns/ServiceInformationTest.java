/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns;

import static com.softwarecraftsmen.dns.ServiceInformation.serviceInformation;
import static com.softwarecraftsmen.dns.names.HostName.hostName;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Four;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.One;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Zero;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;

import static org.hamcrest.Matchers.*;

public class ServiceInformationTest {
    private static final Unsigned16BitInteger somePort = One;
    private static final HostName someHostName = hostName("www.google.com.");

    @Test
    public void compareToSortsBasedOnPriorityFirst() {
        final ServiceInformation lowest = serviceInformation(Zero, Zero,
                                                             somePort,
                                                             someHostName);
        final ServiceInformation middleAndSame1 = serviceInformation(One, Zero,
                                                                     somePort,
                                                                     someHostName);
        final ServiceInformation middleAndSame2 = serviceInformation(One, Zero,
                                                                     somePort,
                                                                     someHostName);
        final ServiceInformation highest = serviceInformation(Four, Zero,
                                                              somePort,
                                                              someHostName);

        assertThat(lowest, lessThan(middleAndSame1));
        assertThat(lowest, lessThan(middleAndSame2));
        assertThat(middleAndSame1, lessThan(highest));
        assertThat(middleAndSame2, lessThan(highest));

        assertThat(lowest, lessThan(highest));
        assertThat(highest, greaterThan(middleAndSame1));
        assertThat(highest, greaterThan(middleAndSame2));
        assertThat(highest, greaterThan(lowest));

        assertThat(middleAndSame1, lessThanOrEqualTo(middleAndSame2));
        assertThat(middleAndSame2, lessThanOrEqualTo(middleAndSame1));
    }

    @Test
    public void compareToSortsBasedOnPriorityThenWeight() {
        final ServiceInformation samePriorityZeroWeight = serviceInformation(One,
                                                                             Zero,
                                                                             somePort,
                                                                             someHostName);
        final ServiceInformation samePriorityOneWeight = serviceInformation(One,
                                                                            One,
                                                                            somePort,
                                                                            someHostName);

        assertThat(samePriorityZeroWeight, lessThan(samePriorityOneWeight));
        assertThat(samePriorityOneWeight, greaterThan(samePriorityZeroWeight));
    }
}
