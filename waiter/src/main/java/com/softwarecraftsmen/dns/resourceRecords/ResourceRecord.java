/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.resourceRecords;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.softwarecraftsmen.Pair;
import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.Name;

public interface ResourceRecord<S extends Name, T extends Serializable> extends
        Serializable {
    void addToCache(final Seconds maximumTimeToLivePermitted,
                    final SortedMap<Seconds, Set<ResourceRecord<? extends Name, ? extends Serializable>>> bestBeforeTimesForResourceRecords,
                    final Map<Pair<Name, InternetClassType>, Set<ResourceRecord<? extends Name, ? extends Serializable>>> cache);

    void appendDataIfIs(final InternetClassType internetClassType,
                        final Set<T> set);

    void removeFromCache(final Map<Pair<Name, InternetClassType>, Set<ResourceRecord<? extends Name, ? extends Serializable>>> cache);
}
