package org.smartfrog.services.anubis.partition.wire.security;

public class WireSecurityException extends SecurityException {

    public WireSecurityException(String msg) {
        super(msg);
    }
    
    public WireSecurityException(String msg, Throwable thr) {
        super(msg, thr);
    } 
    
}
