/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.labels;

import static com.softwarecraftsmen.dns.NonAsciiAndControlCharactersAreNotSupportedInCharacterStringsException.throwExceptionIfUnsupportedCharacterCode;
import static com.softwarecraftsmen.dns.labels.ServiceLabel.serviceLabel;
import static java.lang.String.format;
import static java.util.Locale.UK;

import java.util.ArrayList;
import java.util.List;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;

public class SimpleLabel implements Label, Comparable<SimpleLabel> {

    public static final class LabelsCanNotBeLongerThan63CharactersException
            extends IllegalArgumentException {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public LabelsCanNotBeLongerThan63CharactersException(final String label) {
            super(
                  format(UK,
                         "Labels (the strings between dots in a DNS name) can not be longer than 63 character. This label, %1$s, is.",
                         label));
        }
    }

    public final class LabelsCanNotContainPeriodsException extends
            IllegalArgumentException {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public LabelsCanNotContainPeriodsException(final String value) {
            super(
                  format(UK,
                         "Labels (the strings between dots in a DNS name) can not contain the period character. This label, %1$s, does.",
                         value));
        }

        public void throwExceptionIfCharacterIsAPeriod(final char toWrite) {
            if (toWrite == '.') {
                throw new LabelsCanNotContainPeriodsException(value);
            }
        }
    }

    public static final SimpleLabel Empty = new SimpleLabel("");

    public static List<SimpleLabel> labelsFromDottedName(final String dottedName) {
        final String[] values = dottedName.split("\\.");
        return new ArrayList<SimpleLabel>() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                for (String value : values) {
                    add(simpleLabel(value));
                }
            }
        };
    }

    public static SimpleLabel simpleLabel(final String value) {
        if (value.length() == 0) {
            return Empty;
        }
        return new SimpleLabel(value);
    }

    private final String value;

    private SimpleLabel(final String value) {
        if (value.length() > 63) {
            throw new LabelsCanNotBeLongerThan63CharactersException(value);
        }
        for (char toWrite : value.toCharArray()) {
            throwExceptionIfUnsupportedCharacterCode(toWrite);
            if (toWrite == '.') {
                throw new LabelsCanNotContainPeriodsException(value);
            }
        }
        this.value = value;
    }

    public int compareTo(final SimpleLabel that) {
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

        final SimpleLabel that = (SimpleLabel) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public boolean isEmpty() {
        return value.length() == 0;
    }

    public int length() {
        return value.length();
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeCharacterString(value);
    }

    public ServiceLabel toServiceLabel() {
        return serviceLabel(value);
    }

    public ServiceProtocolLabel toServiceProtocolLabel() {
        try {
            return ServiceProtocolLabel.toServiceProtocolLabel(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return value;
    }

    public String toStringRepresentation() {
        return value;
    }
}
