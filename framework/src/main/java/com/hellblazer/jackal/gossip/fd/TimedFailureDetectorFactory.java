package com.hellblazer.jackal.gossip.fd;

import com.hellblazer.jackal.gossip.FailureDetector;
import com.hellblazer.jackal.gossip.FailureDetectorFactory;

public class TimedFailureDetectorFactory implements FailureDetectorFactory {
    final long maxInterval;

    public TimedFailureDetectorFactory(long maxInterval) {
        this.maxInterval = maxInterval;
    }

    @Override
    public FailureDetector create() {
        return new TimedFailureDetector(maxInterval);
    }

}
