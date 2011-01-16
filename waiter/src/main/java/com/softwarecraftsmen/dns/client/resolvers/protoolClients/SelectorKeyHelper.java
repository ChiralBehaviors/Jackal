package com.softwarecraftsmen.dns.client.resolvers.protoolClients;

import java.io.IOException;
import java.nio.channels.SelectionKey;

public class SelectorKeyHelper {
    private final SelectionKey key;
    private final int numberOfRetries;
    private final int blockInMilliseconds;

    public SelectorKeyHelper(final SelectionKey key,
                             final int blockInMilliseconds,
                             final int numberOfRetries) {
        if (blockInMilliseconds < 0) {
            throw new IllegalArgumentException(
                                               "blockInMilliseconds can not be negative");
        }
        this.key = key;
        this.numberOfRetries = numberOfRetries;
        this.blockInMilliseconds = blockInMilliseconds;
    }

    public void blockUntilReady(final int operationCode) throws IOException {
        key.interestOps(operationCode);
        int retryCount = 0;
        try {
            while (!((key.readyOps() & operationCode) != 0)
                   && retryCount < numberOfRetries) {
                block();
                retryCount++;
            }
        } finally {
            resetKey();
        }
    }

    private boolean block() throws IOException {
        int numberOfKeysSelected = 0;
        if (blockInMilliseconds > 0) {
            numberOfKeysSelected = key.selector().select(blockInMilliseconds);
        } else if (blockInMilliseconds == 0) {
            numberOfKeysSelected = key.selector().selectNow();
        }
        return numberOfKeysSelected == 0;
    }

    private void resetKey() {
        if (key.isValid()) {
            key.interestOps(0);
        }
    }
}
