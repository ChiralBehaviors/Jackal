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
import java.util.logging.Logger;

import org.smartfrog.services.anubis.PartitionTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.testUtil.gossip.GossipControllerCfg;
import com.hellblazer.jackal.testUtil.gossip.GossipDiscoveryNode1Cfg;
import com.hellblazer.jackal.testUtil.gossip.GossipDiscoveryNode2Cfg;
import com.hellblazer.jackal.testUtil.gossip.GossipNodeCfg;
import com.hellblazer.jackal.testUtil.gossip.GossipTestCfg;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class UdpPartitionTest extends PartitionTest {

    @Configuration
    static class member1 extends GossipDiscoveryNode1Cfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class member2 extends GossipDiscoveryNode2Cfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class member extends GossipNodeCfg {
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

    static {
        GossipTestCfg.setTestPorts(24030, 24050);
    }

    @Override
    protected Class<?>[] getConfigs() {
        return new Class<?>[] { member1.class, member2.class, member.class,
                member.class, member.class, member.class, member.class,
                member.class, member.class, member.class, member.class,
                member.class, member.class, member.class, member.class,
                member.class, member.class, member.class, member.class,
                member.class };
    }

    @Override
    protected Class<?> getControllerConfig() {
        return GossipControllerCfg.class;
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(getClass().getCanonicalName());
    }

    @Override
    protected void setUp() throws Exception {
        GossipTestCfg.incrementPorts();
        member.reset();
        super.setUp();
    }
}
