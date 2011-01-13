package com.hellblazer.anubis.satellite;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapter;
import org.smartfrog.services.anubis.locator.subprocess.SPLocatorImpl;

public class Launch {
    private static Logger log = Logger.getLogger(Launch.class.getCanonicalName());

    private long launchTimeout = 30L;
    private TimeUnit launchTimeoutUnit = TimeUnit.SECONDS;
    SPLocatorImpl locator;
    private String configPackage;
    private Process satellite;
    RMIClientSocketFactory clientFactory = new LocalHostSocketFactory();
    RMIServerSocketFactory serverFactory = new LocalHostSocketFactory();
    CyclicBarrier barrier;
    long period;
    long timeout;
    private Thread ioPump;
    Registry localRegistry;

    public void setConfigPackage(String configPackage) {
        this.configPackage = configPackage;
    }

    public AnubisLocator getLocator() throws Exception {
        localRegistry = LocateRegistry.createRegistry(1099);

        Handshake handshake = new HandshakeImpl();
        Remote stub = UnicastRemoteObject.exportObject(handshake, 0);
        String name = UUID.randomUUID().toString();
        localRegistry.bind(name, stub);

        locator = new SPLocatorImpl();
        stub = UnicastRemoteObject.exportObject(locator, 0);

        barrier = new CyclicBarrier(2);

        launchSatellite(name);

        barrier.await(launchTimeout, launchTimeoutUnit);

        locator.deploy();
        locator.start();

        localRegistry.unbind(name);

        return locator;
    }

    @PreDestroy
    public void shutDown() {
        if (satellite != null) {
            try {
                satellite.getErrorStream().close();
                satellite.getInputStream().close();
                satellite.getOutputStream().close();
            } catch (IOException e) {
                // ignore
            }
            satellite.destroy();
        }
        if (locator != null) {
            locator.terminate();
        }
    }

    private void launchSatellite(String name) throws IOException {
        List<String> command = new ArrayList<String>();
        command.add(getJavaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Bootstrap.class.getCanonicalName());
        command.add(configPackage);
        command.add(name);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        satellite = builder.start();
        ioPump = new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream is = satellite.getInputStream();
                byte[] buffer = new byte[4096];
                try {
                    for (int read = is.read(buffer); read != -1; read = is.read(buffer)) {
                        System.out.write(buffer, 0, read);
                    }
                } catch (IOException e) {
                    log.log(Level.WARNING,
                            "Exception while pumping IO for process: "
                                    + satellite, e);
                }
            }
        }, "Anubis: Satellite process IO pump");
        ioPump.start();
    }

    private String getJavaExecutable() {
        String pathSeparator = System.getProperty("file.separator");
        String javaBin = System.getProperty("java.home");
        if (javaBin.endsWith(pathSeparator)) {
            javaBin += "bin";
        } else {
            javaBin += pathSeparator + "bin";
        }
        javaBin += pathSeparator;
        String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("win") >= 0) ? javaBin + "java.exe" : javaBin
                                                                 + "java";
    }

    class HandshakeImpl implements Handshake {
        /* (non-Javadoc)
         * @see com.hellblazer.anubis.satellite.Handshake#setAdapter(org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapter)
         */
        @Override
        public void setAdapter(SPLocatorAdapter adapter) throws RemoteException {
            locator.setPeriod(period);
            locator.setTimeout(timeout);
            locator.setAdapter(adapter);
            try {
                barrier.await();
            } catch (InterruptedException e) {
                return;
            } catch (BrokenBarrierException e) {
                return;
            }
        }
    }
}
