/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.unsignedIntegers.Unsigned4BitInteger.One;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned4BitInteger.Two;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned4BitInteger.Zero;
import static java.lang.String.format;
import static java.util.Locale.UK;

import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;
import com.softwarecraftsmen.unsignedIntegers.Unsigned4BitInteger;

public enum OperationCode {
    Query(Zero), InverseQuery(One), Status(Two);

    public static OperationCode operationCode(final Unsigned4BitInteger unsigned4BitInteger) {
        for (OperationCode operationCode : values()) {
            if (operationCode.unsigned4BitInteger.equals(unsigned4BitInteger)) {
                return operationCode;
            }
        }
        throw new IllegalArgumentException(
                                           format(UK,
                                                  "No OperationCode known for %1$s",
                                                  unsigned4BitInteger));
    }

    private final Unsigned4BitInteger unsigned4BitInteger;

    private OperationCode(final Unsigned4BitInteger unsigned4BitInteger) {
        this.unsigned4BitInteger = unsigned4BitInteger;
    }

    public Unsigned16BitInteger set4Bits(final Unsigned16BitInteger unsigned16BitInteger) {
        return unsigned16BitInteger.set4BitsIetf(unsigned4BitInteger, 1);
    }
}
