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

import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.configuration.Jackal.HeartbeatConfiguration;
import com.hellblazer.jackal.gossip.FailureDetectorFactory;
import com.hellblazer.jackal.gossip.Gossip;
import com.hellblazer.jackal.gossip.GossipCommunications;
import com.hellblazer.jackal.gossip.SystemView;
import com.hellblazer.jackal.gossip.fd.SimpleTimeoutFailureDetectorFactory;
import com.hellblazer.jackal.gossip.udp.UdpCommunications;

/**
 * @author hhildebrand
 * 
 */
@Configuration
public class GossipHeartbeatAndDiscovery {
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
    @Resource(name = "seedHosts")
    private List<InetSocketAddress> seedHosts;
    @Autowired
    private ExecutorService         udpDispatcher;
    @Autowired
    private Identity                partitionIdentity;

    @Bean
    public HeartbeatConfiguration heartbeatConfig() {
        return new HeartbeatConfiguration(3000, 2);
    }

    @Bean
    public GossipCommunications communications() throws IOException {
        return new UdpCommunications(endpoint, udpDispatcher, 20, 4);
    }

    @Bean
    public Gossip gossip() throws IOException {
        return new Gossip(systemView(), new SecureRandom(), communications(),
                          gossipConfiguration.interval,
                          gossipConfiguration.unit, failureDetectorFactory,
                          partitionIdentity);
    }

    @Bean
    public SystemView systemView() throws IOException {
        return new SystemView(new SecureRandom(),
                              communications().getLocalAddress(), seedHosts,
                              gossipConfiguration.quarantineDelay,
                              gossipConfiguration.unreachableNodeDelay);
    }

    @Bean
    public GossipConfiguration gossipConfiguration() {
        return new GossipConfiguration(
                                       500,
                                       TimeUnit.MILLISECONDS,
                                       heartbeatConfig().heartbeatInterval
                                               * (heartbeatConfig().heartbeatTimeout + 1),
                                       500000);
    }

    @Bean
    public FailureDetectorFactory failureDetectorFactory() {
        return new SimpleTimeoutFailureDetectorFactory(
                                                       heartbeatConfig().heartbeatInterval
                                                               * heartbeatConfig().heartbeatTimeout
                                                               * 3);
    }
}
