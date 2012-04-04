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

import static java.util.Arrays.asList;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.configuration.GossipHeartbeatAndDiscoveryConfig;
import com.hellblazer.jackal.configuration.JackalConfig;
import com.hellblazer.jackal.configuration.JackalConfig.HeartbeatConfiguration;
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
         GossipHeartbeatAndDiscoveryConfig.class })
public class LocalGossipConfiguration {
    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(LocalGossipConfiguration.class);
    }

    @Bean(name = "connectionSetEndpoint")
    public InetSocketAddress connectionSetEndpoint() {
        return new InetSocketAddress("127.0.0.1", 0);
    }

    @Bean(name = "controllerAgentEndpoint")
    public InetSocketAddress controllerAgentEndpoint() {
        return new InetSocketAddress("127.0.0.1", 0);
    }

    @Bean(name = "gossipEndpoint")
    public InetSocketAddress gossipEndpoint() {
        return new InetSocketAddress("127.0.0.1", 0);
    }

    @Bean
    public HeartbeatConfiguration heartbeatConfig() {
        return new HeartbeatConfiguration(3000, 2);
    }

    @Bean
    public Identity paritionIdentity() {
        return new Identity(0x1638, node(), System.currentTimeMillis());
    }

    @Bean(name = "seedHosts")
    public List<InetSocketAddress> seedHosts() throws UnknownHostException {
        return asList(new InetSocketAddress("127.0.0.1", 1024));
    }

    private int node() {
        try {
            return Identity.getProcessUniqueId();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
