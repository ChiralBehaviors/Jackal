package com.softwarecraftsmen.dns.messaging.deserializer;

import static com.softwarecraftsmen.dns.labels.SimpleLabel.simpleLabel;
import static com.softwarecraftsmen.unsignedIntegers.Unsigned8BitInteger.Zero;

import java.util.List;

import com.softwarecraftsmen.dns.labels.SimpleLabel;
import com.softwarecraftsmen.unsignedIntegers.Unsigned8BitInteger;

public class LabelsReader {
    private final ByteArrayReader reader;
    private static final Unsigned8BitInteger TerminalLabel = Zero;
    private static final Unsigned8BitInteger CompressedNameMask = new Unsigned8BitInteger(
                                                                                          (1 << 7)
                                                                                                  + (1 << 6));
    private static final Unsigned8BitInteger InverseCompressedNameMask = CompressedNameMask.not();

    public LabelsReader(final ByteArrayReader reader) {
        this.reader = reader;
    }

    public void readLabels(final List<SimpleLabel> appendTo) {
        boolean continueReadingLabels = true;
        while (continueReadingLabels) {
            continueReadingLabels = readLabelOrJumpToLabels(appendTo);
        }
    }

    private boolean isCompressedName(final Unsigned8BitInteger numberOfCharacters) {
        return numberOfCharacters.and(CompressedNameMask).equals(CompressedNameMask);
    }

    private SimpleLabel readLabel(final Unsigned8BitInteger potentialNumberOfCharacters) {
        return simpleLabel(reader.readAsciiString(potentialNumberOfCharacters));
    }

    private boolean readLabelOrJumpToLabels(final List<SimpleLabel> appendTo) {
        final Unsigned8BitInteger potentialNumberOfCharacters = reader.readUnsigned8BitInteger();
        if (isCompressedName(potentialNumberOfCharacters)) {
            final Unsigned8BitInteger upperOffset = potentialNumberOfCharacters.and(InverseCompressedNameMask);
            final Unsigned8BitInteger lowerOffset = reader.readUnsigned8BitInteger();
            final int offset = upperOffset.shiftToSigned32BitInteger(lowerOffset);
            final int currentPosition = reader.currentPosition();
            reader.moveToOffset(offset);
            readLabels(appendTo);
            reader.moveToOffset(currentPosition);
            return false;
        } else if (potentialNumberOfCharacters.equals(TerminalLabel)) {
            return false;
        } else {
            appendTo.add(readLabel(potentialNumberOfCharacters));
            return true;
        }
    }
}
