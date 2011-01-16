/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.client.serverAddressFinders;

import java.net.InetSocketAddress;
import java.util.List;

public interface ServerAddressFinder {
    public static final int StandardUnicastDnsServerPort = 53;
    public static final int StandardMulticastDnsServerPort = 53;

    List<InetSocketAddress> find();
}
