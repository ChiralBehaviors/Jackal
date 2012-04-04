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
package com.hellblazer.jackal.gossip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.util.Identity;

/**
 * The Endpoint keeps track of the heartbeat state and the failure detector for
 * remote clients
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */

public class Endpoint {
    protected static Logger         logger  = LoggerFactory.getLogger(Endpoint.class);

    private final FailureDetector   fd;
    private volatile GossipMessages handler;
    private volatile HeartbeatState heartbeat;
    private volatile boolean        isAlive = true;

    public Endpoint() {
        fd = null;
    }

    public Endpoint(HeartbeatState heartBeatState,
                    FailureDetector failureDetector) {
        heartbeat = heartBeatState;
        fd = failureDetector;
    }

    public long getEpoch() {
        return heartbeat.getEpoch();
    }

    public GossipMessages getHandler() {
        return handler;
    }

    public Identity getId() {
        return heartbeat.getSender();
    }

    public String getMemberString() {
        return heartbeat.getMemberString();
    }

    public HeartbeatState getState() {
        return heartbeat;
    }

    public long getTime() {
        return heartbeat.getTime();
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void markAlive() {
        isAlive = true;
    }

    public void markDead() {
        isAlive = false;
    }

    public void record(HeartbeatState newHbState) {
        if (heartbeat != newHbState) {
            heartbeat = newHbState;
            fd.record(heartbeat.getTime(), System.currentTimeMillis());
        }
    }

    public void setCommunications(GossipMessages communications) {
        handler = communications;
    }

    /**
     * Answer true if the suspicion level of the failure detector is greater
     * than the conviction threshold
     * 
     * @param now
     *            - the time at which to base the measurement
     * @return true if the suspicion level of the failure detector is greater
     *         than the conviction threshold
     */
    public boolean shouldConvict(long now) {
        return !heartbeat.isDiscoveryOnly() && fd.shouldConvict(now);
    }

    @Override
    public String toString() {
        return "Endpoint " + getMemberString();
    }

    public void updateState(HeartbeatState newHbState) {
        heartbeat = newHbState;
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("%s new heartbeat time: %s",
                                       heartbeat.getSender(),
                                       heartbeat.getTime()));
        }
    }
}
