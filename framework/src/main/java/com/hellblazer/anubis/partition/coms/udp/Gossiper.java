package com.hellblazer.anubis.partition.coms.udp;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This module is responsible for Gossiping information for the local endpoint.
 * This abstraction maintains the list of live and dead endpoints. Periodically
 * i.e. every 1 second this module chooses a random node and initiates a round
 * of Gossip with it. A round of Gossip involves 3 rounds of messaging. For
 * instance if node A wants to initiate a round of Gossip with node B it starts
 * off by sending node B a GossipDigestSynMessage. Node B on receipt of this
 * message sends node A a GossipDigestAckMessage. On receipt of this message
 * node A sends node B a GossipDigestAck2Message which completes a round of
 * Gossip. This module as and when it hears one of the three above mentioned
 * messages updates the Failure Detector with the liveness information.
 */

public class Gossiper {
    private class GossipTask implements Runnable {
        @Override
        public void run() {
            try {

                /* Update the local heartbeat counter. */
                endpointStateMap.get(localAddress).setHeartbeatVersion(version.incrementAndGet());
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("My heartbeat is now "
                                  + endpointStateMap.get(localAddress).getHeartBeatVersion());
                }
                final List<GossipDigest> gDigests = new ArrayList<GossipDigest>();
                makeRandomGossipDigest(gDigests);

                if (gDigests.size() > 0) {
                    Syn message = new Syn(
                                                                                CLUSTER_NAME,
                                                                                gDigests);
                    /* Gossip to some random live member */
                    boolean gossipedToSeed = doGossipToLiveMember(message);

                    /* Gossip to some unreachable member with some probability to check if he is back up */
                    doGossipToUnreachableMember(message);

                    /* Gossip to a seed if we did not do so above, or we have seen less nodes
                       than there are seeds.  This prevents partitions where each group of nodes
                       is only gossiping to a subset of the seeds.

                       The most straightforward check would be to check that all the seeds have been
                       verified either as live or unreachable.  To avoid that computation each round,
                       we reason that:

                       either all the live nodes are seeds, in which case non-seeds that come online
                       will introduce themselves to a member of the ring by definition,

                       or there is at least one non-seed node in the list, in which case eventually
                       someone will gossip to it, and then do a gossip to a random seed from the
                       gossipedToSeed check.

                       See CASSANDRA-150 for more exposition. */
                    if (!gossipedToSeed || liveEndpoints.size() < seeds.size()) {
                        doGossipToSeed(message);
                    }

                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("Performing status check ...");
                    }
                    doStatusCheck();
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Gossip warning", e);
            }
        }
    }

    private final static int intervalInMillis = 1000;
    private static final int QUARANTINE_DELAY = 30 * 1000;
    private static final String CLUSTER_NAME = "FOO ME HARD";
    private static final Logger logger = Logger.getLogger(Gossiper.class.getCanonicalName());
    private static final Comparator<InetAddress> ADDRESS_COMPARATOR = new Comparator<InetAddress>() {
        @Override
        public int compare(InetAddress addr1, InetAddress addr2) {
            return addr1.getHostAddress().compareTo(addr2.getHostAddress());
        }
    };

    private final Map<InetAddress, EndpointState> endpointStateMap = new ConcurrentHashMap<InetAddress, EndpointState>();
    private final long aVeryLongTime;
    private final double convictThreshold;
    private final long FatClientTimeout;
    private final ScheduledExecutorService gossipService;
    private AtomicInteger version = new AtomicInteger(0);
    /* map where key is endpoint and value is timestamp when this endpoint was removed from
     * gossip. We will ignore any gossip regarding these endpoints for QUARANTINE_DELAY time
     * after removal to prevent nodes from falsely reincarnating during the time when removal
     * gossip gets propagated to all nodes */
    private final Map<InetAddress, Long> justRemovedEndpoints = new ConcurrentHashMap<InetAddress, Long>();
    /* live member set */
    private final Set<InetAddress> liveEndpoints = new ConcurrentSkipListSet<InetAddress>(
                                                                                          ADDRESS_COMPARATOR);
    private final InetAddress localAddress;
    private final Random random;
    private ScheduledFuture<?> scheduledGossipTask;
    /* initial seeds for joining the cluster */
    private final Set<InetAddress> seeds = new ConcurrentSkipListSet<InetAddress>(
                                                                                  ADDRESS_COMPARATOR);
    /* subscribers for interest in EndpointState change */
    private final List<IEndpointStateChangeSubscriber> subscribers = new CopyOnWriteArrayList<IEndpointStateChangeSubscriber>();
    /* unreachable member set */
    private final Map<InetAddress, Long> unreachableEndpoints = new ConcurrentHashMap<InetAddress, Long>();

    private Gossiper(InetAddress address, Random r, double phiConvictThreshold) {
        if (phiConvictThreshold < 5 || phiConvictThreshold > 16) {
            throw new IllegalArgumentException(
                                               "Phi conviction threshold must be between 5 and 16, inclusively");
        }
        localAddress = address;
        random = r;
        convictThreshold = phiConvictThreshold;
        // 3 days
        aVeryLongTime = 259200 * 1000;
        // half of QUARATINE_DELAY, to ensure justRemovedEndpoints has enough leeway to prevent re-gossip
        FatClientTimeout = QUARANTINE_DELAY / 2;
        gossipService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r, "Anubis: Gossip servicing thread");
                daemon.setDaemon(true);
                return daemon;
            }
        });
    }

    /**
     * Add an endpoint we knew about previously, but whose state is unknown
     */
    public void addSavedEndpoint(InetAddress ep) {
        EndpointState epState = endpointStateMap.get(ep);
        if (epState == null) {
            epState = new EndpointState(0);
            epState.markDead();
            epState.setHasToken(true);
            endpointStateMap.put(ep, epState);
            unreachableEndpoints.put(ep, System.currentTimeMillis());
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Adding saved endpoint " + ep + " "
                              + epState.getGeneration());
            }
        }
    }

    public long getEndpointDowntime(InetAddress ep) {
        Long downtime = unreachableEndpoints.get(ep);
        if (downtime != null) {
            return System.currentTimeMillis() - downtime;
        } else {
            return 0L;
        }
    }

    public Set<InetAddress> getLiveMembers() {
        Set<InetAddress> liveMbrs = new HashSet<InetAddress>(liveEndpoints);
        if (!liveMbrs.contains(localAddress)) {
            liveMbrs.add(localAddress);
        }
        return liveMbrs;
    }

    public Set<InetAddress> getUnreachableMembers() {
        return unreachableEndpoints.keySet();
    }

    public boolean isEnabled() {
        return !scheduledGossipTask.isCancelled();
    }

    public boolean isKnownEndpoint(InetAddress endpoint) {
        return endpointStateMap.containsKey(endpoint);
    }

    /**
     * Register for interesting state changes.
     * 
     * @param subscriber
     *            module which implements the IEndpointStateChangeSubscriber
     */
    public void register(IEndpointStateChangeSubscriber subscriber) {
        subscribers.add(subscriber);
    }

    /**
     * Start the gossiper
     */
    public void start(int generation, Set<InetAddress> seedHosts) {
        for (InetAddress seed : seedHosts) {
            if (seed.equals(localAddress)) {
                continue;
            }
            seeds.add(seed);
        }

        /* initialize the heartbeat state for this localEndpoint */
        EndpointState localState = endpointStateMap.get(localAddress);
        if (localState == null) {
            localState = new EndpointState(generation);
            localState.markAlive();
            endpointStateMap.put(localAddress, localState);
        }

        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("gossip started with generation "
                          + localState.getGeneration());
        }

        scheduledGossipTask = gossipService.scheduleWithFixedDelay(new GossipTask(),
                                                                   Gossiper.intervalInMillis,
                                                                   Gossiper.intervalInMillis,
                                                                   TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduledGossipTask.cancel(false);
    }

    /**
     * Unregister interest for state changes.
     * 
     * @param subscriber
     *            module which implements the IEndpointStateChangeSubscriber
     */
    public void unregister(IEndpointStateChangeSubscriber subscriber) {
        subscribers.remove(subscriber);
    }

    void handleGossipDigestAck(Message message, String id) {
        InetAddress from = message.getFrom();
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("Received a GossipDigestAckMessage from %s",
                                 from));
        }
        if (!isEnabled()) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Ignoring GossipDigestAckMessage because gossip is disabled");
            }
            return;
        }

        byte[] bytes = message.getMessageBody();
        new DataInputStream(new ByteArrayInputStream(bytes));

        Ack gDigestAckMessage = null;
        List<GossipDigest> gDigestList = gDigestAckMessage.digests;
        Map<InetAddress, EndpointState> epStateMap = gDigestAckMessage.getEndpointStateMap();

        if (epStateMap.size() > 0) {
            /* Notify the Failure Detector */
            for (Entry<InetAddress, EndpointState> entry : epStateMap.entrySet()) {
                endpointStateMap.get(entry.getKey()).record(entry.getValue());
            }
            applyStateLocally(epStateMap);
        }

        /* Get the state required to send to this gossipee - construct GossipDigestAck2Message */
        Map<InetAddress, EndpointState> deltaEpStateMap = new HashMap<InetAddress, EndpointState>();
        for (GossipDigest gDigest : gDigestList) {
            InetAddress addr = gDigest.getEndpoint();
            EndpointState localEpStatePtr = getStateForVersionBiggerThan(addr,
                                                                         gDigest.getMaxVersion());
            if (localEpStatePtr != null) {
                deltaEpStateMap.put(addr, localEpStatePtr);
            }
        }

        Ack2 gDigestAck2 = new Ack2(
                                                                          deltaEpStateMap);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("Sending a GossipDigestAck2Message to %s",
                                 from));
        }
        sendOneWay(gDigestAck2, from);
    }

    void handleGossipDigestAck2(Message message, String id) {
        InetAddress from = message.getFrom();
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("Received a GossipDigestAck2Message from %s",
                                 from));
        }

        byte[] bytes = message.getMessageBody();
        new DataInputStream(new ByteArrayInputStream(bytes));
        Ack2 gDigestAck2Message = null;
        Map<InetAddress, EndpointState> remoteEpStateMap = gDigestAck2Message.getEndpointStateMap();
        /* Notify the Failure Detector */
        for (Entry<InetAddress, EndpointState> entry : remoteEpStateMap.entrySet()) {
            endpointStateMap.get(entry.getKey()).record(entry.getValue());
        }
        applyStateLocally(remoteEpStateMap);
    }

    void handleGossipDigestSyn(Message message, String id) {
        InetAddress from = message.getFrom();
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("Received a GossipDigestSynMessage from %s",
                                 from));
        }
        if (!isEnabled()) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Ignoring GossipDigestSynMessage because gossip is disabled");
            }
            return;
        }

        byte[] bytes = message.getMessageBody();
        new DataInputStream(new ByteArrayInputStream(bytes));

        Syn gDigestMessage = null;
        /* If the message is from a different cluster throw it away. */
        if (!gDigestMessage.id.equals(CLUSTER_NAME)) {
            logger.warning(format("ClusterName mismatch from %s %s != %s",
                                  from, gDigestMessage.id, CLUSTER_NAME));
            return;
        }

        List<GossipDigest> gDigestList = gDigestMessage.getGossipDigests();
        if (logger.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (GossipDigest gDigest : gDigestList) {
                sb.append(gDigest);
                sb.append(" ");
            }
            logger.finest(format("Gossip syn digests are : %s" + sb.toString()));
        }
        /* Notify the Failure Detector */
        for (GossipDigest gDigest : gDigestList) {
            endpointStateMap.get(gDigest.endpoint).record(endpointStateMap.get(gDigest.endpoint));
        }

        doSort(gDigestList);

        List<GossipDigest> deltaGossipDigestList = new ArrayList<GossipDigest>();
        Map<InetAddress, EndpointState> deltaEpStateMap = new HashMap<InetAddress, EndpointState>();
        examineGossiper(gDigestList, deltaGossipDigestList, deltaEpStateMap);

        Ack gDigestAck = new Ack(
                                                                       deltaGossipDigestList,
                                                                       deltaEpStateMap);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("Sending a GossipDigestAckMessage to %s", from));
        }
        sendOneWay(gDigestAck, from);
    }

    private void applyNewStates(InetAddress addr, EndpointState localState,
                                EndpointState remoteState) {
        // don't assert here, since if the node restarts the version will go back to zero
        int oldVersion = localState.getHeartBeatVersion();

        localState.setHeartBeatState(remoteState.getHeartBeatState());
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("Updating heartbeat state version to "
                          + localState.getHeartBeatVersion() + " from "
                          + oldVersion + " for " + addr + " ...");
        }
    }

    private void applyStateLocally(Map<InetAddress, EndpointState> epStateMap) {
        for (Entry<InetAddress, EndpointState> entry : epStateMap.entrySet()) {
            InetAddress ep = entry.getKey();
            if (ep.equals(localAddress)) {
                continue;
            }
            if (justRemovedEndpoints.containsKey(ep)) {
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("Ignoring gossip for " + ep
                                  + " because it is quarantined");
                }
                continue;
            }

            EndpointState localEpStatePtr = endpointStateMap.get(ep);
            EndpointState remoteState = entry.getValue();
            /*
                If state does not exist just add it. If it does then add it if the remote generation is greater.
                If there is a generation tie, attempt to break it by heartbeat version.
            */
            if (localEpStatePtr != null) {
                int localGeneration = localEpStatePtr.getGeneration();
                int remoteGeneration = remoteState.getGeneration();
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest(ep + "local generation " + localGeneration
                                  + ", remote generation " + remoteGeneration);
                }

                if (remoteGeneration > localGeneration) {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("Updating heartbeat state generation to "
                                      + remoteGeneration + " from "
                                      + localGeneration + " for " + ep);
                    }
                    // major state change will handle the update by inserting the remote state directly
                    handleMajorStateChange(ep, remoteState);
                } else if (remoteGeneration == localGeneration) // generation has not changed, apply new states
                {
                    /* find maximum state */
                    int localMaxVersion = getMaxEndpointStateVersion(localEpStatePtr);
                    int remoteMaxVersion = getMaxEndpointStateVersion(remoteState);
                    if (remoteMaxVersion > localMaxVersion) {
                        // apply states, but do not notify since there is no major change
                        applyNewStates(ep, localEpStatePtr, remoteState);
                    } else if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("Ignoring remote version "
                                      + remoteMaxVersion + " <= "
                                      + localMaxVersion + " for " + ep);
                    }
                } else {
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest("Ignoring remote generation "
                                      + remoteGeneration + " < "
                                      + localGeneration);
                    }
                }
            } else {
                // this is a new node
                handleMajorStateChange(ep, remoteState);
            }
        }
    }

    /* Sends a Gossip message to a live member and returns true if the recipient was a seed */
    private boolean doGossipToLiveMember(Message message) {
        int size = liveEndpoints.size();
        if (size == 0) {
            return false;
        }
        return sendGossip(message, liveEndpoints);
    }

    /* Gossip to a seed for facilitating partition healing */
    private void doGossipToSeed(Message message) {
        int size = seeds.size();
        if (size > 0) {
            if (size == 1 && seeds.contains(localAddress)) {
                return;
            }

            if (liveEndpoints.size() == 0) {
                sendGossip(message, seeds);
            } else {
                /* Gossip with the seed with some probability. */
                double probability = seeds.size()
                                     / (double) (liveEndpoints.size() + unreachableEndpoints.size());
                double randDbl = random.nextDouble();
                if (randDbl <= probability) {
                    sendGossip(message, seeds);
                }
            }
        }
    }

    /* Sends a Gossip message to an unreachable member */
    private void doGossipToUnreachableMember(Message message) {
        double liveEndpointCount = liveEndpoints.size();
        double unreachableEndpointCount = unreachableEndpoints.size();
        if (unreachableEndpointCount > 0) {
            /* based on some probability */
            double prob = unreachableEndpointCount / (liveEndpointCount + 1);
            double randDbl = random.nextDouble();
            if (randDbl < prob) {
                sendGossip(message, unreachableEndpoints.keySet());
            }
        }
    }

    /*
     * First construct a map whose key is the endpoint in the GossipDigest and the value is the
     * GossipDigest itself. Then build a list of version differences i.e difference between the
     * version in the GossipDigest and the version in the local state for a given InetAddress.
     * Sort this list. Now loop through the sorted list and retrieve the GossipDigest corresponding
     * to the endpoint from the map that was initially constructed.
    */
    private void doSort(List<GossipDigest> gDigestList) {
        /* Construct a map of endpoint to GossipDigest. */
        Map<InetAddress, GossipDigest> epToDigestMap = new HashMap<InetAddress, GossipDigest>();
        for (GossipDigest gDigest : gDigestList) {
            epToDigestMap.put(gDigest.getEndpoint(), gDigest);
        }

        /*
         * These digests have their maxVersion set to the difference of the version
         * of the local EndpointState and the version found in the GossipDigest.
        */
        List<GossipDigest> diffDigests = new ArrayList<GossipDigest>();
        for (GossipDigest gDigest : gDigestList) {
            InetAddress ep = gDigest.getEndpoint();
            EndpointState epState = endpointStateMap.get(ep);
            int version = epState != null ? getMaxEndpointStateVersion(epState)
                                         : 0;
            int diffVersion = Math.abs(version - gDigest.getMaxVersion());
            diffDigests.add(new GossipDigest(ep, gDigest.getGeneration(),
                                             diffVersion));
        }

        gDigestList.clear();
        Collections.sort(diffDigests);
        int size = diffDigests.size();
        /*
         * Report the digests in descending order. This takes care of the endpoints
         * that are far behind w.r.t this local endpoint
        */
        for (int i = size - 1; i >= 0; --i) {
            gDigestList.add(epToDigestMap.get(diffDigests.get(i).getEndpoint()));
        }
    }

    private void doStatusCheck() {
        long now = System.currentTimeMillis();

        for (Entry<InetAddress, EndpointState> entry : endpointStateMap.entrySet()) {
            InetAddress endpoint = entry.getKey();
            if (endpoint.equals(localAddress)) {
                continue;
            }

            EndpointState endpointState = entry.getValue();
            if (endpointState.interpret(convictThreshold)
                && endpointState.isAlive()) {
                markDead(endpoint, endpointState);
            }
            if (endpointState != null) {
                long duration = now - endpointState.getUpdate();

                if (isMember(endpoint)) {
                    endpointState.setHasToken(true);
                }
                // check if this is a fat client. fat clients are removed automatically from
                // gosip after FatClientTimeout
                if (!endpointState.getHasToken() && !endpointState.isAlive()
                    && !justRemovedEndpoints.containsKey(endpoint)
                    && duration > FatClientTimeout) {
                    logger.info("FatClient " + endpoint
                                + " has been silent for " + FatClientTimeout
                                + "ms, removing from gossip");
                    removeEndpoint(endpoint); // will put it in justRemovedEndpoints to respect quarantine delay
                    unreachableEndpoints.remove(endpoint);
                    endpointStateMap.remove(endpoint); // can get rid of the state immediately
                }

                if (!endpointState.isAlive() && duration > aVeryLongTime) {
                    unreachableEndpoints.remove(endpoint);
                    endpointStateMap.remove(endpoint);
                }
            }
        }

        if (!justRemovedEndpoints.isEmpty()) {
            for (Map.Entry<InetAddress, Long> entry : justRemovedEndpoints.entrySet()) {
                if (now - entry.getValue() > QUARANTINE_DELAY) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine(QUARANTINE_DELAY + " elapsed, "
                                    + entry.getKey()
                                    + " gossip quarantine over");
                    }
                    justRemovedEndpoints.remove(entry.getKey());
                }
            }
        }
    }

    /*
        This method is used to figure the state that the Gossiper has but Gossipee doesn't. The delta digests
        and the delta state are built up.
    */
    private void examineGossiper(List<GossipDigest> gDigestList,
                                 List<GossipDigest> deltaGossipDigestList,
                                 Map<InetAddress, EndpointState> deltaEpStateMap) {
        for (GossipDigest gDigest : gDigestList) {
            int remoteGeneration = gDigest.getGeneration();
            int maxRemoteVersion = gDigest.getMaxVersion();
            /* Get state associated with the end point in digest */
            EndpointState epStatePtr = endpointStateMap.get(gDigest.getEndpoint());
            /*
                Here we need to fire a GossipDigestAckMessage. If we have some data associated with this endpoint locally
                then we follow the "if" path of the logic. If we have absolutely nothing for this endpoint we need to
                request all the data for this endpoint.
            */
            if (epStatePtr != null) {
                int localGeneration = epStatePtr.getGeneration();
                /* get the max version of all keys in the state associated with this endpoint */
                int maxLocalVersion = getMaxEndpointStateVersion(epStatePtr);
                if (remoteGeneration == localGeneration
                    && maxRemoteVersion == maxLocalVersion) {
                    continue;
                }

                if (remoteGeneration > localGeneration) {
                    /* we request everything from the gossiper */
                    requestAll(gDigest, deltaGossipDigestList, remoteGeneration);
                } else if (remoteGeneration < localGeneration) {
                    /* send all data with generation = localgeneration and version > 0 */
                    sendAll(gDigest, deltaEpStateMap, 0);
                } else if (remoteGeneration == localGeneration) {
                    /*
                        If the max remote version is greater then we request the remote endpoint send us all the data
                        for this endpoint with version greater than the max version number we have locally for this
                        endpoint.
                        If the max remote version is lesser, then we send all the data we have locally for this endpoint
                        with version greater than the max remote version.
                    */
                    if (maxRemoteVersion > maxLocalVersion) {
                        deltaGossipDigestList.add(new GossipDigest(
                                                                   gDigest.getEndpoint(),
                                                                   remoteGeneration,
                                                                   maxLocalVersion));
                    } else if (maxRemoteVersion < maxLocalVersion) {
                        /* send all data with generation = localgeneration and version > maxRemoteVersion */
                        sendAll(gDigest, deltaEpStateMap, maxRemoteVersion);
                    }
                }
            } else {
                /* We are here since we have no data for this endpoint locally so request everything. */
                requestAll(gDigest, deltaGossipDigestList, remoteGeneration);
            }
        }
    }

    /**
     * Return either: the greatest heartbeat or application state
     * 
     * @param epState
     * @return
     */
    private int getMaxEndpointStateVersion(EndpointState epState) {
        return epState.getHeartBeatVersion();
    }

    private EndpointState getStateForVersionBiggerThan(InetAddress forEndpoint,
                                                       int version) {
        EndpointState epState = endpointStateMap.get(forEndpoint);
        EndpointState reqdEndpointState = null;

        if (epState != null) {
            /*
             * Here we try to include the Heart Beat state only if it is
             * greater than the version passed in. It might happen that
             * the heart beat version maybe lesser than the version passed
             * in and some application state has a version that is greater
             * than the version passed in. In this case we also send the old
             * heart beat and throw it away on the receiver if it is redundant.
            */
            int localHbVersion = epState.getHeartBeatVersion();
            if (localHbVersion > version) {
                reqdEndpointState = new EndpointState(
                                                      epState.getHeartBeatState());
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("local heartbeat version " + localHbVersion
                                  + " greater than " + version + " for "
                                  + forEndpoint);
                }
            }
        }
        return reqdEndpointState;
    }

    /**
     * This method is called whenever there is a "big" change in ep state (a
     * generation change for a known node).
     * 
     * @param ep
     *            endpoint
     * @param epState
     *            EndpointState for the endpoint
     */
    private void handleMajorStateChange(InetAddress ep, EndpointState epState) {
        if (endpointStateMap.get(ep) != null) {
            logger.info(format("Node %s has restarted, now UP again", ep));
        } else {
            logger.info(format("Node %s is now part of the cluster", ep));
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("Adding endpoint state for " + ep);
        }
        endpointStateMap.put(ep, epState);
        markAlive(ep, epState);
        for (IEndpointStateChangeSubscriber subscriber : subscribers) {
            subscriber.onJoin(ep, epState);
        }
    }

    private boolean isMember(InetAddress endpoint) {
        // TODO Auto-generated method stub
        return false;
    }

    /**
     * The gossip digest is built based on randomization rather than just
     * looping through the collection of live endpoints.
     * 
     * @param gDigests
     *            list of Gossip Digests.
     */
    private void makeRandomGossipDigest(List<GossipDigest> gDigests) {
        EndpointState epState;
        int generation = 0;
        int maxVersion = 0;

        // local epstate will be part of endpointStateMap
        List<InetAddress> endpoints = new ArrayList<InetAddress>(
                                                                 endpointStateMap.keySet());
        Collections.shuffle(endpoints, random);
        for (InetAddress endpoint : endpoints) {
            epState = endpointStateMap.get(endpoint);
            if (epState != null) {
                generation = epState.getGeneration();
                maxVersion = getMaxEndpointStateVersion(epState);
            }
            gDigests.add(new GossipDigest(endpoint, generation, maxVersion));
        }

        if (logger.isLoggable(Level.FINEST)) {
            StringBuilder sb = new StringBuilder();
            for (GossipDigest gDigest : gDigests) {
                sb.append(gDigest);
                sb.append(" ");
            }
            logger.finest("Gossip Digests are : " + sb.toString());
        }
    }

    private void markAlive(InetAddress addr, EndpointState localState) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("marking as alive %s", addr));
        }
        localState.markAlive();
        liveEndpoints.add(addr);
        unreachableEndpoints.remove(addr);
        logger.info(format("InetAddress %s is now UP", addr));
        for (IEndpointStateChangeSubscriber subscriber : subscribers) {
            subscriber.onAlive(addr, localState);
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("Notified %s", subscribers));
        }
    }

    private void markDead(InetAddress addr, EndpointState localState) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("marking as dead %s", addr));
        }
        localState.markDead();
        liveEndpoints.remove(addr);
        unreachableEndpoints.put(addr, System.currentTimeMillis());
        logger.info(format("InetAddress %s is now dead.", addr));
        for (IEndpointStateChangeSubscriber subscriber : subscribers) {
            subscriber.onDead(addr, localState);
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("Notified %s", subscribers));
        }
    }

    /**
     * Removes the endpoint from Gossip but retains endpoint state
     */
    private void removeEndpoint(InetAddress endpoint) {
        // do subscribers first so anything in the subscriber that depends on gossiper state won't get confused
        for (IEndpointStateChangeSubscriber subscriber : subscribers) {
            subscriber.onRemove(endpoint);
        }

        liveEndpoints.remove(endpoint);
        unreachableEndpoints.remove(endpoint);
        justRemovedEndpoints.put(endpoint, System.currentTimeMillis());
    }

    /* Request all the state for the endpoint in the gDigest */
    private void requestAll(GossipDigest gDigest,
                            List<GossipDigest> deltaGossipDigestList,
                            int remoteGeneration) {
        /* We are here since we have no data for this endpoint locally so request everthing. */
        deltaGossipDigestList.add(new GossipDigest(gDigest.getEndpoint(),
                                                   remoteGeneration, 0));
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("requestAll for " + gDigest.getEndpoint());
        }
    }

    /* Send all the data with version greater than maxRemoteVersion */
    private void sendAll(GossipDigest gDigest,
                         Map<InetAddress, EndpointState> deltaEpStateMap,
                         int maxRemoteVersion) {
        EndpointState localEpStatePtr = getStateForVersionBiggerThan(gDigest.getEndpoint(),
                                                                     maxRemoteVersion);
        if (localEpStatePtr != null) {
            deltaEpStateMap.put(gDigest.getEndpoint(), localEpStatePtr);
        }
    }

    /**
     * Returns true if the chosen target was also a seed. False otherwise
     * 
     * @param message
     *            produces a message to send
     * @param epSet
     *            a set of endpoint from which a random endpoint is chosen.
     * @return true if the chosen endpoint is also a seed.
     */
    private boolean sendGossip(Message message, Set<InetAddress> epSet) {
        int size = epSet.size();
        /* Generate a random number from 0 -> size */
        List<InetAddress> liveEndpoints = new ArrayList<InetAddress>(epSet);
        int index = size == 1 ? 0 : random.nextInt(size);
        InetAddress to = liveEndpoints.get(index);
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(format("Sending a GossipDigestSynMessage to %s ...",
                                 to));
        }
        sendOneWay(message, to);
        return seeds.contains(to);
    }

    private void sendOneWay(Message message, InetAddress to) {
        // TODO Auto-generated method stub

    }
}