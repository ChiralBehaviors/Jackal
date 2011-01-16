package com.softwarecraftsmen.dns.names;

import static com.softwarecraftsmen.toString.ToString.string;

import java.util.ArrayList;
import java.util.List;

import com.softwarecraftsmen.dns.labels.Label;
import com.softwarecraftsmen.dns.labels.ServiceLabel;
import com.softwarecraftsmen.dns.labels.ServiceProtocolLabel;
import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;

public class ServiceName implements Name {
    public static ServiceName serviceName(final ServiceLabel serviceLabel,
                                          final ServiceProtocolLabel serviceProtocolLabel,
                                          final DomainName domainName) {
        return new ServiceName(serviceLabel, serviceProtocolLabel, domainName);
    }

    private final ServiceLabel serviceLabel;
    private final ServiceProtocolLabel serviceProtocolLabel;

    private final DomainName domainName;

    public ServiceName(final ServiceLabel serviceLabel,
                       final ServiceProtocolLabel serviceProtocolLabel,
                       final DomainName domainName) {
        this.serviceLabel = serviceLabel;
        this.serviceProtocolLabel = serviceProtocolLabel;
        this.domainName = domainName;
    }

    @Override
    @SuppressWarnings({ "RedundantIfStatement" })
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ServiceName that = (ServiceName) o;

        if (!domainName.equals(that.domainName)) {
            return false;
        }
        if (!serviceLabel.equals(that.serviceLabel)) {
            return false;
        }
        if (!serviceProtocolLabel.equals(that.serviceProtocolLabel)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = serviceLabel.hashCode();
        result = 31 * result + serviceProtocolLabel.hashCode();
        result = 31 * result + domainName.hashCode();
        return result;
    }

    public void serialize(final AtomicWriter writer) {
        serviceLabel.serialize(writer);
        serviceProtocolLabel.serialize(writer);
        domainName.serialize(writer);
    }

    public List<Label> toLabels() {
        return new ArrayList<Label>() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                add(serviceLabel);
                add(serviceProtocolLabel);
                addAll(domainName.toLabels());
            }
        };
    }

    @Override
    public String toString() {
        return string(this, serviceLabel, serviceProtocolLabel, domainName);
    }
}
