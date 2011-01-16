package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.CNAME;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.names.HostName;

public class CanonicalNameResourceRecord extends
        AbstractResourceRecord<HostName, HostName> {
    public static CanonicalNameResourceRecord canonicalNameResourceRecord(final HostName alias,
                                                                          final Seconds timeToLive,
                                                                          final HostName canonicalName) {
        return new CanonicalNameResourceRecord(alias, timeToLive, canonicalName);
    }

    public CanonicalNameResourceRecord(final HostName alias,
                                       final Seconds timeToLive,
                                       final HostName canonicalName) {
        super(alias, CNAME, Internet, timeToLive, canonicalName);
    }
}
