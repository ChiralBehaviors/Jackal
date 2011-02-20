package com.hellblazer.anubis.partition.coms.udp;

import java.net.InetAddress;

public abstract class Message {
    private InetAddress from;

    public InetAddress getFrom() {
        return from;
    }

    abstract public byte[] getMessageBody();

}
