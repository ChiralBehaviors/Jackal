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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This abstraction represents the HeartBeatState in an EndpointState instance.
 */

public class EndpointState {

    static class HeartBeat {
        private int generation;
        private int version;

        public HeartBeat(int gen) {
            generation = gen;
        }

        public boolean record(HeartBeat remote) {
            return remote.generation > generation || remote.version > version;
        }
    }

    protected static Logger logger = Logger.getLogger(EndpointState.class.getCanonicalName());

    private final PhiAccrualFailureDetector fd = new PhiAccrualFailureDetector();
    private volatile HeartBeat heartbeat;
    private volatile boolean isAlive = true;
    private volatile long update = System.currentTimeMillis();

    public EndpointState(HeartBeat heartBeatState) {
        heartbeat = heartBeatState;
    }

    EndpointState(int generation) {
        this(new HeartBeat(generation));
    }

    public int getGeneration() {
        return heartbeat.generation;
    }

    public int getHeartbeatVersion() {
        return heartbeat.version;
    }

    public boolean interpret(long now, double convictThreshold) {
        return fd.phi(now) > convictThreshold;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void record(EndpointState remote, long now) {
        if (heartbeat.record(remote.heartbeat)) {
            fd.record(now);
        }
    }

    HeartBeat getHeartBeatState() {
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

    void setHeartBeatState(HeartBeat newHbState) {
        update();
        heartbeat = newHbState;
    }

    void updateHeartbeatVersion(int newVersion) {
        heartbeat.version = newVersion;
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("My heartbeat is now " + heartbeat.version);
        }
    }

    private void update() {
        update = System.currentTimeMillis();
    }
}
