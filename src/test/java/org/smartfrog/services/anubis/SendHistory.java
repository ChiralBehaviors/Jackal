package org.smartfrog.services.anubis;

public class SendHistory {
    public final long timeRef;
    public final Object value;

    public SendHistory(long timeRef, Object value) {
        this.timeRef = timeRef;
        this.value = value;
    }
}