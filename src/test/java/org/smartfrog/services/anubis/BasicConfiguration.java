package org.smartfrog.services.anubis;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
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

import com.hellblazer.anubis.annotations.DeployedPostProcessor;

@Configuration
public class BasicConfiguration {

	@Bean
	public DeployedPostProcessor deployedPostProcessor() {
		return new DeployedPostProcessor();
	}

	public long heartbeatInterval() {
		return 2000L;
	}

	public long heartbeatTimeout() {
		return 3L;
	}

	public int heartbeatGroupPort() {
		return 1966;
	}

	public InetAddress heartbeatGroupMulticastAddress()
			throws UnknownHostException {
		return InetAddress.getByName("233.1.2.30");
	}

	public int heartbeatGroupTTL() {
		return 1;
	}

	@Bean
	public ConnectionAddress contactAddress() throws UnknownHostException {
		return new ConnectionAddress(contactHost(), contactPort());
	}

	public int contactPort() {
		return 0;
	}

	public InetAddress contactHost() throws UnknownHostException {
		return InetAddress.getLocalHost();
	}

	@Bean
	public Epoch epoch() {
		return new Epoch();
	}

	@Bean
	public Identity partitionIdentity() {
		return new Identity(getMagic(), getNode(), epoch().longValue());
	}

	public int getMagic() {
		return 12345;
	}

	public int getNode() {
		return 1;
	}

	@Bean
	public MulticastAddress heartbeatGroup() throws UnknownHostException {
		return new MulticastAddress(heartbeatGroupMulticastAddress(),
				heartbeatGroupPort(), heartbeatGroupTTL());
	}

	@Bean
	public WireSecurity wireSecurity() {
		return new NoSecurityImpl();
	}

	@Bean
	public AnubisLocator locator() {
		Locator locator = new Locator();
		locator.setIdentity(partitionIdentity());
		locator.setPartition(partition());
		locator.setHeartbeatInterval(heartbeatInterval());
		locator.setHeartbeatTimeout(heartbeatTimeout());
		return locator;
	}

	@Bean
	public PartitionManager partition() {
		PartitionManager partition = new PartitionManager();
		partition.setIdentity(partitionIdentity());
		return partition;
	}

	@Bean
	public Timer timer() {
		return new Timer("Partition timer", true);
	}

	@Bean
	public LeaderProtocolFactory leaderProtocolFactory() {
		return new LeaderProtocolFactory();
	}

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
	public PartitionProtocol partitionProtocol() throws UnknownHostException {
		PartitionProtocol protocol = new PartitionProtocol();
		protocol.setPartitionMgr(partition());
		protocol.setIdentity(partitionIdentity());
		return protocol;
	}

	@Bean
	public HeartbeatProtocolFactory heartbeatProtocolFactory() {
		return new TimedProtocolFactory();
	}

	@Bean
	public HeartbeatCommsFactory heartbeatCommsFactory() {
		HeartbeatCommsFactory factory = new HeartbeatCommsFactory();
		factory.setWireSecurity(wireSecurity());
		return factory;
	}

	@Bean
	public IOConnectionServerFactory ioConnectionServerFactory()
			throws Exception {
		MessageNioServerFactory factory = new MessageNioServerFactory();
		factory.setWireSecurity(wireSecurity());
		return factory;
	}

	@Bean
	public TestMgr testMgr() throws Exception {
		TestMgr mgr = new TestMgr(contactHost().getCanonicalHostName(),
				contactPort(), partition(), getNode());
		return mgr;
	}
}
