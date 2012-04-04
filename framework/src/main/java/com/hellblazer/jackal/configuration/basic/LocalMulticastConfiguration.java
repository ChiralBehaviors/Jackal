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
package com.hellblazer.jackal.configuration.basic;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.configuration.JackalConfig;
import com.hellblazer.jackal.configuration.JackalConfig.HeartbeatConfiguration;
import com.hellblazer.jackal.configuration.MulticastHeartbeatAndDiscoveryConfig;
import com.hellblazer.jackal.configuration.PartitionAgentConfig;
import com.hellblazer.jackal.configuration.StandardConfigurationConfig;
import com.hellblazer.jackal.configuration.ThreadConfig;

/**
 * @author hhildebrand
 * 
 */
@Configuration
@Import({ JackalConfig.class, StandardConfigurationConfig.class,
         ThreadConfig.class, PartitionAgentConfig.class,
         MulticastHeartbeatAndDiscoveryConfig.class })
public class LocalMulticastConfiguration {
    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(
                                               LocalMulticastConfiguration.class);
    }

    @Bean(name = "connectionSetEndpoint")
    public InetSocketAddress connectionSetEndpoint() {
        return new InetSocketAddress("127.0.0.1", 0);
    }

    @Bean(name = "controllerAgentEndpoint")
    public InetSocketAddress controllerAgentEndpoint() {
        return new InetSocketAddress("127.0.0.1", 0);
    }

    @Bean
    public HeartbeatConfiguration heartbeatConfig() {
        return new HeartbeatConfiguration(3000, 2);
    }

    @Bean
    public MulticastAddress heartbeatGroup() throws UnknownHostException {
        return new MulticastAddress(InetAddress.getByName("233.1.2.30"), 1966,
                                    0);
    }

    @Bean(name = "multicastInterface")
    public InetAddress multicastInterface() {
        try {
            return InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public Identity paritionIdentity() {
        return new Identity(0x1638, node(), System.currentTimeMillis());
    }

    private int node() {
        try {
            return Identity.getProcessUniqueId();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
