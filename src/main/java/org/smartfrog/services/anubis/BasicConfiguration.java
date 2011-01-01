/** (C) Copyright 2010 Hal Hildebrand, all rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.smartfrog.services.anubis;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.nonblocking.MessageNioServerFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.ping.PingProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.timed.TimedProtocolFactory;
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

import com.hellblazer.anubis.annotations.DeployedPostProcessor;
import com.hellblazer.anubis.basiccomms.nio.MessageConnectionServerFactory;

@Configuration
public class BasicConfiguration {

    @Bean
    public ConnectionSet connectionSet() throws Exception {
        ConnectionSet connectionSet = new ConnectionSet();
        connectionSet.setLeaderProtocolFactory(leaderProtocolFactory());
        connectionSet.setConnectionAddress(contactAddress());
        connectionSet.setHeartbeatAddress(heartbeatGroup());
        connectionSet.setFactory(ioConnectionServerFactory());
        connectionSet.setHeartbeatCommsFactory(heartbeatCommsFactory());
        connectionSet.setHeartbeatProtocolFactory(heartbeatProtocolFactory());
        connectionSet.setIdentity(partitionIdentity());
        connectionSet.setPartitionProtocol(partitionProtocol());
        connectionSet.setTiming(heartbeatInterval(), heartbeatTimeout());
        return connectionSet;
    }

    @Bean
    public ConnectionAddress contactAddress() throws UnknownHostException {
        return new ConnectionAddress(contactHost(), contactPort());
    }

    public InetAddress contactHost() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }

    public int contactPort() {
        return 0;
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
    public HeartbeatCommsFactory heartbeatCommsFactory() {
        HeartbeatCommsFactory factory = new HeartbeatCommsFactory();
        factory.setWireSecurity(wireSecurity());
        return factory;
    }

    @Bean
    public MulticastAddress heartbeatGroup() throws UnknownHostException {
        return new MulticastAddress(heartbeatGroupMulticastAddress(),
                                    heartbeatGroupPort(), heartbeatGroupTTL());
    }

    public InetAddress heartbeatGroupMulticastAddress()
                                                       throws UnknownHostException {
        return InetAddress.getByName("233.1.2.30");
    }

    public int heartbeatGroupPort() {
        return 1966;
    }

    public int heartbeatGroupTTL() {
        return 1;
    }

    public long heartbeatInterval() {
        return 2000L;
    }

    @Bean
    public HeartbeatProtocolFactory heartbeatProtocolFactory() {
        if (useTimed()) {
            return new TimedProtocolFactory();
        }
        return new PingProtocolFactory();
    }

    public long heartbeatTimeout() {
        return 3L;
    }

    @Bean
    public IOConnectionServerFactory ioConnectionServerFactory()
                                                                throws Exception {
        if (useNewNioServer()) {
            return newNioServer();
        } else {
            return oldNioServer();
        }
    }

    @Bean
    public LeaderProtocolFactory leaderProtocolFactory() {
        return new LeaderProtocolFactory();
    }

    @Bean
    public AnubisLocator locator() throws UnknownHostException {
        Locator locator = new Locator();
        locator.setIdentity(partitionIdentity());
        locator.setPartition(partition());
        locator.setHeartbeatInterval(heartbeatInterval());
        locator.setHeartbeatTimeout(heartbeatTimeout());
        return locator;
    }

    @Bean
    public PartitionManager partition() throws UnknownHostException {
        PartitionManager partition = new PartitionManager();
        partition.setIdentity(partitionIdentity());
        return partition;
    }

    @Bean
    public Identity partitionIdentity() throws UnknownHostException {
        return new Identity(getMagic(), getNode(), epoch().longValue());
    }

    @Bean
    public PartitionProtocol partitionProtocol() throws UnknownHostException {
        PartitionProtocol protocol = new PartitionProtocol();
        protocol.setPartitionMgr(partition());
        protocol.setIdentity(partitionIdentity());
        return protocol;
    }

    public TestMgr testMgr() throws Exception {
        TestMgr mgr = new TestMgr(contactHost().getCanonicalHostName(),
                                  contactPort(), partition(), getNode());
        mgr.setConnectionAddress(contactAddress());
        mgr.setConnectionSet(connectionSet());
        mgr.setIdentity(partitionIdentity());
        mgr.setTestable(isTestable());
        return mgr;
    }

    @Bean
    public Timer timer() {
        return new Timer("Partition timer", true);
    }

    @Bean
    public WireSecurity wireSecurity() {
        return new NoSecurityImpl();
    }

    protected ExecutorService executorService() {
        int poolSize = poolSize();
        if (poolSize < 3) {
            throw new IllegalArgumentException("Pool size must be >= 3");
        }
        return Executors.newFixedThreadPool(poolSize, getTheadFactory());
    }

    protected int getMagic() {
        return 12345;
    }

    protected int getNode() throws UnknownHostException {
        return Identity.getProcessUniqueId();
    }

    protected ThreadFactory getTheadFactory() {
        return new ThreadFactory() {
            int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r,
                                           "Anubis: ServerChannelHandler thread "
                                                   + counter++);
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    protected boolean isTestable() {
        return true;
    }

    protected IOConnectionServerFactory newNioServer() throws Exception {
        MessageConnectionServerFactory factory = new MessageConnectionServerFactory();
        factory.setWireSecurity(wireSecurity());
        factory.setExecutor(executorService());
        return factory;
    }

    protected IOConnectionServerFactory oldNioServer() throws Exception {
        MessageNioServerFactory factory = new MessageNioServerFactory();
        factory.setWireSecurity(wireSecurity());
        return factory;
    }

    protected int poolSize() {
        return 5;
    }

    protected boolean useNewNioServer() {
        return true;
    }

    protected boolean useTimed() {
        return false;
    }
}
