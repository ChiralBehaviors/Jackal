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
package com.hellblazer.jackal.testUtil;

import java.net.InetSocketAddress;

import org.smartfrog.services.anubis.partition.wire.security.NoSecurityImpl;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.context.annotation.Bean;

import com.hellblazer.jackal.configuration.JackalConfig.HeartbeatConfiguration;
import com.hellblazer.pinkie.SocketOptions;

/**
 * @author hhildebrand
 * 
 */
public class TestCfg {

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
        return new HeartbeatConfiguration(2000, 3);
    }

    @Bean
    public SocketOptions socketOptions() {
        SocketOptions socketOptions = new SocketOptions();
        socketOptions.setReuse_address(false);
        socketOptions.setLinger(-1);
        return socketOptions;
    }

    @Bean
    public WireSecurity wireSecurity() {
        return new NoSecurityImpl();
    }
}
