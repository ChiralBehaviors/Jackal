/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.client.resourceRecordRepositories;

import java.util.Set;

import com.softwarecraftsmen.dns.client.resolvers.DnsResolver;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.Name;

public class NonCachingResourceRecordRepository implements
        ResourceRecordRepository {
    private final DnsResolver dnsResolver;

    public NonCachingResourceRecordRepository(final DnsResolver dnsResolver) {
        this.dnsResolver = dnsResolver;
    }

    public <T extends Serializable> Set<T> findData(final Name name,
                                                    final InternetClassType internetClassType) {
        return dnsResolver.resolve(name, internetClassType).allAnswersMatching(internetClassType);
    }
}
