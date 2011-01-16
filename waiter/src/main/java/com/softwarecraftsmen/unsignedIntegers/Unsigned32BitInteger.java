package com.softwarecraftsmen.unsignedIntegers;

import static java.lang.String.format;
import static java.util.Locale.UK;

import java.io.IOException;
import java.io.OutputStream;

public class Unsigned32BitInteger implements Comparable<Unsigned32BitInteger> {

    public static Unsigned32BitInteger Zero = new Unsigned32BitInteger(0);

    public static Unsigned32BitInteger unsigned32BitInteger(final long value) {
        if (value == 0) {
            return Zero;
        }
        return new Unsigned32BitInteger(value);
    }

    private final long value;

    public Unsigned32BitInteger(final long value) {
        if (value < 0l || value > 4294967296l) {
            throw new IllegalArgumentException(
                                               format(UK,
                                                      "The value %1$s is not a valid unsigned 32 bit integer",
                                                      value));
        }
        this.value = value;
    }

    public Unsigned32BitInteger add(final Unsigned16BitInteger unsigned16BitInteger) {
        return new Unsigned32BitInteger(value + unsigned16BitInteger.toLong());
    }

    public Unsigned32BitInteger add(final Unsigned32BitInteger that) {
        return unsigned32BitInteger(value + that.value);
    }

    public int compareTo(final Unsigned32BitInteger unsigned32BitInteger) {
        if (value == unsigned32BitInteger.value) {
            return 0;
        }
        return value < unsigned32BitInteger.value ? -1 : 1;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Unsigned32BitInteger that = (Unsigned32BitInteger) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ value >>> 32);
    }

    public long to() {
        return value;
    }

    @Override
    public String toString() {
        return format(UK, "%1$s", value);
    }

    public void write(final OutputStream stream) throws IOException {
        stream.write((int) (value >>> 24 & 0xFF));
        stream.write((int) (value >>> 16 & 0xFF));
        stream.write((int) (value >>> 8 & 0xFF));
        stream.write((int) (value & 0xFF));
    }
}
