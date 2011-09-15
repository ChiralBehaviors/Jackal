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

import static java.util.Arrays.asList;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.smartfrog.services.anubis.SmokeTest;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.gossip.configuration.GossipConfiguration;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class UdpSmokeTest extends SmokeTest {
    static class noTestCfg extends GossipConfiguration {
        static final int testPort;

        static {
            String port = System.getProperty("com.hellblazer.jackal.gossip.test.port",
                                             "53001");
            testPort = Integer.parseInt(port);
        }

        @Override
        public boolean getTestable() {
            return false;
        }

        @Override
        public int getMagic() {
            try {
                return Identity.getMagicFromLocalIpAddress();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        InetSocketAddress seedContact() throws UnknownHostException {
            return new InetSocketAddress("127.0.0.1", testPort);
        }

        InetSocketAddress seedContact2() throws UnknownHostException {
            return new InetSocketAddress("127.0.0.1", testPort + 2);
        }

        @Override
        protected Collection<InetSocketAddress> seedHosts()
                                                           throws UnknownHostException {
            return asList(seedContact(), seedContact2());
        }
    }

    @Configuration
    static class testA extends noTestCfg {
        @Override
        public int node() {
            return 0;
        }

        @Override
        protected InetSocketAddress gossipEndpoint()
                                                    throws UnknownHostException {
            return seedContact();
        }
    }

    @Configuration
    static class testB extends noTestCfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class testC extends noTestCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class testD extends noTestCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class testE extends noTestCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class testF extends noTestCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class testG extends noTestCfg {
        @Override
        public int node() {
            return 6;
        }

        @Override
        protected InetSocketAddress gossipEndpoint()
                                                    throws UnknownHostException {
            return seedContact2();
        }
    }

    @Override
    protected Class<?>[] getConfigurations() {
        return new Class[] { testA.class, testB.class, testC.class,
                testD.class, testE.class, testF.class, testG.class };
    }
}
