package org.smartfrog.services.anubis;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.MulticastHeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.nonblocking.MessageNioServerFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
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

import com.hellblazer.jackal.annotations.DeployedPostProcessor;

@Configuration
public class BasicConfiguration {

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
}
