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

import java.util.logging.Logger;

/**
 * The Endpoint keeps track of the heartbeat state and the failure detector for
 * remote clients
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */

public class Endpoint {
    protected static Logger                 logger  = Logger.getLogger(Endpoint.class.getCanonicalName());

    private final PhiAccrualFailureDetector fd      = new PhiAccrualFailureDetector();
    private volatile GossipMessages         handler;
    private volatile HeartbeatState         heartbeat;
    private volatile boolean                isAlive = true;

    public Endpoint() {

    }

    public Endpoint(HeartbeatState heartBeatState) {
        heartbeat = heartBeatState;
    }

    public long getEpoch() {
        return heartbeat.getEpoch();
    }

    public GossipMessages getHandler() {
        return handler;
    }

    public String getMemberString() {
        return "[" + heartbeat.getSender() + " : "
               + heartbeat.getSenderAddress() + "]";
    }

    public HeartbeatState getState() {
        return heartbeat;
    }

    public long getViewNumber() {
        return heartbeat.getViewNumber();
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

    public void setCommunications(GossipMessages communications) {
        handler = communications;
    }

    /**
     * Answer true if the suspicion level of the failure detector is greater
     * than the conviction threshold
     * 
     * @param now
     *            - the time to measure at
     * @param convictThreshold
     *            - the threshold for conviction
     * @return true if the suspicion level of the failure detector is greater
     *         than the conviction threshold
     */
    public boolean shouldConvict(long now, double convictThreshold) {
        return fd.phi(now) > convictThreshold;
    }

    @Override
    public String toString() {
        return "Endpoint " + getMemberString();
    }

    public void updateState(long now, HeartbeatState newHbState) {
        heartbeat = newHbState;
        fd.record(now);
    }
}
