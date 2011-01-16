package com.softwarecraftsmen.dns.names;

import static com.softwarecraftsmen.dns.labels.SimpleLabel.labelsFromDottedName;
import static com.softwarecraftsmen.toString.ToString.string;

import java.util.List;

import com.softwarecraftsmen.dns.labels.SimpleLabel;

public class DomainName extends AbstractName {
    public static DomainName domainName(final String dottedName) {
        return new DomainName(labelsFromDottedName(dottedName));
    }

    public DomainName(final List<SimpleLabel> labels) {
        super(labels);
    }

    public DomainName(final SimpleLabel... labels) {
        super(labels);
    }

    @Override
    public String toString() {
        return string(this, super.toString());
    }
}
