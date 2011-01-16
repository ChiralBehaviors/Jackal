/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns;

import static com.softwarecraftsmen.dns.ServiceInformation.serviceInformation;
import static com.softwarecraftsmen.dns.ServiceInformationPrioritised.prioritise;
import static com.softwarecraftsmen.dns.names.HostName.hostName;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Four;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.One;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.Zero;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Test;

import com.softwarecraftsmen.dns.ServiceInformationPrioritised.WeightRandomNumberGenerator;
import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;

public class ServiceInformationPrioritisedTest {
    public static final class BentWeightRandomNumberGenerator implements
            WeightRandomNumberGenerator {

        public Unsigned16BitInteger generate(final Unsigned16BitInteger maximum) {
            return maximum;
        }
    }

    private static final Unsigned16BitInteger somePort = One;

    private static final HostName someHostName = hostName("www.google.com.");

    @Test
    public void correctlySortsRecordsOfDifferentPrioritiesWhenAlreadySorted() {
        final ServiceInformation lower = serviceInformation(Zero, Zero,
                                                            somePort,
                                                            someHostName);
        final ServiceInformation higher = serviceInformation(One, Zero,
                                                             somePort,
                                                             someHostName);
        final Iterator<ServiceInformation> informationIterator = prioritise(
                                                                            lower,
                                                                            higher).iterator();
        assertThat(informationIterator.next(), is(lower));
        assertThat(informationIterator.next(), is(higher));
        assertFalse(informationIterator.hasNext());
    }

    @Test
    public void correctlySortsRecordsOfDifferentPrioritiesWhenNotAlreadySorted() {
        final ServiceInformation lower = serviceInformation(Zero, Zero,
                                                            somePort,
                                                            someHostName);
        final ServiceInformation higher = serviceInformation(One, Zero,
                                                             somePort,
                                                             someHostName);
        final Iterator<ServiceInformation> informationIterator = prioritise(
                                                                            higher,
                                                                            lower).iterator();
        assertThat(informationIterator.next(), is(lower));
        assertThat(informationIterator.next(), is(higher));
        assertFalse(informationIterator.hasNext());
    }

    @Test
    public void correctlySortsRecordsOfDifferentPrioritiesWhenTwoAreTheSamePriority() {
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
        final Iterator<ServiceInformation> informationIterator = prioritise(
                                                                            middleAndSame1,
                                                                            highest,
                                                                            lowest,
                                                                            middleAndSame2).iterator();
        assertThat(informationIterator.next(), is(lowest));
        assertThat(informationIterator.next(), is(middleAndSame1));
        assertThat(informationIterator.next(), is(middleAndSame2));
        assertThat(informationIterator.next(), is(highest));
        assertFalse(informationIterator.hasNext());
    }

    @Test
    public void randomlyOrdersByWeightThoseRecordsOfTheSamePriority() {
        final ServiceInformation lowest = serviceInformation(Zero, Zero,
                                                             somePort,
                                                             someHostName);
        final ServiceInformation middleAndSame1 = serviceInformation(Zero, One,
                                                                     somePort,
                                                                     someHostName);
        final ServiceInformation middleAndSame2 = serviceInformation(Zero, One,
                                                                     somePort,
                                                                     someHostName);
        final ServiceInformation highest = serviceInformation(Zero, Four,
                                                              somePort,
                                                              someHostName);

        final Iterator<ServiceInformation> informationIterator = new ServiceInformationPrioritised(
                                                                                                   new BentWeightRandomNumberGenerator(),
                                                                                                   new ArrayList<ServiceInformation>() {
                                                                                                       private static final long serialVersionUID = 1L;

                                                                                                       {
                                                                                                           add(highest);
                                                                                                           add(middleAndSame2);
                                                                                                           add(lowest);
                                                                                                           add(middleAndSame1);
                                                                                                       }
                                                                                                   }).iterator();

        assertThat(informationIterator.next(), is(lowest));
        assertThat(informationIterator.next(), is(middleAndSame1));
        assertThat(informationIterator.next(), is(middleAndSame2));
        assertThat(informationIterator.next(), is(highest));
        assertFalse(informationIterator.hasNext());
    }

    @Test
    public void supportsNotHavingAnyRecords() {
        final Iterator<ServiceInformation> informationIterator = prioritise().iterator();
        assertFalse(informationIterator.hasNext());
    }

    @Test
    public void supportsOneRecord() {
        final ServiceInformation expected = serviceInformation(Zero, Zero,
                                                               somePort,
                                                               someHostName);
        final Iterator<ServiceInformation> informationIterator = prioritise(
                                                                            expected).iterator();
        assertTrue(informationIterator.hasNext());
        assertThat(informationIterator.next(), is(expected));
        assertFalse(informationIterator.hasNext());
    }
}
