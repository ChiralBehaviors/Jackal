package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.NS;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.names.DomainName;
import com.softwarecraftsmen.dns.names.HostName;

public class NameServerResourceRecord extends
        AbstractResourceRecord<DomainName, HostName> {
    public static NameServerResourceRecord nameServerResourceRecord(final DomainName owner,
                                                                    final Seconds timeToLive,
                                                                    final HostName nameServerHostName) {
        return new NameServerResourceRecord(owner, timeToLive,
                                            nameServerHostName);
    }

    public NameServerResourceRecord(final DomainName owner,
                                    final Seconds timeToLive,
                                    final HostName nameServerHostName) {
        super(owner, NS, Internet, timeToLive, nameServerHostName);
    }
}
