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
    protected static Logger logger = Logger.getLogger(Endpoint.class.getCanonicalName());

    private final PhiAccrualFailureDetector fd = new PhiAccrualFailureDetector();
    private volatile HeartbeatState heartbeat;
    private volatile boolean isAlive = true;
    private volatile long update = System.currentTimeMillis();
    private final GossipCommunications handler;

    public Endpoint(HeartbeatState heartBeatState,
                    GossipCommunications communications) {
        heartbeat = heartBeatState;
        handler = communications;
    }

    public GossipCommunications getHandler() {
        return handler;
    }

    public long getEpoch() {
        return heartbeat.getSender().epoch;
    }

    public boolean interpret(long now, double convictThreshold) {
        return fd.phi(now) > convictThreshold;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void record(long now) {
        fd.record(now);
    }

    HeartbeatState getState() {
        return heartbeat;
    }

    long getUpdate() {
        return update;
    }

    void markAlive() {
        isAlive = true;
    }

    void markDead() {
        isAlive = false;
    }

    void updateState(long now, HeartbeatState newHbState) {
        update = now;
        heartbeat = newHbState;
    }

    public long getViewNumber() {
        return heartbeat.getViewNumber();
    }
}
