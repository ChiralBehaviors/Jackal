package com.softwarecraftsmen.dns;

import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.HostName;
import com.softwarecraftsmen.toString.ToString;
import com.softwarecraftsmen.unsignedIntegers.Unsigned32BitInteger;

public class StatementOfAuthority implements Serializable {
    private final HostName primaryNameServerHostName;
    private final MailBox administratorMailbox;
    private final Unsigned32BitInteger serial;
    private final Seconds referesh;
    private final Seconds retry;
    private final Seconds expire;

    // TODO: Subclass / create MailBox which uses Name
    public StatementOfAuthority(final HostName primaryNameServerHostName,
                                final MailBox administratorMailbox,
                                final Unsigned32BitInteger serial,
                                final Seconds refresh, final Seconds retry,
                                final Seconds expire) {
        this.primaryNameServerHostName = primaryNameServerHostName;
        this.administratorMailbox = administratorMailbox;
        this.serial = serial;
        referesh = refresh;
        this.retry = retry;
        this.expire = expire;
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

        final StatementOfAuthority that = (StatementOfAuthority) o;

        if (!administratorMailbox.equals(that.administratorMailbox)) {
            return false;
        }
        if (!expire.equals(that.expire)) {
            return false;
        }
        if (!primaryNameServerHostName.equals(that.primaryNameServerHostName)) {
            return false;
        }
        if (!referesh.equals(that.referesh)) {
            return false;
        }
        if (!retry.equals(that.retry)) {
            return false;
        }
        if (!serial.equals(that.serial)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = primaryNameServerHostName.hashCode();
        result = 31 * result + administratorMailbox.hashCode();
        result = 31 * result + serial.hashCode();
        result = 31 * result + referesh.hashCode();
        result = 31 * result + retry.hashCode();
        result = 31 * result + expire.hashCode();
        return result;
    }

    public void serialize(final AtomicWriter writer) {
        primaryNameServerHostName.serialize(writer);
        administratorMailbox.serialize(writer);
        writer.writeUnsigned32BitInteger(serial);
        writer.writeUnsignedSeconds(referesh);
        writer.writeUnsignedSeconds(retry);
        writer.writeUnsignedSeconds(expire);
    }

    @Override
    public String toString() {
        return ToString.string(this, primaryNameServerHostName,
                               administratorMailbox, serial, referesh, retry,
                               expire);
    }
}
