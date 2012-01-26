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
package com.hellblazer.jackal.testUtil.gossip;

import java.net.InetSocketAddress;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.configuration.GossipHeartbeatAndDiscoveryConfig;
import com.hellblazer.jackal.testUtil.TestNodeCfg;

/**
 * @author hhildebrand
 * 
 */
@Configuration
@Import({ GossipTestCfg.class, GossipHeartbeatAndDiscoveryConfig.class })
abstract public class GossipDiscoveryNode1Cfg extends TestNodeCfg {
    @Bean(name = "gossipEndpoint")
    public InetSocketAddress gossipEndpoint() {
        return new InetSocketAddress("127.0.0.1", GossipTestCfg.getTestPort1());
    }
}
