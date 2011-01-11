package com.hellblazer.anubis.logging;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class AsynchronousLoggerFactory {
    private final Executor executor;

    public AsynchronousLoggerFactory(Executor executor) {
        this.executor = executor;
    }

    public Log wrap(Logger log) {
        return new AsynchronousLogger(log, executor);
    }
}
