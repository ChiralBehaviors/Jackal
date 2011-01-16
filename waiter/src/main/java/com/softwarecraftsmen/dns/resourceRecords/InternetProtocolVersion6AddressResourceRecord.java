package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.AAAA;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import java.net.Inet6Address;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.SerializableInternetProtocolAddress;
import com.softwarecraftsmen.dns.names.HostName;

public class InternetProtocolVersion6AddressResourceRecord
        extends
        AbstractResourceRecord<HostName, SerializableInternetProtocolAddress<Inet6Address>> {
    public static InternetProtocolVersion6AddressResourceRecord internetProtocolVersion6AddressResourceRecord(final HostName owner,
                                                                                                              final Seconds timeToLive,
                                                                                                              final SerializableInternetProtocolAddress<Inet6Address> internetProtocolVersion6Address) {
        return new InternetProtocolVersion6AddressResourceRecord(owner,
                                                                 timeToLive,
                                                                 internetProtocolVersion6Address);
    }

    public InternetProtocolVersion6AddressResourceRecord(final HostName owner,
                                                         final Seconds timeToLive,
                                                         final SerializableInternetProtocolAddress<Inet6Address> internetProtocolVersion6Address) {
        super(owner, AAAA, Internet, timeToLive,
              internetProtocolVersion6Address);
    }
}
