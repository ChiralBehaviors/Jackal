package org.smartfrog.services.anubis.load;

import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;

import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.gossip.configuration.GossipConfiguration;

@Configuration
public class Load extends GossipConfiguration {
    static final int                             WAIT_TIME      = 1500;
    static final String                          STATE_NAME     = "Whip It Good";
    static final String                          PROP_FILE_NAME = "load.properties";
    static final String                          ENDPOINT       = "endpoint";
    private static final String                  SEED           = "seed";

    private static Collection<InetSocketAddress> seedHosts      = new ArrayList<InetSocketAddress>();
    private static InetSocketAddress             gossipEndpoint;
    private static InetSocketAddress             contactEndpoint;
    private static int                           id;

    @Override
    protected Collection<InetSocketAddress> seedHosts()
                                                       throws UnknownHostException {
        return seedHosts;
    }

    @Override
    protected InetSocketAddress gossipEndpoint() throws UnknownHostException {
        return gossipEndpoint;
    }

    @Override
    protected InetSocketAddress contactAddress() throws UnknownHostException {
        return contactEndpoint;
    }

    @Override
    protected int node() {
        if (id == -1) {
            return super.node();
        }
        return id;
    }

    public static void main(String[] argv) throws Exception {
        FileInputStream fis = new FileInputStream(PROP_FILE_NAME);
        Properties props = new Properties();
        props.load(fis);
        String idString = props.getProperty("id");
        if (idString != null) {
            id = Integer.parseInt(idString);
        } else {
            id = -1;
        }
        gossipEndpoint = null;
        InetSocketAddress localAddress = from(props.getProperty(ENDPOINT));
        for (Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
            for (Enumeration<InetAddress> addresses = ifaces.nextElement().getInetAddresses(); addresses.hasMoreElements();) {
                InetAddress address = addresses.nextElement();
                if (address.isAnyLocalAddress() || address.isLinkLocalAddress()
                    || address.isLoopbackAddress()) {
                    continue;
                }
                gossipEndpoint = new InetSocketAddress(address,
                                                       localAddress.getPort());
                break;
            }
            if (gossipEndpoint != null) {
                break;
            }
        }
        if (gossipEndpoint == null) {
            System.out.println("Could not find a non local address for "
                               + localAddress.getHostName());
            System.exit(1);
        }
        System.out.println("My gossip endpoint: " + gossipEndpoint);

        contactEndpoint = new InetSocketAddress(gossipEndpoint.getAddress(), 0);

        for (Object k : props.keySet()) {
            String key = (String) k;
            if (key.startsWith(SEED)) {
                seedHosts.add(from(props.getProperty(key)));
            }
        }

        String stateName = STATE_NAME;
        int waitTime = WAIT_TIME;

        Gate gate = new Gate();
        gate.close();

        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                                                                                        Load.class);
        AnubisLocator locator = ctx.getBean(AnubisLocator.class);

        AnubisProvider provider = new AnubisProvider(stateName);
        provider.setValue(0);

        locator.registerProvider(provider);
        locator.registerListener(new Receiver(stateName, provider.getInstance()));
        locator.registerStability(new Stability(gate, provider.getInstance()));

        Thread senderThread = new Thread(new Sender(gate, provider, waitTime),
                                         "Sender thread");
        senderThread.start();

        while (true) {
            Thread.sleep(40000);
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
