package com.softwarecraftsmen.dns.resourceRecords;

import static com.softwarecraftsmen.dns.Seconds.currentTime;
import static java.lang.String.format;
import static java.util.Locale.UK;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import com.softwarecraftsmen.Pair;
import com.softwarecraftsmen.dns.Seconds;
import com.softwarecraftsmen.dns.messaging.InternetClassType;
import com.softwarecraftsmen.dns.messaging.QClass;
import com.softwarecraftsmen.dns.messaging.serializer.AtomicWriter;
import com.softwarecraftsmen.dns.messaging.serializer.Serializable;
import com.softwarecraftsmen.dns.names.Name;

public abstract class AbstractResourceRecord<S extends Name, T extends Serializable>
        implements ResourceRecord<S, T> {
    private final S owner;
    private final InternetClassType internetClassType;
    private final QClass qClass;
    private final Seconds timeToLive;
    private final T data;

    // TODO: Create a Seconds time and use it here and for StatementOfAuthority
    public AbstractResourceRecord(final S owner,
                                  final InternetClassType internetClassType,
                                  final QClass qClass,
                                  final Seconds timeToLive, final T data) {
        this.owner = owner;
        this.internetClassType = internetClassType;
        this.qClass = qClass;
        this.timeToLive = timeToLive;
        this.data = data;
    }

    public void addToCache(final Seconds maximumTimeToLivePermitted,
                           final SortedMap<Seconds, Set<ResourceRecord<? extends Name, ? extends Serializable>>> bestBeforeTimesForResourceRecords,
                           final Map<Pair<Name, InternetClassType>, Set<ResourceRecord<? extends Name, ? extends Serializable>>> cache) {
        final Seconds expiresAtSystemTime = expiresAtSystemTime(maximumTimeToLivePermitted);
        if (expiresAtSystemTime.compareTo(currentTime()) == -1) {
            return;
        }
        if (!bestBeforeTimesForResourceRecords.containsKey(expiresAtSystemTime)) {
            bestBeforeTimesForResourceRecords.put(expiresAtSystemTime,
                                                  new LinkedHashSet<ResourceRecord<? extends Name, ? extends Serializable>>());
        }
        bestBeforeTimesForResourceRecords.get(expiresAtSystemTime).add(this);

        final Pair<Name, InternetClassType> key = new Pair<Name, InternetClassType>(
                                                                                    owner,
                                                                                    internetClassType);
        if (!cache.containsKey(key)) {
            cache.put(key,
                      new LinkedHashSet<ResourceRecord<? extends Name, ? extends Serializable>>());
        }
        final Set<ResourceRecord<? extends Name, ? extends Serializable>> resourceRecordSet = cache.get(key);
        resourceRecordSet.add(this);
    }

    public void appendDataIfIs(final InternetClassType internetClassType,
                               final Set<T> set) {
        if (isFor(internetClassType)) {
            set.add(data);
        }
    }

    @Override
    @SuppressWarnings({ "RedundantIfStatement" })
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AbstractResourceRecord that = (AbstractResourceRecord) o;

        if (!data.equals(that.data)) {
            return false;
        }
        if (internetClassType != that.internetClassType) {
            return false;
        }
        if (!owner.equals(that.owner)) {
            return false;
        }
        if (qClass != that.qClass) {
            return false;
        }
        if (!timeToLive.equals(that.timeToLive)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = owner.hashCode();
        result = 31 * result + internetClassType.hashCode();
        result = 31 * result + qClass.hashCode();
        result = 31 * result + timeToLive.hashCode();
        result = 31 * result + data.hashCode();
        return result;
    }

    public void removeFromCache(final Map<Pair<Name, InternetClassType>, Set<ResourceRecord<? extends Name, ? extends Serializable>>> cache) {
        final Pair<Name, InternetClassType> key = new Pair<Name, InternetClassType>(
                                                                                    owner,
                                                                                    internetClassType);
        if (cache.containsKey(key)) {
            cache.get(key).remove(this);
        }
    }

    public void serialize(final AtomicWriter writer) {
        owner.serialize(writer);
        internetClassType.serialize(writer);
        qClass.serialize(writer);
        timeToLive.serialize(writer);
        throw new UnsupportedOperationException(
                                                "Find a way to serialize length RDLENGTH for RDATA");
        //writer.writeUnsigned16BitInteger(data.length);
        //data.serialize(writer);
    }

    @Override
    public String toString() {
        return format(UK, "%1$s %2$s %3$s %4$s %5$s", owner, timeToLive,
                      qClass, internetClassType, data);
    }

    private Seconds expiresAtSystemTime(final Seconds maximumTimeToLivePermitted) {
        final Seconds actualTimeToLive = timeToLive.chooseSmallestValue(maximumTimeToLivePermitted);
        return currentTime().add(actualTimeToLive);
    }

    private boolean isFor(final InternetClassType internetClassType) {
        return this.internetClassType.equals(internetClassType);
    }
}
