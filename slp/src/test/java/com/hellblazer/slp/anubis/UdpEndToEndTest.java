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

import java.util.logging.Logger;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

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

    @Configuration
    @Import({ SlpConfig.class })
    static class node0 extends GossipDiscoveryNode1Cfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    @Import({ SlpConfig.class })
    static class node1 extends GossipDiscoveryNode2Cfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class node2 extends gossipSlpConfig {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends gossipSlpConfig {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends gossipSlpConfig {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends gossipSlpConfig {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends gossipSlpConfig {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends gossipSlpConfig {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends gossipSlpConfig {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends gossipSlpConfig {
        @Override
        public int node() {
            return 9;
        }
    }

    @Import({ SlpConfig.class })
    @Configuration
    static abstract class gossipSlpConfig extends GossipNodeCfg {

    }

    static {
        GossipTestCfg.setTestPorts(23010, 23020);
    }

    @Override
    protected Class<?>[] getConfigs() {
        return new Class<?>[] { node0.class, node1.class, node2.class,
                node3.class, node4.class, node5.class, node6.class,
                node7.class, node8.class, node9.class };
    }

    @Override
    protected Class<?> getControllerConfig() {
        return GossipControllerCfg.class;
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(UdpEndToEndTest.class.getCanonicalName());
    }

    @Override
    protected void setUp() throws Exception {
        GossipTestCfg.incrementPorts();
        super.setUp();
    }
}
