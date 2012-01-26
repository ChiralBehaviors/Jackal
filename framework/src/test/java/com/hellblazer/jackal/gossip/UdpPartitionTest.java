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

import java.util.logging.Logger;

import org.smartfrog.services.anubis.PartitionTest;
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
    static class node0 extends GossipDiscoveryNode1Cfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class node1 extends GossipDiscoveryNode2Cfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class node10 extends GossipNodeCfg {
        @Override
        public int node() {
            return 10;
        }
    }

    @Configuration
    static class node11 extends GossipNodeCfg {
        @Override
        public int node() {
            return 11;
        }
    }

    @Configuration
    static class node12 extends GossipNodeCfg {
        @Override
        public int node() {
            return 12;
        }
    }

    @Configuration
    static class node13 extends GossipNodeCfg {
        @Override
        public int node() {
            return 13;
        }
    }

    @Configuration
    static class node14 extends GossipNodeCfg {
        @Override
        public int node() {
            return 14;
        }
    }

    @Configuration
    static class node15 extends GossipNodeCfg {
        @Override
        public int node() {
            return 15;
        }
    }

    @Configuration
    static class node16 extends GossipNodeCfg {
        @Override
        public int node() {
            return 16;
        }
    }

    @Configuration
    static class node17 extends GossipNodeCfg {
        @Override
        public int node() {
            return 17;
        }
    }

    @Configuration
    static class node18 extends GossipNodeCfg {
        @Override
        public int node() {
            return 18;
        }
    }

    @Configuration
    static class node19 extends GossipNodeCfg {
        @Override
        public int node() {
            return 19;
        }
    }

    @Configuration
    static class node2 extends GossipNodeCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends GossipNodeCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends GossipNodeCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends GossipNodeCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends GossipNodeCfg {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends GossipNodeCfg {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends GossipNodeCfg {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends GossipNodeCfg {
        @Override
        public int node() {
            return 9;
        }
    }

    static {
        GossipTestCfg.setTestPorts(24030, 24050);
    }

    @Override
    protected Class<?>[] getConfigs() {
        return new Class<?>[] { node0.class, node1.class, node2.class,
                node3.class, node4.class, node5.class, node6.class,
                node7.class, node8.class, node9.class, node10.class,
                node11.class, node12.class, node13.class, node14.class,
                node15.class, node16.class, node17.class, node18.class,
                node19.class };
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
        super.setUp();
    }
}
