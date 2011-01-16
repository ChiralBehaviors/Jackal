package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.messaging.InternetClassType.A;
import static com.softwarecraftsmen.dns.messaging.QClass.Internet;

import java.net.Inet4Address;

import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.SerializableInternetProtocolAddress;
import com.softwarecraftsmen.dns.names.HostName;

public class InternetProtocolVersion4AddressResourceRecord
        extends
        AbstractResourceRecord<HostName, SerializableInternetProtocolAddress<Inet4Address>> {
    public static InternetProtocolVersion4AddressResourceRecord internetProtocolVersion4AddressResourceRecord(final HostName owner,
                                                                                                              final Seconds timeToLive,
                                                                                                              final SerializableInternetProtocolAddress<Inet4Address> internetProtocolVersion4Address) {
        return new InternetProtocolVersion4AddressResourceRecord(owner,
                                                                 timeToLive,
                                                                 internetProtocolVersion4Address);
    }

    public InternetProtocolVersion4AddressResourceRecord(final HostName owner,
                                                         final Seconds timeToLive,
                                                         final SerializableInternetProtocolAddress<Inet4Address> internetProtocolVersion4Address) {
        super(owner, A, Internet, timeToLive, internetProtocolVersion4Address);
    }
}
