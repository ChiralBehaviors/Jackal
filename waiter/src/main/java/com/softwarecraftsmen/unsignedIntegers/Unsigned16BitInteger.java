package com.softwarecraftsmen.unsignedIntegers;

import static com.softwarecraftsmen.unsignedIntegers.Unsigned3BitInteger.unsigned3BitInteger;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned4BitInteger.unsigned4BitInteger;
import static java.lang.Character.MAX_VALUE;
import static java.lang.String.format;
import static java.util.Locale.UK;

import java.io.IOException;
import java.io.OutputStream;

public class Unsigned16BitInteger implements Comparable<Unsigned16BitInteger> {
    private final int value;

    public static final Unsigned16BitInteger Zero = new Unsigned16BitInteger(0);

    public static final Unsigned16BitInteger One = new Unsigned16BitInteger(1);

    public static Unsigned16BitInteger Four = new Unsigned16BitInteger(4);

    public static Unsigned16BitInteger Sixteen = new Unsigned16BitInteger(16);

    public static Unsigned16BitInteger MaximumValue = new Unsigned16BitInteger(
                                                                               MAX_VALUE);

    private static final int TopBit = 15;

    public static Unsigned16BitInteger unsigned16BitInteger(final int value) {
        switch (value) {
            case 0:
                return Zero;
            case 1:
                return One;
            case 4:
                return Four;
            case 16:
                return Sixteen;
            default:
                return new Unsigned16BitInteger(value);
        }
    }

    private Unsigned16BitInteger(final int value) {
        if (value < 0 || value > 65536) {
            throw new IllegalArgumentException(
                                               format(UK,
                                                      "The value %1$s is not a valid unsigned 16 bit integer",
                                                      value));
        }
        this.value = value;
    }

    public int compareTo(final Unsigned16BitInteger that) {
        if (value < that.value) {
            return -1;
        }
        if (value > that.value) {
            return 1;
        }
        return 0;
    }

    public byte[] createByteArray() {
        return new byte[value];
    }

    public char[] createCharacterArray() {
        return new char[value];
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Unsigned16BitInteger that = (Unsigned16BitInteger) o;
        return value == that.value;
    }

    public boolean getBitIetf(final int zeroBasedIetfBitNumber) {
        return getBitPowerOfTwo(TopBit - zeroBasedIetfBitNumber);
    }

    public Unsigned3BitInteger getThreeBitsIetf(final int zeroBasedIetfBitNumberStart) {
        return unsigned3BitInteger(getBitsIetf(zeroBasedIetfBitNumberStart,
                                               0x07));
    }

    public Unsigned4BitInteger getUnsigned4BitIntegerIetf(final int zeroBasedIetfBitNumberStart) {
        return unsigned4BitInteger(getBitsIetf(zeroBasedIetfBitNumberStart,
                                               0x0F));
    }

    @Override
    public int hashCode() {
        return value;
    }

    public Unsigned16BitInteger increment() {
        if (value == MAX_VALUE) {
            return Zero;
        }
        return unsigned16BitInteger(value + 1);
    }

    public Unsigned32BitInteger leftShift16() {
        return new Unsigned32BitInteger(value << 16);
    }

    public Unsigned16BitInteger set4BitsIetf(final Unsigned4BitInteger unsigned4BitInteger,
                                             final int zeroBasedIetfBitNumberStart) {
        return set4BitsPowerOfTwo(unsigned4BitInteger,
                                  TopBit - zeroBasedIetfBitNumberStart);
    }

    public Unsigned16BitInteger setBitIetf(final boolean bitOnOrOff,
                                           final int zeroBasedIetfBitNumber) {
        return setBitPowerOfTwo(bitOnOrOff, TopBit - zeroBasedIetfBitNumber);
    }

    public Unsigned16BitInteger setBitPowerOfTwo(final boolean bitOnOrOff,
                                                 final int zeroBasedPowerOfTwoBitNumber) {
        return unsigned16BitInteger(value
                                    | (bitOnOrOff ? 1 << zeroBasedPowerOfTwoBitNumber
                                                 : 0));
    }

    public long toLong() {
        return value;
    }

    public int toSigned32BitInteger() {
        return value;
    }

    @Override
    public String toString() {
        return format(UK, "%1$s", value);
    }

    public void write(final OutputStream stream) throws IOException {
        stream.write(value >>> 8 & 0xFF);
        stream.write(value & 0xFF);
    }

    private boolean getBitPowerOfTwo(final int zeroBasedPowerOfTwoBitNumber) {
        final int mask = 1 << zeroBasedPowerOfTwoBitNumber;
        return (value & mask) == mask;
    }

    private int getBitsIetf(final int zeroBasedIetfBitNumberStart,
                            final int mask) {
        return getBitsPowerOfTwo(TopBit - zeroBasedIetfBitNumberStart, mask);
    }

    private int getBitsPowerOfTwo(final int zeroBasedPowerOfTwoBitNumberStart,
                                  final int mask) {
        final int mask2 = mask << zeroBasedPowerOfTwoBitNumberStart;
        final int value2 = value & mask2;
        return value2 >> zeroBasedPowerOfTwoBitNumberStart;
    }

    private int set4Bits(final Unsigned4BitInteger unsigned4BitInteger,
                         final int zeroBasedPowerOfTwoBitNumber) {
        return value
               | unsigned4BitInteger.toUnsigned16BitInteger().value << zeroBasedPowerOfTwoBitNumber;
    }

    private Unsigned16BitInteger set4BitsPowerOfTwo(final Unsigned4BitInteger unsigned4BitInteger,
                                                    final int zeroBasedPowerOfTwoBitNumber) {
        return unsigned16BitInteger(set4Bits(unsigned4BitInteger,
                                             zeroBasedPowerOfTwoBitNumber));
    }
}
