/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.client.resourceRecordRepositories;

import static com.softwarecraftsmen.dns.Seconds.seconds;
import static com.softwarecraftsmen.dns.SerializableInternetProtocolAddress.serializableInternetProtocolVersion4Address;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.A;
import static com.softwarecraftsmen.dns.messaging.InternetClassType.CNAME;
import static com.softwarecraftsmen.dns.names.HostName.hostName;
import static com.softwarecraftsmen.dns.resourceRecords.CanonicalNameResourceRecord.canonicalNameResourceRecord;
import static com.softwarecraftsmen.dns.resourceRecords.InternetProtocolVersion4AddressResourceRecord.internetProtocolVersion4AddressResourceRecord;
import static java.lang.Thread.sleep;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.Inet4Address;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.softwarecraftsmen.dns.SerializableInternetProtocolAddress;
import com.softwarecraftsmen.dns.client.resolvers.MockDnsResolver;
import com.softwarecraftsmen.dns.names.HostName;

import static org.hamcrest.Matchers.*;

public class CachingResourceRecordRepositoryTest {
    private static final HostName CanonicalName = hostName("www.l.google.com");
    private static final HostName AliasName = hostName("www.google.com");

    private MockDnsResolver dnsResolver;

    private CachingResourceRecordRepository cachingResourceRecordRepository;

    @Before
    public void before() {
        dnsResolver = new MockDnsResolver();
        cachingResourceRecordRepository = new CachingResourceRecordRepository(
                                                                              dnsResolver,
                                                                              seconds(1));
    }

    @Test
    public void cacheExpiryAfterMaximumTimeToLiveResultsInASecondCall()
                                                                       throws InterruptedException {
        canSelectMultipleRecordsOfTheSameTypeFromDifferentTypes();
        dnsResolver.assertResolveCalledOnceOnly();

        final int OneAndAHalfSeconds = 1500;
        sleep(OneAndAHalfSeconds);

        final Set<SerializableInternetProtocolAddress<Inet4Address>> secondCall = cachingResourceRecordRepository.findData(AliasName,
                                                                                                                           A);
        assertThat(secondCall.size(), is(equalTo(2)));
        dnsResolver.assertResolveCalledTwice();
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
                                                                          seconds(1001),
                                                                          address2));
        dnsResolver.program(canonicalNameResourceRecord(AliasName,
                                                        seconds(1002),
                                                        CanonicalName));
        final Iterable<SerializableInternetProtocolAddress<Inet4Address>> data = cachingResourceRecordRepository.findData(AliasName,
                                                                                                                          A);
        final Matcher<Iterable<SerializableInternetProtocolAddress<Inet4Address>>> iterableMatcher = hasItems(address1,
                                                                                                              address2);
        assertThat(data, iterableMatcher);
    }

    // TODO: Ignores SOA responses and negative caching.
    @Test
    public void nonExistentResultIsAlwaysRequeried() {
        final Set<SerializableInternetProtocolAddress<Inet4Address>> firstCall = cachingResourceRecordRepository.findData(AliasName,
                                                                                                                          A);
        assertThat(firstCall.size(), is(equalTo(0)));
        dnsResolver.assertResolveCalledOnceOnly();

        final Set<SerializableInternetProtocolAddress<Inet4Address>> secondCall = cachingResourceRecordRepository.findData(AliasName,
                                                                                                                           A);
        assertThat(secondCall.size(), is(equalTo(0)));
        dnsResolver.assertResolveCalledTwice();
    }

    @Test
    public void secondSelectionUsesCacheAndNotDnsResolverButLooksAtAliasRecordsInCache() {
        cachingResourceRecordRepository = new CachingResourceRecordRepository(
                                                                              dnsResolver,
                                                                              seconds(5000));
        canSelectMultipleRecordsOfTheSameTypeFromDifferentTypes();
        dnsResolver.assertResolveCalledOnceOnly();

        cachingResourceRecordRepository.findData(AliasName, CNAME);
        dnsResolver.assertResolveCalledOnceOnly();
    }

    @Test
    public void secondSelectionUsesCacheAndNotDnsResolverButLooksAtCanonicalRecordsInCache() {
        cachingResourceRecordRepository = new CachingResourceRecordRepository(
                                                                              dnsResolver,
                                                                              seconds(5000));
        canSelectMultipleRecordsOfTheSameTypeFromDifferentTypes();
        dnsResolver.assertResolveCalledOnceOnly();

        cachingResourceRecordRepository.findData(CanonicalName, A);
        dnsResolver.assertResolveCalledOnceOnly();
    }
}