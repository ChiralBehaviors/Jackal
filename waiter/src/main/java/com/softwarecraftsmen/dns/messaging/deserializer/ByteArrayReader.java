package com.softwarecraftsmen.dns.messaging.deserializer;

import static com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger.unsigned16BitInteger;

import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;
import com.softwarecraftsmen.unsignedIntegers.Unsigned32BitInteger;
import com.softwarecraftsmen.unsignedIntegers.Unsigned8BitInteger;

public class ByteArrayReader {
    private final byte[] bytes;
    private int currentPosition;
    private final int count;

    public ByteArrayReader(final byte[] bytes) {
        this.bytes = bytes;
        currentPosition = 0;
        count = bytes.length;
    }

    public int currentPosition() {
        return currentPosition;
    }

    public void moveToOffset(final int offset) {
        currentPosition = offset;
    }

    public String readAsciiString(final Unsigned16BitInteger numberOfCharacters) {
        return readAsciiString(numberOfCharacters.createCharacterArray());
    }

    public String readAsciiString(final Unsigned8BitInteger numberOfCharacters) {
        return readAsciiString(numberOfCharacters.createCharacterArray());
    }

    public char readByteAsAsciiCharacter() {
        return readUnsigned8BitInteger().toAsciiCharacter();
    }

    public byte[] readRawByteArray(final Unsigned16BitInteger numberOfBytes) {
        final byte[] byteArray = numberOfBytes.createByteArray();
        for (int byteIndex = 0; byteIndex < byteArray.length; byteIndex++) {
            byteArray[byteIndex] = rawRead();
        }
        return byteArray;
    }

    public Unsigned16BitInteger readUnsigned16BitInteger() {
        return unsigned16BitInteger((read() << 8) + read());
    }

    public Unsigned32BitInteger readUnsigned32BitInteger() {
        return readUnsigned16BitInteger().leftShift16().add(readUnsigned16BitInteger());
    }

    public Unsigned8BitInteger readUnsigned8BitInteger() {
        return new Unsigned8BitInteger(read());
    }

    private byte rawRead() {
        return bytes[currentPosition++];
    }

    private int read() {
        if (currentPosition >= count) {
            throw new IllegalStateException(
                                            "You've read beyond the end of the byte array");
        }
        return rawRead() & 0xFF;
    }

    private String readAsciiString(final char[] asciiCharacters) {
        for (int characterIndex = 0; characterIndex < asciiCharacters.length; characterIndex++) {
            asciiCharacters[characterIndex] = readByteAsAsciiCharacter();
        }
        return String.valueOf(asciiCharacters);
    }
}
