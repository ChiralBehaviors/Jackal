/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen.dns.messaging.deserializer;

public class TruncatedDnsMessageException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public TruncatedDnsMessageException() {
        super("DNS response message is truncated");
    }
}
