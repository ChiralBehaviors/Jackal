package org.smartfrog.services.anubis.partition.test.controller.gui;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
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
    private static InetSocketAddress             endpoint;

    @Override
    protected Collection<InetSocketAddress> seedHosts()
                                                       throws UnknownHostException {
        return seedHosts;
    }

    @Override
    protected InetSocketAddress gossipEndpoint() throws UnknownHostException {
        return endpoint;
    }

    @Override
    protected Controller constructController() throws UnknownHostException {
        return new GraphicController(timer(), 1000, 300000,
                                     partitionIdentity(), heartbeatTimeout(),
                                     heartbeatInterval());
    }

    public static void main(String[] argv) throws Exception {
        FileInputStream fis = new FileInputStream(PROP_FILE_NAME);
        Properties props = new Properties();
        props.load(fis);

        endpoint = new InetSocketAddress(InetAddress.getLocalHost(), 0);
        for (Object k : props.keySet()) {
            String key = (String) k;
            if (key.startsWith(SEED)) {
                seedHosts.add(from(props.getProperty(key)));
            }
        }

        @SuppressWarnings("unused")
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                                                                                        PartitionManager.class);
        while(true) {
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
