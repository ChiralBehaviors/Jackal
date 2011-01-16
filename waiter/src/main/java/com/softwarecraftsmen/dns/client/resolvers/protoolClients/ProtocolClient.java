package com.softwarecraftsmen.dns.client.resolvers.protoolClients;

import java.io.IOException;

public interface ProtocolClient {
    public static final byte[] EmptyByteArray = new byte[] {};

    void close();

    byte[] sendAndReceive(byte[] sendData) throws IOException;
}
