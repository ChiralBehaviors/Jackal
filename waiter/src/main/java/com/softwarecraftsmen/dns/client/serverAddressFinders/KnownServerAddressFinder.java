package com.softwarecraftsmen.dns.client.serverAddressFinders;

import static java.util.Collections.unmodifiableList;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import com.softwarecraftsmen.dns.SerializableInternetProtocolAddress;

public class KnownServerAddressFinder implements ServerAddressFinder {
    private List<InetSocketAddress> inetSocketAddresses;

    public KnownServerAddressFinder(final SerializableInternetProtocolAddress... addresses) {
        final ArrayList<InetSocketAddress> knownAddresses = new ArrayList<InetSocketAddress>();
        for (SerializableInternetProtocolAddress address : addresses) {
            knownAddresses.add(new InetSocketAddress(address.address,
                                                     StandardUnicastDnsServerPort));
        }
        inetSocketAddresses = unmodifiableList(knownAddresses);
    }

    public List<InetSocketAddress> find() {
        return inetSocketAddresses;
    }
}
