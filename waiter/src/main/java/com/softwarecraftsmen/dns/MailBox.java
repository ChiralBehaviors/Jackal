package com.softwarecraftsmen.dns;

import static com.softwarecraftsmen.dns.labels.SimpleLabel.simpleLabel;
import static java.lang.String.format;
import static java.util.Locale.UK;

import java.util.ArrayList;
import java.util.List;

import com.softwarecraftsmen.dns.labels.Label;
import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.names.DomainName;
import com.softwarecraftsmen.dns.names.Name;

public class MailBox implements Name {
    public static MailBox mailBox(final String userName,
                                  final DomainName domainName) {
        return new MailBox(userName, domainName);
    }

    private final String userName;

    private final DomainName domainName;

    public MailBox(final String userName, final DomainName domainName) {
        this.userName = userName;
        this.domainName = domainName;
        if (userName.length() > 63) {
            throw new IllegalArgumentException(
                                               "An userName must be less than 64 characters in length");
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

        final MailBox mailBox = (MailBox) o;
        return domainName.equals(mailBox.domainName)
               && userName.equals(mailBox.userName);
    }

    @Override
    public int hashCode() {
        int result;
        result = userName.hashCode();
        result = 31 * result + domainName.hashCode();
        return result;
    }

    public void serialize(final AtomicWriter writer) {
        writer.writeCharacterString(userName);
        domainName.serialize(writer);
    }

    public List<Label> toLabels() {
        return new ArrayList<Label>() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            {
                add(simpleLabel(userName));
                addAll(domainName.toLabels());
            }
        };
    }

    @Override
    public String toString() {
        return format(UK, "%1$s@%2$s", userName, domainName);
    }
}
