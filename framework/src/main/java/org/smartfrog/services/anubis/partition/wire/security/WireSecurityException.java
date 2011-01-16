package org.smartfrog.services.anubis.partition.wire.security;

public class WireSecurityException extends SecurityException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public WireSecurityException(String msg) {
        super(msg);
    }

    public WireSecurityException(String msg, Throwable thr) {
        super(msg, thr);
    }

}
