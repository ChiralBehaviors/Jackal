/**
 * This file is Copyright © 2008 Software Craftsmen Limited. All Rights Reserved.
 */
package com.softwarecraftsmen;

public class CanNeverHappenException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public CanNeverHappenException() {
    }

    public CanNeverHappenException(final Exception cause) {
        super(cause);
    }
}
