package com.hellblazer.anubis.basiccomms.nio;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServer;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

public class MessageConnectionServerFactory implements
        IOConnectionServerFactory {
    private WireSecurity wireSecurity;
    private SocketOptions options;
    private int selectTimeout;
    private ExecutorService executor;

    @Override
    public IOConnectionServer create(ConnectionAddress address, Identity id,
                                     ConnectionSet cs) throws IOException {
        ServerChannelHandler handler = new ServerChannelHandler();
        handler.setEndpoint(address.asSocketAddress());
        handler.setOptions(options);
        handler.setWireSecurity(wireSecurity);
        handler.setSelectTimeout(selectTimeout);
        handler.setExecutor(executor);
        handler.setIdentity(id);
        handler.setConnectionSet(cs);
        return handler;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public SocketOptions getOptions() {
        return options;
    }

    public int getSelectTimeout() {
        return selectTimeout;
    }

    public WireSecurity getWireSecurity() {
        return wireSecurity;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void setOptions(SocketOptions options) {
        this.options = options;
    }

    public void setSelectTimeout(int selectTimeout) {
        this.selectTimeout = selectTimeout;
    }

    public void setWireSecurity(WireSecurity wireSecurity) {
        this.wireSecurity = wireSecurity;
    }
}
