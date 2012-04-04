package org.smartfrog.services.anubis.partition.test.controller.gui;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import com.hellblazer.jackal.configuration.GossipHeartbeatAndDiscoveryConfig;
import com.hellblazer.jackal.configuration.GossipSnoopConfig;

@Configuration
@Import({ GraphicControllerConfig.class, GossipSnoopConfig.class,
         GossipHeartbeatAndDiscoveryConfig.class })
public class PartitionManager {
    private static final Logger                  log            = LoggerFactory.getLogger(PartitionManager.class.getCanonicalName());
    static final String                          ENDPOINT       = "endpoint";
    private static final String                  SEED           = "seed";
    static final String                          PROP_FILE_NAME = "load.properties";

    private static Collection<InetSocketAddress> seedHosts      = new ArrayList<InetSocketAddress>();
    private static InetAddress                   contactAddress;

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

    @Bean(name = "gossipDispatchers")
    @Lazy
    @Autowired
    public ExecutorService gossipDispatchers(Identity partitionIdentity) {
        final int id = partitionIdentity.id;
        return Executors.newCachedThreadPool(new ThreadFactory() {
            int count = 0;

            @Override
            public Thread newThread(Runnable target) {
                Thread t = new Thread(
                                      target,
                                      String.format("Gossip Dispatcher[%s] for node[%s]",
                                                    count++, id));
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.error(String.format("Exception on %s", t), e);
                    }
                });
                return t;
            }
        });
    }

    @Bean(name = "gossipEndpoint")
    public InetSocketAddress gossipEndpoint() {
        return new InetSocketAddress(0);
    }

    @Bean
    public Identity partitionIdentity() {
        return new Identity(getMagic(), node(), System.currentTimeMillis());
    }

    protected int getMagic() {
        try {
            return Identity.getMagicFromLocalIpAddress();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected int node() {
        return 2047;
    }

    @Bean(name = "seedHosts")
    protected Collection<InetSocketAddress> seedHosts()
                                                       throws UnknownHostException {
        return seedHosts;
    }
}
