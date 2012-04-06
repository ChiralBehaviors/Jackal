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
package com.hellblazer.slp.anubis;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.testUtil.TestNodeCfg;
import com.hellblazer.jackal.testUtil.gossip.GossipControllerCfg;
import com.hellblazer.jackal.testUtil.gossip.GossipDiscoveryNode1Cfg;
import com.hellblazer.jackal.testUtil.gossip.GossipDiscoveryNode2Cfg;
import com.hellblazer.jackal.testUtil.gossip.GossipNodeCfg;
import com.hellblazer.jackal.testUtil.gossip.GossipTestCfg;

/**
 * 
 * Functionally test the scope across multiple members in different failure
 * scenarios.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class UdpEndToEndTest extends EndToEndTest {

    @Import({ SlpConfig.class })
    @Configuration
    static abstract class gossipSlpConfig extends GossipNodeCfg {

    }

    @Configuration
    static class member extends gossipSlpConfig {
        private static final AtomicInteger id = new AtomicInteger(1);

        public static void reset() {
            id.set(1);
        }

        @Override
        @Bean
        public int node() {
            return id.incrementAndGet();
        }
    }

    @Configuration
    @Import({ SlpConfig.class })
    static class member0 extends GossipDiscoveryNode1Cfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    @Import({ SlpConfig.class })
    static class member1 extends GossipDiscoveryNode2Cfg {
        @Override
        public int node() {
            return 1;
        }
    }

    static {
        GossipTestCfg.setTestPorts(23010, 23020);
    }

    @Override
    protected Class<?>[] getConfigs() {
        return new Class<?>[] { member0.class, member1.class, member.class,
                member.class, member.class, member.class, member.class,
                member.class, member.class, member.class };
    }

    @Override
    protected Class<?> getControllerConfig() {
        return GossipControllerCfg.class;
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(UdpEndToEndTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        TestNodeCfg.nextMagic();
        GossipTestCfg.incrementPorts();
        member.reset();
        super.setUp();
    }
}
