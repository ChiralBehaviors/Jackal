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

import static java.util.Arrays.asList;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.configuration.GossipHeartbeatAndDiscoveryConfig;
import com.hellblazer.jackal.configuration.JackalConfig;
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
public class GossipTestCfg {
    private static int testPort1;

    private static int testPort2;

    static {
        String port = System.getProperty("com.hellblazer.jackal.gossip.test.port.1",
                                         "24010");
        testPort1 = Integer.parseInt(port);
        port = System.getProperty("com.hellblazer.jackal.gossip.test.port.2",
                                  "24020");
        testPort2 = Integer.parseInt(port);
    }

    public static int getTestPort1() {
        return testPort1;
    }

    public static int getTestPort2() {
        return testPort2;
    }

    public static void incrementPorts() {
        testPort1++;
        testPort2++;
    }

    @Bean(name = "seedHosts")
    public List<InetSocketAddress> seedHosts() throws UnknownHostException {
        return asList(new InetSocketAddress("127.0.0.1", getTestPort1()),
                      new InetSocketAddress("127.0.0.1", getTestPort2()));
    }
}
