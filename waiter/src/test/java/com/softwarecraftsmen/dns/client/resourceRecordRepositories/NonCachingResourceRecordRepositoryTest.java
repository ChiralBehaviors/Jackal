/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.client.resourceRecordRepositories;

import static com.softwarecraftsmen.dns.Seconds.seconds;
import static com.softwarecraftsmen.dns.SerializableInternetProtocolAddress.serializableInternetProtocolVersion4Address;
import static com.softwarecraftsmen.dns.names.HostName.hostName;
import static com.softwarecraftsmen.dns.resourceRecords.CanonicalNameResourceRecord.canonicalNameResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.InternetProtocolVersion4AddressResourceRecord.internetProtocolVersion4AddressResourceRecord;
import static org.junit.Assert.assertThat;

import java.net.Inet4Address;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.softwarecraftsmen.dns.SerializableInternetProtocolAddress;
import com.softwarecraftsmen.dns.client.resolvers.MockDnsResolver;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.names.HostName;

import static org.hamcrest.Matchers.*;

public class NonCachingResourceRecordRepositoryTest {
    private static final HostName CanonicalName = hostName("www.l.google.com");
    private static final HostName AliasName = hostName("www.google.com");

    private MockDnsResolver dnsResolver;

    private NonCachingResourceRecordRepository nonCachingResourceRecordRepository;

    @Before
    public void before() {
        dnsResolver = new MockDnsResolver();
        nonCachingResourceRecordRepository = new NonCachingResourceRecordRepository(
                                                                                    dnsResolver);
    }

    @Test
    public void canSelectMultipleRecordsOfTheSameTypeFromDifferentTypes() {
        final SerializableInternetProtocolAddress<Inet4Address> address1 = serializableInternetProtocolVersion4Address(1,
                                                                                                                       2,
                                                                                                                       3,
                                                                                                                       4);
        final SerializableInternetProtocolAddress<Inet4Address> address2 = serializableInternetProtocolVersion4Address(2,
                                                                                                                       2,
                                                                                                                       3,
                                                                                                                       4);
        dnsResolver.program(internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          address1));
        dnsResolver.program(internetProtocolVersion4AddressResourceRecord(CanonicalName,
                                                                          seconds(1000),
                                                                          address2));
        dnsResolver.program(canonicalNameResourceRecord(AliasName,
                                                        seconds(1000),
                                                        CanonicalName));
        final Iterable<SerializableInternetProtocolAddress<Inet4Address>> data = nonCachingResourceRecordRepository.findData(AliasName,
                                                                                                                             InternetClassType.A);
        final Matcher<Iterable<SerializableInternetProtocolAddress<Inet4Address>>> iterableMatcher = hasItems(address1,
                                                                                                              address2);
        assertThat(data, iterableMatcher);
    }
}
