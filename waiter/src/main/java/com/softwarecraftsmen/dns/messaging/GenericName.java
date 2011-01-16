package com.softwarecraftsmen.dns.messaging;

import static com.softwarecraftsmen.dns.labels.SimpleLabel.labelsFromDottedName;
import static com.softwarecraftsmen.toString.ToString.string;
import static java.lang.String.format;
import static java.util.Locale.UK;

import java.util.ArrayList;
import java.util.List;

import com.softwarecraftsmen.dns.labels.Label;
import com.softwarecraftsmen.dns.labels.ServiceLabel;
import com.softwarecraftsmen.dns.labels.ServiceProtocolLabel;
import com.softwarecraftsmen.dns.labels.SimpleLabel;
import com.softwarecraftsmen.dns.messaging.deserializer.BadlyFormedDnsMessageException;
import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.names.DomainName;
import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.dns.names.Name;
import com.softwarecraftsmen.dns.names.PointerName;
import com.softwarecraftsmen.dns.names.ServiceName;

public class GenericName implements Name {
    public static GenericName genericName(final String dottedName) {
        return new GenericName(labelsFromDottedName(dottedName));
    }

    private final List<SimpleLabel> labels;

    public GenericName(final List<SimpleLabel> labels) {
        this.labels = labels;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final GenericName that = (GenericName) o;
        return labels.equals(that.labels);
    }

    @Override
    public int hashCode() {
        return labels.hashCode();
    }

    // TODO: Generic name serialization...
    public void serialize(final AtomicWriter writer) {
        throw new UnsupportedOperationException("Write some code!");
    }

    public DomainName toDomainName() {
        return new DomainName(labels);
    }

    public HostName toHostName() {
        return new HostName(labels);
    }

    public List<Label> toLabels() {
        return new ArrayList<Label>(labels);
    }

    public PointerName toPointerName() {
        return new PointerName(labels);
    }

    public ServiceName toServiceName() throws BadlyFormedDnsMessageException {
        final ServiceLabel serviceLabel;
        try {
            serviceLabel = labels.get(0).toServiceLabel();
        } catch (IndexOutOfBoundsException exception) {
            throw new BadlyFormedDnsMessageException(
                                                     "There must be at least a service class label in a service name",
                                                     exception);
        }
        final ServiceProtocolLabel serviceProtocolLabel;
        try {
            serviceProtocolLabel = labels.get(1).toServiceProtocolLabel();
        } catch (IndexOutOfBoundsException exception) {
            throw new BadlyFormedDnsMessageException(
                                                     "There must be at least a service protocol label in a service name",
                                                     exception);
        } catch (IllegalArgumentException exception) {
            throw new BadlyFormedDnsMessageException(
                                                     format(UK,
                                                            "The service protocol label %1$s was unrecognised",
                                                            labels.get(1)),
                                                     exception);
        }

        final DomainName domainName;
        try {
            domainName = new DomainName(labels.subList(2, labels.size()));
        } catch (IndexOutOfBoundsException exception) {
            throw new BadlyFormedDnsMessageException(
                                                     "There must be at least one domain label in a service name",
                                                     exception);
        }
        return new ServiceName(serviceLabel, serviceProtocolLabel, domainName);
    }

    @Override
    public String toString() {
        return string(this, labels);
    }
}
