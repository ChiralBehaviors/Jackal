package com.softwarecraftsmen.dns.labels;

import static java.lang.String.format;
import static java.util.Locale.UK;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;

public enum ServiceProtocolLabel implements Label {
    TCP("_tcp"), UDP("_udp");
    public static ServiceProtocolLabel toServiceProtocolLabel(final String value) {
        final String searchValue = value.charAt(0) == '_' ? value : "_" + value;
        final ServiceProtocolLabel[] serviceProtocolLabels = values();
        for (ServiceProtocolLabel serviceProtocolLabel : serviceProtocolLabels) {
            if (serviceProtocolLabel.value.equals(searchValue)) {
                return serviceProtocolLabel;
            }
        }
        throw new IllegalArgumentException(
                                           format(UK,
                                                  "The value %1$s is not a valid ServiceProtocolLabel",
                                                  value));
    }

    private final String value;

    private ServiceProtocolLabel(final String value) {
        this.value = value;
    }

    public boolean isEmpty() {
        return true;
    }

    public int length() {
        return value.length();
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeCharacterString(value);
    }

    public String toStringRepresentation() {
        return value;
    }
}
