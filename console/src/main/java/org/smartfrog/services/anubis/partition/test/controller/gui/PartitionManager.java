package org.smartfrog.services.anubis.partition.test.controller.gui;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.gossip.configuration.ControllerGossipConfiguration;

@Configuration
public class PartitionManager extends ControllerGossipConfiguration {
    static final String                          ENDPOINT       = "endpoint";
    private static final String                  SEED           = "seed";
    static final String                          PROP_FILE_NAME = "load.properties";

    private static Collection<InetSocketAddress> seedHosts      = new ArrayList<InetSocketAddress>();
    private static InetAddress                   contactAddress;

    @Override
    protected Collection<InetSocketAddress> seedHosts()
                                                       throws UnknownHostException {
        return seedHosts;
    }

    @Override
    protected Controller constructController() throws IOException {
        return new GraphicController(partitionIdentity(), heartbeatTimeout(),
                                     heartbeatInterval(), socketOptions(),
                                     dispatchExecutor(), wireSecurity());
    }

    public static void main(String[] argv) throws Exception {
        FileInputStream fis = new FileInputStream(PROP_FILE_NAME);
        Properties props = new Properties();
        props.load(fis);

        for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
            for (Enumeration<InetAddress> addresses = ifaces.nextElement().getInetAddresses(); addresses.hasMoreElements();) {
                InetAddress address = addresses.nextElement();
                if (address.isAnyLocalAddress() || address.isLinkLocalAddress()
                    || address.isLoopbackAddress()) {
                    continue;
                }
                contactAddress = address;
                break;
            }
            if (contactAddress != null) {
                break;
            }
        }
        if (contactAddress == null) {
            System.out.println("Could not find a non local address for this machine");
            System.exit(1);
        }

        for (Object k : props.keySet()) {
            String key = (String) k;
            if (key.startsWith(SEED)) {
                seedHosts.add(from(props.getProperty(key)));
            }
        }

        @SuppressWarnings("unused")
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                                                                                        PartitionManager.class);
        while (true) {
            Thread.sleep(30000);
        }
    }

    static InetSocketAddress from(String endpoint) {
        StringTokenizer tokes = new StringTokenizer(endpoint, ":");
        if (tokes.hasMoreTokens()) {
            String host = tokes.nextToken();
            if (tokes.hasMoreTokens()) {
                int port = Integer.parseInt(tokes.nextToken());
                return new InetSocketAddress(host, port);
            }
            throw new IllegalArgumentException("No port: " + endpoint);
        }
        throw new IllegalArgumentException("Invalid host:port specification: "
                                           + endpoint);
    }
}
