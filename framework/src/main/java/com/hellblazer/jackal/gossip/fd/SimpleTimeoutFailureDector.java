package com.hellblazer.jackal.gossip.fd;

import com.hellblazer.jackal.gossip.FailureDetector;

public class SimpleTimeoutFailureDector implements FailureDetector {
    private final long    timeout;
    private volatile long lastRecord = -1;

    public SimpleTimeoutFailureDector(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void record(long now, long delay) {
        lastRecord = now + delay;
    }

    @Override
    public boolean shouldConvict(long now) {
        if (lastRecord < 0) {
            return false;
        }
        return now - lastRecord > timeout;
    }

}
