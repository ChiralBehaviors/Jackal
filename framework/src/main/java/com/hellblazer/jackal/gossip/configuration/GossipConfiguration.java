/** 
 * (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.jackal.gossip.configuration;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.leader.LeaderProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.PartitionProtocol;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.NoSecurityImpl;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.gossip.FailureDetectorFactory;
import com.hellblazer.jackal.gossip.Gossip;
import com.hellblazer.jackal.gossip.GossipCommunications;
import com.hellblazer.jackal.gossip.GossipHeartbeatProtocolFactory;
import com.hellblazer.jackal.gossip.SystemView;
import com.hellblazer.jackal.gossip.fd.AdaptiveFailureDetectorFactory;
import com.hellblazer.jackal.gossip.fd.PhiFailureDetectorFactory;
import com.hellblazer.jackal.gossip.fd.SimpleTimeoutFailureDetectorFactory;
import com.hellblazer.jackal.gossip.fd.TimedFailureDetectorFactory;
import com.hellblazer.jackal.gossip.udp.UdpCommunications;
import com.hellblazer.jackal.partition.test.node.ControllerAgent;
import com.hellblazer.partition.comms.ConnectionServerFactory;
import com.hellblazer.pinkie.SocketOptions;

/**
 * Basic gossip based discovery/replication Anubis configuration.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
@Configuration
public class GossipConfiguration {

    private static final Logger log = Logger.getLogger(GossipConfiguration.class.getCanonicalName());

    @Bean
    public GossipCommunications communications() throws IOException {
        ThreadFactory threadFactory = new ThreadFactory() {
            int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(
                                      r,
                                      String.format("Gossip comm for node %s #%s",
                                                    node(), count++));
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.SEVERE,
                                String.format("Exception on %s", t), e);
                    }
                });
                return t;
            }
        };
        ExecutorService executor = Executors.newCachedThreadPool(threadFactory);
        return new UdpCommunications(gossipEndpoint(), executor, 20, 4);
    }

    @Bean
    public ConnectionSet connectionSet() throws Exception {
        return new ConnectionSet(contactAddress(), partitionIdentity(),
                                 heartbeatCommsFactory(),
                                 connectionServerFactory(),
                                 leaderProtocolFactory(),
                                 heartbeatProtocolFactory(),
                                 partitionProtocol(), heartbeatInterval(),
                                 heartbeatTimeout(), false);
    }

    @Bean
    public FailureDetectorFactory failureDetectorFactory() {
        return simpleTimeoutFailureDetectorFactory();
    }

    @Bean
    public Gossip gossip() throws IOException {
        return new Gossip(systemView(), new SecureRandom(), communications(),
                          gossipInterval(), gossipIntervalTimeUnit(),
                          failureDetectorFactory(), partitionIdentity());
    }

    @Bean
    public HeartbeatProtocolFactory heartbeatProtocolFactory()
                                                              throws IOException {
        // return new TimedProtocolFactory();
        return new GossipHeartbeatProtocolFactory(gossip());
    }

    @Bean
    public AnubisLocator locator() {
        return new Locator(partitionIdentity(), partition(),
                           heartbeatInterval(), heartbeatTimeout());
    }

    @Bean
    public PartitionManager partition() {
        PartitionManager partition = new PartitionManager(partitionIdentity());
        return partition;
    }

    @Bean
    public Identity partitionIdentity() {
        return new Identity(getMagic(), node(), System.currentTimeMillis());
    }

    @Bean
    public PartitionProtocol partitionProtocol() {
        PartitionProtocol protocol = new PartitionProtocol(partitionIdentity(),
                                                           partition());
        return protocol;
    }

    @Bean
    public SystemView systemView() throws IOException {
        return new SystemView(new SecureRandom(),
                              communications().getLocalAddress(), seedHosts(),
                              quarantineDelay(), unreachableNodeDelay());
    }

    @Bean
    public ControllerAgent controller() throws Exception {
        if (getTestable()) {
            return new ControllerAgent(contactAddress(), partition(), node(),
                                       connectionSet(), socketOptions(),
                                       wireSecurity(), testMgrExecutor());
        } else {
            return null;
        }
    }

    protected ExecutorService testMgrExecutor() {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            int count = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(
                                      r,
                                      String.format("Test mgr comm for node %s #%s",
                                                    node(), count++));
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.SEVERE,
                                String.format("Exception on %s", t), e);
                    }
                });
                return t;
            }
        });
    }

    @Bean
    public WireSecurity wireSecurity() {
        return new NoSecurityImpl();
    }

    protected FailureDetectorFactory adaptiveAccrualFailureDetectorFactory() {
        return new AdaptiveFailureDetectorFactory(0.90, 1000, 0.45,
                                                  heartbeatInterval()
                                                          * heartbeatTimeout(),
                                                  3, 100);
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

    protected boolean getTestable() {
        return true;
    }

    protected InetSocketAddress gossipEndpoint() throws UnknownHostException {
        return new InetSocketAddress("127.0.0.1", 0);
    }

    protected int gossipInterval() {
        return 500;
    }

    protected TimeUnit gossipIntervalTimeUnit() {
        return TimeUnit.MILLISECONDS;
    }

    protected HeartbeatCommsFactory heartbeatCommsFactory() throws IOException {
        return gossip();
    }

    protected long heartbeatInterval() {
        return 2000L;
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

    protected ExecutorService commExecutor() {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable target) {
                Thread t = new Thread(target, String.format("I/O exec for %s",
                                                            node()));
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.SEVERE,
                                String.format("Exception on %s", t), e);
                    }
                });
                // t.setPriority(Thread.MAX_PRIORITY);
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
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected FailureDetectorFactory phiAccrualFailureDetectorFactory() {
        return new PhiFailureDetectorFactory(3.5, 1000,
                                             2 * heartbeatInterval(), 3, 100,
                                             false);
    }

    protected int quarantineDelay() {
        return (int) (heartbeatInterval() * (heartbeatTimeout() + 1));
    }

    protected Collection<InetSocketAddress> seedHosts()
                                                       throws UnknownHostException {
        return asList(gossipEndpoint());
    }

    protected FailureDetectorFactory simpleTimeoutFailureDetectorFactory() {
        return new SimpleTimeoutFailureDetectorFactory(heartbeatTimeout()
                                                       * heartbeatInterval()
                                                       * 3);
    }

    protected FailureDetectorFactory timedFailureDetectorFactory() {
        return new TimedFailureDetectorFactory(heartbeatInterval()
                                               * heartbeatTimeout());
    }

    protected int unreachableNodeDelay() {
        return 500000;
    }
}
