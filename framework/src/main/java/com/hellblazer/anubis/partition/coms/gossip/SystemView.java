/** 
 * (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.anubis.partition.coms.gossip;

import static java.lang.String.format;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a view on the known endpoint state for the system. The primary
 * responsibility of the system view is to provide random members from the
 * various subsets of the member endpoints the view tracks.The system endpoint
 * view is composed of live endpoints - endpoints that are considered up and
 * functioning normally - and unreachable endpoints - endpoints that are
 * considered down and non functional. A subset of the endpoints in the system
 * serve as seeds that form the kernel set of endpoints used to construct the
 * system view. The system view also tracks members that are considered
 * quarantined. Quarantined members are members that have been marked dead and
 * are prohibited from rejoing the set of live endpoints until the quarantine
 * period has elapsed.
 * 
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class SystemView {
    private static final long A_VERY_LONG_TIME = 259200 * 1000; // 3 days
    private static final Comparator<InetSocketAddress> ADDRESS_COMPARATOR = new Comparator<InetSocketAddress>() {
        @Override
        public int compare(InetSocketAddress addr1, InetSocketAddress addr2) {
            int hostCompare = addr1.getAddress().getHostAddress().compareTo(addr2.getAddress().getHostAddress());
            if (hostCompare == 0) {
                int port1 = addr1.getPort();
                int port2 = addr2.getPort();
                if (port1 == port2) {
                    return 0;
                }
                if (port1 > port2) {
                    return 1;
                }
                return -1;
            }
            return hostCompare;
        }
    };
    private static final Logger log = Logger.getLogger(SystemView.class.getCanonicalName());
    private static final int QUARANTINE_DELAY = 30 * 1000;
    private final Random entropy;
    private final Set<InetSocketAddress> live = new ConcurrentSkipListSet<InetSocketAddress>(
                                                                                             ADDRESS_COMPARATOR);
    private final InetSocketAddress localAddress;
    private final Map<InetSocketAddress, Long> quarantined = new ConcurrentHashMap<InetSocketAddress, Long>();
    private final Set<InetSocketAddress> seeds = new ConcurrentSkipListSet<InetSocketAddress>(
                                                                                              ADDRESS_COMPARATOR);
    private final Map<InetSocketAddress, Long> unreachable = new ConcurrentHashMap<InetSocketAddress, Long>();

    public SystemView(Random random, InetSocketAddress local,
                      Collection<InetSocketAddress> seedHosts) {
        entropy = random;
        localAddress = local;
        for (InetSocketAddress seed : seedHosts) {
            if (!seed.equals(localAddress)) {
                seeds.add(seed);
            }
        }
    }

    /**
     * Remove endpoints that have been quarantined for a sufficient time.
     * 
     * @param now
     *            - the time to determine the interval the endpoint has been
     *            quarantined
     */
    public void cullQuarantined(long now) {
        if (!quarantined.isEmpty()) {
            for (Map.Entry<InetSocketAddress, Long> entry : quarantined.entrySet()) {
                if (now - entry.getValue() > QUARANTINE_DELAY) {
                    if (log.isLoggable(Level.FINE)) {
                        log.fine(QUARANTINE_DELAY + " elapsed, "
                                 + entry.getKey() + " gossip quarantine over");
                    }
                    quarantined.remove(entry.getKey());
                }
            }
        }
    }

    public boolean cullUnreachable(InetSocketAddress endpoint, long now) {
        if (now > A_VERY_LONG_TIME) {
            unreachable.remove(endpoint);
            return true;
        }
        return false;
    }

    /**
     * Answer how long, in millseconds, the endpoint has been unreachable
     * 
     * @param endpoint
     * @return the number of milliseconds the endpoint has been unreachable, or
     *         0, if the endpoint is not in the set of unreachable endpoints
     */
    public long getEndpointDowntime(InetSocketAddress endpoint) {
        Long downtime = unreachable.get(endpoint);
        if (downtime != null) {
            return System.currentTimeMillis() - downtime;
        }
        return 0L;
    }

    /**
     * Answer the collection of live endpoints in the view
     * 
     * @return the collection of endpoints that are considered live
     */
    public Collection<InetSocketAddress> getLiveMembers() {
        return Collections.unmodifiableCollection(live);
    }

    /**
     * Answer the view's local address
     * 
     * @return the address of the view
     */
    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    /**
     * Answer a random member of the live set.
     * 
     * @return the live member, or null if there are no live members
     */
    public InetSocketAddress getRandomLiveMember() {
        return getRandomMember(live);
    }

    /**
     * Answer a random member of the seed set. We only return a member of the
     * seed set if the member supplied is null, or if the size of the live
     * endpoint set is smaller than the size of the seed set, a member is
     * selected with the probleability defined by the ratio of the cardinality
     * of the seed set dived by the sum of the cardinalities of the live and
     * unreachable endpoint sets
     * 
     * @param member
     *            - the member that has been gossiped with
     * @return a random member of the seed set, if appropriate, or null
     */
    public InetSocketAddress getRandomSeedMember(InetSocketAddress member) {
        if (member == null) {
            return getRandomMember(seeds);
        }
        if (seeds.contains(member)) {
            return null;
        }
        if (live.size() < seeds.size()) {
            int size = seeds.size();
            if (size > 0) {
                if (size == 1 && seeds.contains(localAddress)) {
                    return null;
                }

                if (live.size() == 0) {
                    return getRandomMember(seeds);
                }
                if (entropy.nextDouble() <= seeds.size()
                                            / (double) (live.size() + unreachable.size())) {
                    return getRandomMember(seeds);
                }
            }
        }
        return null;
    }

    /**
     * Answer a random member of the unreachable set. The unreachable set will
     * be sampled with a probability of the cardinality of the unreachable set
     * divided by the cardinality of the live set of endpionts + 1
     * 
     * @return the unreachable member selected, or null if none selected or
     *         available
     */
    public InetSocketAddress getRandomUnreachableMember() {
        if (entropy.nextDouble() < unreachable.size()
                                   / ((double) live.size() + 1)) {
            return getRandomMember(unreachable.keySet());
        }
        return null;
    }

    /**
     * Answer the set of unreachable members in the view
     * 
     * @return the set of unreachable endpoints.
     */
    public Collection<InetSocketAddress> getUnreachableMembers() {
        return Collections.unmodifiableCollection(unreachable.keySet());
    }

    /**
     * Answer true if the endpoint is quarantined.
     * 
     * @param ep
     *            - the endpoint to query
     * @return true if the endpoint is currently quarantined
     */
    public boolean isQuarantined(InetSocketAddress ep) {
        return quarantined.containsKey(ep);
    }

    /**
     * Mark the endpoint as live.
     * 
     * @param endpoint
     *            - the endpoint to mark as live
     */
    public void markAlive(InetSocketAddress endpoint) {
        live.add(endpoint);
        unreachable.remove(endpoint);
    }

    /**
     * Mark the endpoint as dead
     * 
     * @param endpoint
     *            - the endpoint to mark as dead
     */
    public void markDead(InetSocketAddress endpoint) {
        live.remove(endpoint);
        unreachable.put(endpoint, System.currentTimeMillis());
        quarantined.put(endpoint, System.currentTimeMillis());
    }

    public void markUnreachable(InetSocketAddress ep) {
        unreachable.put(ep, System.currentTimeMillis());
    }

    /**
     * Answer a random member of the endpoint collection.
     * 
     * @param endpoints
     *            - the endpoints to sample
     * @return the selected member
     */
    private InetSocketAddress getRandomMember(Collection<InetSocketAddress> endpoints) {
        if (endpoints.isEmpty()) {
            return null;
        }
        int size = endpoints.size();
        int index = size == 1 ? 0 : entropy.nextInt(size);
        int i = 0;
        for (InetSocketAddress address : endpoints) {
            if (i++ == index) {
                return address;
            }
        }
        throw new IllegalStateException(
                                        format("We should have found the selected random member of the supplied endpoint set: %s",
                                               index));
    }
}
