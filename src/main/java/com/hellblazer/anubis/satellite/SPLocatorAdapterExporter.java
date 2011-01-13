package com.hellblazer.anubis.satellite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;

import javax.annotation.PostConstruct;

import org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapterImpl;

import sun.rmi.server.UnicastServerRef2;

import com.hellblazer.anubis.util.Base64Coder;

public class SPLocatorAdapterExporter {
    private int port;
    private SPLocatorAdapterImpl adapter;
    private RMIClientSocketFactory clientFactory;
    private RMIServerSocketFactory serverFactory;
    private String ref;

    public void setPort(int port) {
        this.port = port;
    }

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
        UnicastServerRef2 reference = new UnicastServerRef2(port,
                                                            clientFactory,
                                                            serverFactory);
        Remote stub = reference.exportObject(adapter, null, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(stub);
        oos.close();
        ref = Base64Coder.encodeLines(baos.toByteArray());
    }

    public String getRef() {
        return ref;
    }
}