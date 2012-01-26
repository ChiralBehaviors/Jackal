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

import org.smartfrog.services.anubis.SmokeTest;
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

    static {
        GossipTestCfg.setTestPorts(24010, 24020);
    }

    @Configuration
    static class testA extends GossipDiscoveryNode1Cfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class testB extends GossipNodeCfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class testC extends GossipNodeCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class testD extends GossipNodeCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class testE extends GossipNodeCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class testF extends GossipNodeCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class testG extends GossipDiscoveryNode2Cfg {
        @Override
        public int node() {
            return 6;
        }
    }

    @Override
    protected Class<?>[] getConfigurations() {
        return new Class[] { testA.class, testB.class, testC.class,
                testD.class, testE.class, testF.class, testG.class };
    }
}
