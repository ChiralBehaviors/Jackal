package com.hellblazer.anubis.satellite;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapter;
import org.smartfrog.services.anubis.locator.subprocess.SPLocatorImpl;

import sun.rmi.server.UnicastServerRef2;

import com.hellblazer.anubis.util.Base64Coder;

@SuppressWarnings("restriction")
public class Launch {
    private long launchTimeout = 30L;
    private TimeUnit launchTimeoutUnit = TimeUnit.SECONDS;
    private SPLocatorImpl locator;
    private String configPackage;
    private Process satellite;
    private RMIClientSocketFactory clientFactory = new LocalHostSocketFactory();
    private RMIServerSocketFactory serverFactory = new LocalHostSocketFactory();
    private CyclicBarrier barrier;
    private long period;
    private long timeout;

    public void setConfigPackage(String configPackage) {
        this.configPackage = configPackage;
    }

    public AnubisLocator getLocator() throws IOException, InterruptedException,
                                     BrokenBarrierException, TimeoutException {
        barrier = new CyclicBarrier(2);
        launchSatellite();
        barrier.await(launchTimeout, launchTimeoutUnit);
        locator.deploy();
        locator.start();
        return locator;
    }

    @PreDestroy
    public void shutDown() {
        satellite.destroy();
        locator.terminate();
    }

    private void launchSatellite() throws IOException {
        List<String> command = new ArrayList<String>();
        command.add(getJavaExecutable());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(Bootstrap.class.getCanonicalName());
        command.add(exportHandshake());
        command.add(configPackage);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        satellite = builder.start();
    }

    private String exportHandshake() throws IOException {
        UnicastServerRef2 reference = new UnicastServerRef2(0, clientFactory,
                                                            serverFactory);
        Remote stub = reference.exportObject(new HandshakeImpl(), null, true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(stub);
        oos.close();
        return Base64Coder.encodeLines(baos.toByteArray());
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

    public class HandshakeImpl implements Remote, Handshake {
        /* (non-Javadoc)
         * @see com.hellblazer.anubis.satellite.Handshake#setAdapter(org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapter)
         */
        @Override
        public void setAdapter(SPLocatorAdapter adapter) throws RemoteException {
            locator = new SPLocatorImpl(0, clientFactory, serverFactory);
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
