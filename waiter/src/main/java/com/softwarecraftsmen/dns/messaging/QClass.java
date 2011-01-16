/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.unsigned16BitInteger;
import static java.lang.String.format;
import static java.util.Locale.UK;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;

public enum QClass implements Serializable {
    Reserved0000(unsigned16BitInteger(0x0000), "Reserved", false, true, false),
    Internet(unsigned16BitInteger(0x0001), "Internet", false, false, false),
    CSNET(unsigned16BitInteger(0x0002), "CS", true, false, false),
    Chaos(unsigned16BitInteger(0x0003), "CH", false, true, false),
    Hesiod(unsigned16BitInteger(0x0004), "HS", false, false, false),
    Any(unsigned16BitInteger(0x00FF), "Any", false, false, true),
    ReservedFFFF(unsigned16BitInteger(0x0000), "Reserved", false, true, false), ;

    public static QClass qclass(final Unsigned16BitInteger value) {
        for (QClass qClass : values()) {
            if (qClass.value.equals(value)) {
                return qClass;
            }
        }
        throw new IllegalArgumentException(
                                           format(UK,
                                                  "Unrecognised class code %1$s",
                                                  value));
    }

    private final Unsigned16BitInteger value;
    private final String description;
    private final boolean obsolete;
    private final boolean reserved;

    private final boolean isOnlyQClass;

    private QClass(final Unsigned16BitInteger value, final String description,
                   final boolean obsolete, final boolean reserved,
                   final boolean isOnlyQClass) {
        this.value = value;
        this.description = description;
        this.obsolete = obsolete;
        this.reserved = reserved;
        this.isOnlyQClass = isOnlyQClass;
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeUnsigned16BitInteger(value);
    }

    @Override
    public String toString() {
        return format(UK, "%1$s (%2$s) (%3$s) (%4$s) (%5$s)", name(),
                      description, obsolete ? "obsolete" : "current",
                      reserved ? "reserved" : "unreserved",
                      isOnlyQClass ? "is only QCLASS" : "is not only QCLASS");
    }
}
