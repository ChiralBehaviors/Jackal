package com.hellblazer.anubis.satellite;

import java.io.IOException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;

import javax.annotation.PostConstruct;

import org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapter;
import org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapterImpl;

public class SPLocatorAdapterExporter {
    private SPLocatorAdapterImpl adapter;
    private RMIClientSocketFactory clientFactory;
    private RMIServerSocketFactory serverFactory;
    private SPLocatorAdapter stub;

    public void setClientFactory(RMIClientSocketFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public void setServerFactory(RMIServerSocketFactory serverFactory) {
        this.serverFactory = serverFactory;
    }

    public void setAdapter(SPLocatorAdapterImpl adapter) {
        this.adapter = adapter;
    }

    @PostConstruct
    public void export() throws IOException {
        stub = (SPLocatorAdapter) UnicastRemoteObject.exportObject(adapter,
                                                                   0,
                                                                   clientFactory,
                                                                   serverFactory);
    }

    public SPLocatorAdapter getAdapter() {
        return stub;
    }
}