package com.hellblazer.anubis.partition.coms.udp;

import java.util.logging.Logger;

/**
 * This abstraction represents the HeartBeatState in an EndpointState instance.
 */

public class EndpointState {

    static class HeartBeat {
        public HeartBeat(int gen) {
            generation = gen;
        }

        private int generation;
        private int version;
        public boolean record(HeartBeat remote) { 
            return (remote.generation > generation) || (remote.version > version);
        }
    }

    protected static Logger logger = Logger.getLogger(EndpointState.class.getCanonicalName());

    private volatile HeartBeat heartbeat;
    /* fields below do not get serialized */
    private volatile long update = System.currentTimeMillis();
    private volatile boolean isAlive = true;

    // whether this endpoint has token associated with it or not. Initially set false for all
    // endpoints. After certain time of inactivity, gossiper will examine if this node has a
    // token or not and will set this true if token is found. If there is no token, this is a
    // fat client and will be removed automatically from gossip.
    private volatile boolean hasToken = false;
    private final PhiAccrualFailureDetector fd = new PhiAccrualFailureDetector();

    EndpointState(int generation) {
        heartbeat = new HeartBeat(generation);
    }

    public EndpointState(HeartBeat heartBeatState) {
        heartbeat = heartBeatState;
    }

    public boolean getHasToken() {
        return hasToken;
    }

    public boolean interpret(double convictThreshold) {
        return fd.phi() > convictThreshold;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setHasToken(boolean value) {
        hasToken = value;
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

    private void update() {
        update = System.currentTimeMillis();
    }

    public int getHeartBeatVersion() {
        return heartbeat.version;
    }

    public int getGeneration() {
        return heartbeat.generation;
    }

    public void setHeartbeatVersion(int newVersion) {
        heartbeat.version = newVersion;
    }
    
    public void record(EndpointState remote) {
        if (heartbeat.record(remote.heartbeat)) {
            fd.record();
        }
    }
}
