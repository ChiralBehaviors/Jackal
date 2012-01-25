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

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.leader.LeaderProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.PartitionProtocol;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.partition.comms.ConnectionServerFactory;
import com.hellblazer.pinkie.SocketOptions;

/**
 * Configuration for the Anubis system
 * 
 * @author hhildebrand
 * 
 */
@Configuration
public class Jackal {
    public static class ConnectionSetConfiguration {
        /**
         * @param contactAddress
         * @param heartbeatInterval
         * @param heartbeatTimeout
         */
        public ConnectionSetConfiguration(InetSocketAddress contactAddress,
                                          int heartbeatInterval,
                                          int heartbeatTimeout,
                                          boolean isPreferredLeader) {
            this.contactAddress = contactAddress;
            this.heartbeatInterval = heartbeatInterval;
            this.heartbeatTimeout = heartbeatTimeout;
            this.isPreferredLeader = isPreferredLeader;
        }

        public final InetSocketAddress contactAddress;
        public final int               heartbeatInterval;
        public final int               heartbeatTimeout;
        public final boolean           isPreferredLeader;
    }

    @Autowired
    private ExecutorService            executorService;
    @Autowired
    private HeartbeatCommsFactory      heartbeatCommsFactory;
    @Autowired
    private HeartbeatProtocolFactory   heartbeatProtocolFactory;
    @Autowired
    private Identity                   partitionIdentity;
    @Autowired
    private SocketOptions              socketOptions;
    @Autowired
    private WireSecurity               wireSecurity;
    @Autowired
    private ConnectionSetConfiguration configuration;

    @Bean
    public IOConnectionServerFactory connectionServerFactory() throws Exception {
        return new ConnectionServerFactory(wireSecurity, socketOptions,
                                           executorService);
    }

    @Bean
    public ConnectionSet connectionSet() throws Exception {
        return new ConnectionSet(configuration.contactAddress,
                                 partitionIdentity, heartbeatCommsFactory,
                                 connectionServerFactory(),
                                 leaderProtocolFactory(),
                                 heartbeatProtocolFactory, partitionProtocol(),
                                 configuration.heartbeatInterval,
                                 configuration.heartbeatTimeout,
                                 configuration.isPreferredLeader);
    }

    @Bean
    public LeaderProtocolFactory leaderProtocolFactory() {
        return new LeaderProtocolFactory();
    }

    @Bean
    public PartitionManager partitionManager() {
        return new PartitionManager(partitionIdentity);
    }

    @Bean
    public PartitionProtocol partitionProtocol() {
        return new PartitionProtocol(partitionIdentity, partitionManager());
    }

}
