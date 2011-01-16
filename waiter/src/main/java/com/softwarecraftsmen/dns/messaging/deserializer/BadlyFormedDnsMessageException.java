package com.softwarecraftsmen.dns.messaging.deserializer;

public final class BadlyFormedDnsMessageException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public BadlyFormedDnsMessageException(final String message) {
        super(message);
    }

    public BadlyFormedDnsMessageException(final String message,
                                          final Exception exception) {
        super(message, exception);
    }
}
