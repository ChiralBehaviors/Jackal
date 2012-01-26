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

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.MulticastHeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.timed.TimedProtocolFactory;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * @author hhildebrand
 * 
 */
@Configuration
public class MulticastHeartbeatAndDiscoveryConfig {

    @Autowired
    Identity                 partitionIdentity;
    @Autowired
    private WireSecurity     wireSecurity;
    @Autowired
    private MulticastAddress heartbeatGroup;
    @Autowired
    @Qualifier("multicastInterface")
    private InetAddress      networkInterface;

    @Bean
    @Primary
    public HeartbeatCommsFactory heartbeatCommsFactory()
                                                        throws UnknownHostException {
        return new MulticastHeartbeatCommsFactory(wireSecurity, heartbeatGroup,
                                                  networkInterface,
                                                  partitionIdentity);
    }

    @Bean
    @Primary
    public HeartbeatProtocolFactory heartbeatProtocolFactory() {
        return new TimedProtocolFactory();
    }
}
