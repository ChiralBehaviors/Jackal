package com.hellblazer.jackal.gossip.fd;

import com.hellblazer.jackal.gossip.FailureDetector;
import com.hellblazer.jackal.gossip.FailureDetectorFactory;

public class SimpleTimeoutFailureDetectorFactory implements
        FailureDetectorFactory {

    private final long timeout;

    public SimpleTimeoutFailureDetectorFactory(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public FailureDetector create() {
        return new SimpleTimeoutFailureDector(timeout);
    }

}
