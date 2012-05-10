/** (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.jackal.gossip;

import java.util.concurrent.atomic.AtomicInteger;

import org.smartfrog.services.anubis.SmokeTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.testUtil.gossip.GossipDiscoveryNode1Cfg;
import com.hellblazer.jackal.testUtil.gossip.GossipDiscoveryNode2Cfg;
import com.hellblazer.jackal.testUtil.gossip.GossipNodeCfg;
import com.hellblazer.jackal.testUtil.gossip.GossipTestCfg;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class UdpSmokeTest extends SmokeTest {

    @Configuration
    static class test extends GossipNodeCfg {
        private static final AtomicInteger id = new AtomicInteger(1);

        @Override
        @Bean
        public int node() {
            return id.incrementAndGet();
        }
    }

    @Configuration
    static class test1 extends GossipDiscoveryNode1Cfg {

        @Override
        @Bean
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class test2 extends GossipDiscoveryNode2Cfg {

        @Override
        @Bean
        public int node() {
            return 1;
        }
    }

    static {
        GossipTestCfg.setTestPorts(24910, 24820);
    }

    @Override
    protected Class<?>[] getConfigurations() {
        return new Class[] { test1.class, test2.class, test.class, test.class,
                test.class, test.class, test.class };
    }
}
