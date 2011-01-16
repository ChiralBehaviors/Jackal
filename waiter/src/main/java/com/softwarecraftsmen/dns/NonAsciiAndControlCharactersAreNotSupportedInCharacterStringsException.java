package com.softwarecraftsmen.dns;

import static java.lang.Character.isISOControl;
import static java.lang.String.format;
import static java.util.Locale.UK;

public final class NonAsciiAndControlCharactersAreNotSupportedInCharacterStringsException
        extends IllegalArgumentException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static void throwExceptionIfUnsupportedCharacterCode(final char toWrite) {
        if (isISOControl(toWrite) || toWrite > 255) {
            throw new NonAsciiAndControlCharactersAreNotSupportedInCharacterStringsException(
                                                                                             toWrite);
        }
    }

    public NonAsciiAndControlCharactersAreNotSupportedInCharacterStringsException(final char nonAsciiCharacter) {
        super(
              format(UK,
                     "Non ASCII characters, such as %1$s, are not supported in DNS names",
                     nonAsciiCharacter));
    }
}
