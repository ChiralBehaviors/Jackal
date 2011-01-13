package com.hellblazer.anubis.satellite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.subprocess.SPLocatorImpl;

import sun.rmi.server.UnicastServerRef2;

import com.hellblazer.anubis.util.Base64Coder;

public class Launch {
    private int port;
    private SPLocatorImpl locator;
    private String configPackage;
    private Process satellite;
    private RMIClientSocketFactory clientFactory;
    private RMIServerSocketFactory serverFactory;

    public void setConfigPackage(String configPackage) {
        this.configPackage = configPackage;
    }

    public AnubisLocator getLocator() throws IOException {
        launchProcess();
        return locator;
    }

    @PreDestroy
    public void shutDown() {
        satellite.destroy();
    }

    private void launchProcess() throws IOException {
        List<String> command = new ArrayList<String>();
        command.add(export());
        command.add(configPackage);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        satellite = builder.start();
    }

    private String export() throws IOException {
        UnicastServerRef2 reference = new UnicastServerRef2(port,
                                                            clientFactory,
                                                            serverFactory);
        Remote stub = reference.exportObject(new Handshake(), null, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(stub);
        oos.close();
        return Base64Coder.encodeLines(baos.toByteArray());
    }

    private class Handshake implements Remote {

    }
}
