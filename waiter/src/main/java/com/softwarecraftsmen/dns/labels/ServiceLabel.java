package com.softwarecraftsmen.dns.labels;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;

public class ServiceLabel implements Label {
    public static class ServiceClassLabelMustBeLessThan15CharactersException
            extends IllegalArgumentException {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public ServiceClassLabelMustBeLessThan15CharactersException() {
            super("A service class value must be less than 14 characters long");
        }
    }

    public static ServiceLabel serviceLabel(final String label) {
        return new ServiceLabel(label);
    }

    private final String value;

    public ServiceLabel(final String value) {
        if (value.startsWith("_")) {
            this.value = value;
            if (value.length() == 1) {
                throw new IllegalArgumentException("label must be more than _");
            }
            if (value.length() > 15) {
                throw new ServiceClassLabelMustBeLessThan15CharactersException();
            }
        } else {
            if (value.length() == 0) {
                throw new IllegalArgumentException(
                                                   "label must have a substantive value");
            }
            if (value.length() > 14) {
                throw new ServiceClassLabelMustBeLessThan15CharactersException();
            }
            this.value = "_" + value;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ServiceLabel that = (ServiceLabel) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    public boolean isEmpty() {
        return false;
    }

    public int length() {
        return value.length();
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeCharacterString(value);
    }

    @Override
    public String toString() {
        return value;
    }

    public String toStringRepresentation() {
        return value;
    }
}
