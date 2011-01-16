/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.unsignedIntegers;

import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.unsigned16BitInteger;
import static java.lang.String.format;
import static java.util.Locale.UK;

public class Unsigned4BitInteger {
    private final int value;

    public static final Unsigned4BitInteger Zero = new Unsigned4BitInteger(0);

    public static final Unsigned4BitInteger One = new Unsigned4BitInteger(1);

    public static final Unsigned4BitInteger Two = new Unsigned4BitInteger(2);

    public static final Unsigned4BitInteger Three = new Unsigned4BitInteger(3);

    public static final Unsigned4BitInteger Four = new Unsigned4BitInteger(4);

    public static final Unsigned4BitInteger Five = new Unsigned4BitInteger(5);

    public static Unsigned4BitInteger unsigned4BitInteger(final int value) {
        return new Unsigned4BitInteger(value);
    }

    private Unsigned4BitInteger(final int value) {
        if (value < 0 || value > 15) {
            throw new IllegalArgumentException(
                                               format(UK,
                                                      "The value %1$s is not a valid unsigned 4 bit integer (nibble)",
                                                      value));
        }
        this.value = value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Unsigned4BitInteger that = (Unsigned4BitInteger) o;

        return value == that.value;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return format(UK, "%1$s", value);
    }

    public Unsigned16BitInteger toUnsigned16BitInteger() {
        return unsigned16BitInteger(value);
    }
}
