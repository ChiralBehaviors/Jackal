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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.nonblocking.MessageNioServerFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.leader.LeaderProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.PartitionProtocol;
import org.smartfrog.services.anubis.partition.test.node.TestMgr;
import org.smartfrog.services.anubis.partition.util.Epoch;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.NoSecurityImpl;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.annotations.DeployedPostProcessor;
import com.hellblazer.jackal.gossip.FailureDetectorFactory;
import com.hellblazer.jackal.gossip.Gossip;
import com.hellblazer.jackal.gossip.GossipCommunications;
import com.hellblazer.jackal.gossip.GossipHeartbeatProtocolFactory;
import com.hellblazer.jackal.gossip.SystemView;
import com.hellblazer.jackal.gossip.fd.AdaptiveFailureDetectorFactory;
import com.hellblazer.jackal.gossip.fd.PhiFailureDetectorFactory;
import com.hellblazer.jackal.gossip.fd.TimedFailureDetectorFactory;
import com.hellblazer.jackal.gossip.udp.UdpCommunications;

/**
 * Basic gossip based discovery/replication Anubis configuration.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
@Configuration
public class GossipConfiguration {

    @Bean
    public GossipCommunications communications() throws IOException {
        return new UdpCommunications(gossipEndpoint(),
                                     Executors.newFixedThreadPool(3));
    }

    @Bean
    public ConnectionSet connectionSet() throws Exception {
        return new ConnectionSet(contactAddress(), partitionIdentity(),
                                 heartbeatCommsFactory(),
                                 ioConnectionServerFactory(),
                                 leaderProtocolFactory(),
                                 heartbeatProtocolFactory(),
                                 partitionProtocol(), heartbeatInterval(),
                                 heartbeatTimeout(), false, alwaysReconnect());
    }

    public InetAddress contactHost() throws UnknownHostException {
        return InetAddress.getLocalHost();
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
    public FailureDetectorFactory failureDetectorFactory() {
        return adaptiveAccrualFailureDetectorFactory();
    }

    @Bean
    public Gossip gossip() throws IOException {
        return new Gossip(systemView(), new SecureRandom(),
                          (int) heartbeatInterval(), communications(),
                          gossipInterval(), gossipIntervalTimeUnit(),
                          failureDetectorFactory());
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
        return new Identity(getMagic(), node(), epoch().longValue());
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
    public TestMgr testMgr() throws Exception {
        TestMgr mgr = new TestMgr(contactHost().getCanonicalHostName(),
                                  contactPort(), partition(), node());
        mgr.setConnectionAddress(contactAddress());
        mgr.setConnectionSet(connectionSet());
        mgr.setIdentity(partitionIdentity());
        mgr.setTestable(getTestable());
        return mgr;
    }

    @Bean
    public WireSecurity wireSecurity() {
        return new NoSecurityImpl();
    }

    protected FailureDetectorFactory adaptiveAccrualFailureDetectorFactory() {
        return new AdaptiveFailureDetectorFactory(0.90, 1000, 0.5,
                                                  heartbeatInterval()
                                                          * heartbeatTimeout(),
                                                  20, 100);
    }

    protected boolean alwaysReconnect() {
        return true;
    }

    protected InetSocketAddress contactAddress() throws UnknownHostException {
        return new InetSocketAddress(contactHost(), contactPort());
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
        return new InetSocketAddress(contactHost(), 0);
    }

    protected int gossipInterval() {
        return 1;
    }

    protected TimeUnit gossipIntervalTimeUnit() {
        return TimeUnit.SECONDS;
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

    protected IOConnectionServerFactory ioConnectionServerFactory()
                                                                   throws Exception {
        MessageNioServerFactory factory = new MessageNioServerFactory();
        factory.setWireSecurity(wireSecurity());
        return factory;
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

    protected FailureDetectorFactory timedFailureDetectorFactory() {
        return new TimedFailureDetectorFactory(heartbeatInterval()
                                               * heartbeatTimeout());
    }

    protected int unreachableNodeDelay() {
        return 500000;
    }
}
