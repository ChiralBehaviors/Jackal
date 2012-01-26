/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.jackal.configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.hellblazer.jackal.configuration.JackalConfig.HeartbeatConfiguration;
import com.hellblazer.jackal.gossip.FailureDetectorFactory;
import com.hellblazer.jackal.gossip.Gossip;
import com.hellblazer.jackal.gossip.GossipCommunications;
import com.hellblazer.jackal.gossip.GossipHeartbeatProtocolFactory;
import com.hellblazer.jackal.gossip.SystemView;
import com.hellblazer.jackal.gossip.fd.SimpleTimeoutFailureDetectorFactory;
import com.hellblazer.jackal.gossip.udp.UdpCommunications;

/**
 * @author hhildebrand
 * 
 */
@Configuration
public class GossipHeartbeatAndDiscoveryConfig {
    public static class GossipConfiguration {
        public final int      interval;
        public final int      quarantineDelay;
        public final TimeUnit unit;
        public final int      unreachableNodeDelay;

        public GossipConfiguration(int interval, TimeUnit unit,
                                   int quarantineDelay, int unreachableNodeDelay) {
            this.interval = interval;
            this.unit = unit;
            this.quarantineDelay = quarantineDelay;
            this.unreachableNodeDelay = unreachableNodeDelay;
        }
    }

    @Autowired
    @Qualifier("gossipEndpoint")
    private InetSocketAddress       endpoint;
    @Autowired
    private FailureDetectorFactory  failureDetectorFactory;
    @Autowired
    private GossipConfiguration     gossipConfiguration;
    @Autowired
    private Identity                partitionIdentity;
    @Resource(name = "seedHosts")
    private List<InetSocketAddress> seedHosts;
    @Autowired
    @Qualifier("gossipDispatchers")
    private ExecutorService         gossipDispatchers;

    @Bean
    @Primary
    public GossipCommunications communications() throws IOException {
        return new UdpCommunications(endpoint, gossipDispatchers, 20, 4);
    }

    @Bean
    @Primary
    @Autowired
    public FailureDetectorFactory failureDetectorFactory(HeartbeatConfiguration heartbeatConfig) {
        return new SimpleTimeoutFailureDetectorFactory(
                                                       heartbeatConfig.heartbeatInterval
                                                               * heartbeatConfig.heartbeatTimeout
                                                               * 3);
    }

    @Bean
    @Primary
    public Gossip gossip() throws IOException {
        return new Gossip(systemView(), new SecureRandom(), communications(),
                          gossipConfiguration.interval,
                          gossipConfiguration.unit, failureDetectorFactory,
                          partitionIdentity);
    }

    @Bean
    @Primary
    @Autowired
    public GossipConfiguration gossipConfiguration(HeartbeatConfiguration heartbeatConfig) {
        return new GossipConfiguration(
                                       500,
                                       TimeUnit.MILLISECONDS,
                                       heartbeatConfig.heartbeatInterval
                                               * (heartbeatConfig.heartbeatTimeout + 1),
                                       500000);
    }

    @Bean
    @Primary
    public HeartbeatProtocolFactory heartbeatProtocolFactory()
                                                              throws IOException {
        return new GossipHeartbeatProtocolFactory(gossip());
    }

    @Bean
    @Primary
    public SystemView systemView() throws IOException {
        return new SystemView(new SecureRandom(),
                              communications().getLocalAddress(), seedHosts,
                              gossipConfiguration.quarantineDelay,
                              gossipConfiguration.unreachableNodeDelay);
    }
}
