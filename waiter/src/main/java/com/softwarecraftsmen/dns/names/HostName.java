package com.softwarecraftsmen.dns.names;

import static com.softwarecraftsmen.dns.labels.SimpleLabel.labelsFromDottedName;
import static com.softwarecraftsmen.toString.ToString.string;

import java.util.List;

import com.softwarecraftsmen.dns.labels.SimpleLabel;

public class HostName extends AbstractName {
    public static HostName hostName(final String dottedName) {
        return new HostName(labelsFromDottedName(dottedName));
    }

    public HostName(final List<SimpleLabel> labels) {
        super(labels);
    }

    public HostName(final SimpleLabel... labels) {
        super(labels);
    }

    @Override
    public String toString() {
        return string(this, super.toString());
    }
}
