/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Locale.UK;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.unsignedIntegers.Unsigned32BitInteger;

public class Seconds implements Serializable, Comparable<Seconds> {
    public static Seconds currentTime() {
        return seconds(currentTimeMillis() / 1000);
    }

    public static Seconds seconds(final long value) {
        return seconds(new Unsigned32BitInteger(value));
    }

    public static Seconds seconds(final Unsigned32BitInteger value) {
        return new Seconds(value);
    }

    private final Unsigned32BitInteger value;

    public Seconds(final Unsigned32BitInteger value) {
        this.value = value;
    }

    public Seconds add(final Seconds offset) {
        return seconds(value.add(offset.value));
    }

    public Seconds chooseSmallestValue(final Seconds that) {
        switch (compareTo(that)) {
            case -1:
                return this;
            case 0:
                return this;
            case 1:
                return that;
            default:
                return that;
        }
    }

    public int compareTo(final Seconds that) {
        return value.compareTo(that.value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Seconds seconds = (Seconds) o;
        return value.equals(seconds.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeUnsigned32BitInteger(value);
    }

    @Override
    public String toString() {
        return format(UK, "%1$s second(s)", value);
    }
}
