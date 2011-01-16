package com.softwarecraftsmen.dns.messaging.serializer;

import static com.softwarecraftsmen.dns.NonAsciiAndControlCharactersAreNotSupportedInCharacterStringsException.throwExceptionIfUnsupportedCharacterCode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import com.softwarecraftsmen.CanNeverHappenException;
import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;
import com.softwarecraftsmen.unsignedIntegers.Unsigned32BitInteger;
import com.softwarecraftsmen.unsignedIntegers.Unsigned8BitInteger;

public class AtomicWriter {
    private final ByteArrayOutputStream stream;

    public AtomicWriter(final ByteArrayOutputStream stream) {
        this.stream = stream;
    }

    public byte[] toByteArray() {
        return stream.toByteArray();
    }

    public void writeBytes(final byte[] bytes) {
        try {
            stream.write(bytes);
        } catch (IOException e) {
            throw new CanNeverHappenException(e);
        }
    }

    public void writeCharacter(final char toWrite) {
        throwExceptionIfUnsupportedCharacterCode(toWrite);
        stream.write(toWrite & 0xFF);
    }

    public void writeCharacterString(final char[] characterString) {
        if (characterString.length > 255) {
            throw new IllegalArgumentException(
                                               "Maximum length of a character string in DNS is 255 characters");
        }
        writeUnsigned8BitUnsignedInteger(new Unsigned8BitInteger(
                                                                 characterString.length));
        for (char toWrite : characterString) {
            writeCharacter(toWrite);
        }
    }

    public void writeCharacterString(final String characterString) {
        writeCharacterString(characterString.toCharArray());
    }

    public void writeTimeToLiveInSeconds(final Unsigned32BitInteger timeToLiveInSeconds) {
        writeUnsigned32BitInteger(timeToLiveInSeconds);
    }

    public void writeUnsigned16BitInteger(final Unsigned16BitInteger unsigned16BitInteger) {
        try {
            unsigned16BitInteger.write(stream);
        } catch (IOException e) {
            throw new CanNeverHappenException(e);
        }
    }

    public void writeUnsigned32BitInteger(final Unsigned32BitInteger unsigned32BitInteger) {
        try {
            unsigned32BitInteger.write(stream);
        } catch (IOException e) {
            throw new CanNeverHappenException(e);
        }
    }

    public void writeUnsigned8BitUnsignedInteger(final Unsigned8BitInteger unsigned8BitInteger) {
        try {
            unsigned8BitInteger.write(stream);
        } catch (IOException e) {
            throw new CanNeverHappenException(e);
        }
    }

    public void writeUnsignedSeconds(final Seconds seconds) {
        seconds.serialize(this);
    }
}
