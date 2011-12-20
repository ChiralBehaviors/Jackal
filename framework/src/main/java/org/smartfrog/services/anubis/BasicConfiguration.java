package org.smartfrog.services.anubis;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.MulticastHeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.timed.TimedProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.leader.LeaderProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.PartitionProtocol;
import org.smartfrog.services.anubis.partition.util.Epoch;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.NoSecurityImpl;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.annotations.DeployedPostProcessor;
import com.hellblazer.jackal.partition.test.node.Controller;
import com.hellblazer.partition.comms.ConnectionServerFactory;
import com.hellblazer.pinkie.SocketOptions;

@Configuration
public class BasicConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(BasicConfiguration.class);
    }

    @Bean
    public ConnectionSet connectionSet() throws Exception {
        return new ConnectionSet(contactAddress(), partitionIdentity(),
                                 heartbeatCommsFactory(),
                                 connectionServerFactory(),
                                 leaderProtocolFactory(),
                                 heartbeatProtocolFactory(),
                                 partitionProtocol(), heartbeatInterval(),
                                 heartbeatTimeout(), false, alwaysReconnect());
    }

    @Bean
    public DeployedPostProcessor deployedPostProcessor() {
        return new DeployedPostProcessor();
    }

    @Bean
    public Epoch epoch() {
        return new Epoch();
    }

    @Bean
    public AnubisLocator locator() {
        Locator locator = new Locator(partitionIdentity(), partition(),
                                      heartbeatInterval(), heartbeatTimeout());
        return locator;
    }

    @Bean
    public PartitionManager partition() {
        PartitionManager partition = new PartitionManager(partitionIdentity());
        return partition;
    }

    @Bean
    public Identity partitionIdentity() {
        return new Identity(getMagic(), node(), epoch().longValue());
    }

    @Bean
    public PartitionProtocol partitionProtocol() {
        PartitionProtocol protocol = new PartitionProtocol(partitionIdentity(),
                                                           partition());
        return protocol;
    }

    @Bean
    public Controller controller() throws Exception {
        if (isControllable()) {
            return new Controller(contactAddress(), partition(), node(),
                                  connectionSet(), socketOptions(),
                                  wireSecurity(), testMgrExecutor());
        } else {
            return null;
        }
    }

    protected Executor testMgrExecutor() {
        return Executors.newCachedThreadPool();
    }

    @Bean
    public WireSecurity wireSecurity() {
        return new NoSecurityImpl();
    }

    protected boolean alwaysReconnect() {
        return false;
    }

    protected InetSocketAddress contactAddress() throws UnknownHostException {
        return new InetSocketAddress("127.0.0.1", contactPort());
    }

    protected int contactPort() {
        return 0;
    }

    protected int getMagic() {
        return 12345;
    }

    protected boolean isControllable() {
        return true;
    }

    protected HeartbeatCommsFactory heartbeatCommsFactory()
                                                           throws UnknownHostException {
        return new MulticastHeartbeatCommsFactory(wireSecurity(),
                                                  heartbeatGroup(),
                                                  contactAddress(),
                                                  partitionIdentity());
    }

    protected MulticastAddress heartbeatGroup() throws UnknownHostException {
        return new MulticastAddress(heartbeatGroupMulticastAddress(),
                                    heartbeatGroupPort(), heartbeatGroupTTL());
    }

    protected InetAddress heartbeatGroupMulticastAddress()
                                                          throws UnknownHostException {
        return InetAddress.getByName("233.1.2.30");
    }

    protected int heartbeatGroupPort() {
        return 1966;
    }

    protected int heartbeatGroupTTL() {
        return 1;
    }

    protected long heartbeatInterval() {
        return 3000L;
    }

    protected HeartbeatProtocolFactory heartbeatProtocolFactory() {
        return new TimedProtocolFactory();
    }

    protected long heartbeatTimeout() {
        return 3L;
    }

    protected SocketOptions socketOptions() {
        SocketOptions socketOptions = new SocketOptions();
        socketOptions.setBacklog(100);
        socketOptions.setTimeout(1 * 1000);
        return socketOptions;
    }

    protected IOConnectionServerFactory connectionServerFactory()
                                                                 throws Exception {
        return new ConnectionServerFactory(wireSecurity(), socketOptions(),
                                           commExecutor());
    }

    protected Executor commExecutor() {
        return Executors.newFixedThreadPool(4, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable target) {
                Thread t = new Thread(target, String.format("I/O exec for %s",
                                                            node()));
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(Thread arg0, Throwable arg1) {
                        arg1.printStackTrace();
                    }
                });
                t.setPriority(Thread.MAX_PRIORITY);
                return t;
            }
        });
    }

    protected LeaderProtocolFactory leaderProtocolFactory() {
        return new LeaderProtocolFactory();
    }

    protected int node() {
        try {
            return Identity.getProcessUniqueId();
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }
}
