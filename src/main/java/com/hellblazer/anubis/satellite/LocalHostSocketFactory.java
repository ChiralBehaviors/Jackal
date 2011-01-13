package com.hellblazer.anubis.satellite;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

public class LocalHostSocketFactory implements RMIServerSocketFactory,
        RMIClientSocketFactory, Serializable {
    private static final long serialVersionUID = 1L;
    private int backlog = 10;
    private final static InetAddress LOOPBACK;

    static {
        try {
            LOOPBACK = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            throw new IllegalStateException(
                                            "Unable to get local loopback address",
                                            e);
        }
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress("127.0.0.1", port), backlog);
        return serverSocket;
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        InetAddress address = InetAddress.getByName(host);
        if (!address.equals(LOOPBACK)) {
            throw new IllegalArgumentException("Must be local host: " + host);
        }

        return new Socket(address, port);
    }
}
