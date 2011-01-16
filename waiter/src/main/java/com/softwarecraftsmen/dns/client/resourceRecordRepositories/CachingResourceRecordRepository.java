/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.client.resourceRecordRepositories;

import static com.softwarecraftsmen.dns.Seconds.currentTime;
import static com.softwarecraftsmen.dns.messaging.Message.allAnswersMatching;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.softwarecraftsmen.Pair;
import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.client.resolvers.DnsResolver;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.Name;
import com.softwarecraftsmen.dns.resourceRecords.ResourceRecord;

public class CachingResourceRecordRepository implements
        ResourceRecordRepository {
    private final DnsResolver dnsResolver;
    private final Seconds maximumTimeToLivePermitted;
    private final Map<Pair<Name, InternetClassType>, Set<ResourceRecord<? extends Name, ? extends Serializable>>> cache;
    private final SortedMap<Seconds, Set<ResourceRecord<? extends Name, ? extends Serializable>>> bestBeforeTimesForResourceRecords;

    public CachingResourceRecordRepository(final DnsResolver dnsResolver,
                                           final Seconds maximumTimeToLivePermitted) {
        this.dnsResolver = dnsResolver;
        this.maximumTimeToLivePermitted = maximumTimeToLivePermitted;
        cache = new HashMap<Pair<Name, InternetClassType>, Set<ResourceRecord<? extends Name, ? extends Serializable>>>();
        bestBeforeTimesForResourceRecords = new TreeMap<Seconds, Set<ResourceRecord<? extends Name, ? extends Serializable>>>();
    }

    public <T extends Serializable> Set<T> findData(final Name name,
                                                    final InternetClassType internetClassType) {
        removeStaleEntries();

        final Pair<Name, InternetClassType> key = new Pair<Name, InternetClassType>(
                                                                                    name,
                                                                                    internetClassType);
        if (!cache.containsKey(key)) {
            cache.put(key,
                      new LinkedHashSet<ResourceRecord<? extends Name, ? extends Serializable>>());
        }
        Set<ResourceRecord<? extends Name, ? extends Serializable>> resourceRecords = cache.get(key);
        if (resourceRecords.isEmpty()) {
            resourceRecords = resolveAndCache(name, internetClassType);
        }

        return allAnswersMatching(resourceRecords, internetClassType);
    }

    private void removeStaleEntries() {
        final SortedMap<Seconds, Set<ResourceRecord<? extends Name, ? extends Serializable>>> map = bestBeforeTimesForResourceRecords.headMap(currentTime());
        for (Seconds key : map.keySet()) {
            final Set<ResourceRecord<? extends Name, ? extends Serializable>> resourceRecords = map.get(key);
            for (ResourceRecord<? extends Name, ? extends Serializable> resourceRecord : resourceRecords) {
                resourceRecord.removeFromCache(cache);
            }
            map.remove(key);
        }
    }

    private Set<ResourceRecord<? extends Name, ? extends Serializable>> resolveAndCache(final Name name,
                                                                                        final InternetClassType internetClassType) {
        final Set<ResourceRecord<? extends Name, ? extends Serializable>> resourceRecords = dnsResolver.resolve(name,
                                                                                                                internetClassType).allResourceRecords();
        for (ResourceRecord<? extends Name, ? extends Serializable> resourceRecord : resourceRecords) {
            // TODO: Only code that needs to be clever is Client IpAddress finding code.
            // BETTER: do a findCanonicalName from insider find Ip address, if result, do query with that else do query with original name
            resourceRecord.addToCache(maximumTimeToLivePermitted,
                                      bestBeforeTimesForResourceRecords, cache);
        }
        return resourceRecords;
    }
}
