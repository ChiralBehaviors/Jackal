package com.softwarecraftsmen.dns;

import static com.softwarecraftsmen.toString.ToString.string;
import static java.util.Collections.reverse;

import java.util.ArrayList;
import java.util.List;

import com.softwarecraftsmen.dns.labels.SimpleLabel;
import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.unsignedIntegers.Unsigned16BitInteger;

public class MailExchange implements Comparable<MailExchange>, Serializable {
    public static MailExchange mailExchange(final Unsigned16BitInteger preference,
                                            final HostName hostName) {
        return new MailExchange(preference, hostName);
    }

    private final Unsigned16BitInteger preference;

    private final HostName hostName;

    public MailExchange(final Unsigned16BitInteger preference,
                        final HostName hostName) {
        this.preference = preference;
        this.hostName = hostName;
    }

    public int compareTo(final MailExchange that) {
        final int initialPreference = preference.compareTo(that.preference);
        if (initialPreference != 0) {
            return initialPreference;
        }
        final List<SimpleLabel> thisLabels = reverseLabelsInHostName(this);
        final List<SimpleLabel> thatLabels = reverseLabelsInHostName(that);

        if (thisLabels.size() < thatLabels.size()) {
            return -1;
        }
        if (thisLabels.size() > thatLabels.size()) {
            return 1;
        }
        for (int index = 0; index < thisLabels.size(); index++) {
            final int compareTo = thisLabels.get(index).compareTo(thatLabels.get(index));
            if (compareTo != 0) {
                return compareTo;
            }
        }
        return 0;
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

        final MailExchange that = (MailExchange) o;

        if (!hostName.equals(that.hostName)) {
            return false;
        }
        if (!preference.equals(that.preference)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = preference.hashCode();
        result = 31 * result + hostName.hashCode();
        return result;
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeUnsigned16BitInteger(preference);
        hostName.serialize(writer);
    }

    @Override
    public String toString() {
        return string(this, preference, hostName);
    }

    private List<SimpleLabel> reverseLabelsInHostName(final MailExchange mailExchange) {
        return new ArrayList<SimpleLabel>(mailExchange.hostName.toLabels()) {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                reverse(this);
            }
        };
    }
}
