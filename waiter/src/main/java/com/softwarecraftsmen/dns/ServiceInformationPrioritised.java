/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;

import java.util.Iterator;
import java.util.List;

import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;

public class ServiceInformationPrioritised implements
        Iterable<ServiceInformation> {

    public interface WeightRandomNumberGenerator {

        Unsigned16BitInteger generate(final Unsigned16BitInteger maximum);
    }

    private static final class RegularWeightRandomNumberGenerator implements
            WeightRandomNumberGenerator {

        public Unsigned16BitInteger generate(final Unsigned16BitInteger maximum) {
            return null;
        }
    }

    public static final WeightRandomNumberGenerator RegularRandomNumberGenerator = new RegularWeightRandomNumberGenerator();

    public static Iterable<ServiceInformation> prioritise(final ServiceInformation... serviceInformation) {
        return new ServiceInformationPrioritised(RegularRandomNumberGenerator,
                                                 asList(serviceInformation));
    }

    private final WeightRandomNumberGenerator weightRandomNumberGenerator;

    private final List<ServiceInformation> serviceInformationPrioritised;

    public ServiceInformationPrioritised(final WeightRandomNumberGenerator weightRandomNumberGenerator,
                                         final List<ServiceInformation> serviceInformationPrioritised) {
        this.weightRandomNumberGenerator = weightRandomNumberGenerator;
        this.serviceInformationPrioritised = serviceInformationPrioritised;
        sort(this.serviceInformationPrioritised);
    }

    public Iterator<ServiceInformation> iterator() {
        return unmodifiableList(serviceInformationPrioritised).iterator();
    }
}
