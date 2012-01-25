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

import static java.util.Arrays.asList;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.hellblazer.jackal.gossip.configuration.ControllerGossipConfiguration;
import com.hellblazer.jackal.gossip.configuration.GossipConfiguration;
import com.hellblazer.slp.ServiceScope;

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
    static class MyControllerConfig extends ControllerGossipConfiguration {

        @Override
        public int magic() {
            try {
                return Identity.getMagicFromLocalIpAddress();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected Controller constructController() throws IOException {
            return new MyController(partitionIdentity(), heartbeatTimeout(),
                                    heartbeatInterval(), socketOptions(),
                                    dispatchExecutor(), wireSecurity());
        }

        @Override
        protected Collection<InetSocketAddress> seedHosts()
                                                           throws UnknownHostException {
            return asList(seedContact1(), seedContact2());
        }

        InetSocketAddress seedContact1() throws UnknownHostException {
            return new InetSocketAddress("127.0.0.1", testPort1);
        }

        InetSocketAddress seedContact2() throws UnknownHostException {
            return new InetSocketAddress("127.0.0.1", testPort2);
        }
    }

    @Configuration
    static class node0 extends slpConfig {
        @Override
        public int node() {
            return 0;
        }

        @Override
        protected InetSocketAddress gossipEndpoint()
                                                    throws UnknownHostException {
            return seedContact1();
        }
    }

    @Configuration
    static class node1 extends slpConfig {
        @Override
        public int node() {
            return 1;
        }

        @Override
        protected InetSocketAddress gossipEndpoint()
                                                    throws UnknownHostException {
            return seedContact2();
        }
    }

    @Configuration
    static class node2 extends slpConfig {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends slpConfig {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends slpConfig {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends slpConfig {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends slpConfig {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends slpConfig {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends slpConfig {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends slpConfig {
        @Override
        public int node() {
            return 9;
        }
    }

    static class slpConfig extends GossipConfiguration {

        @Bean
        public ServiceScope anubisScope() {
            return new AnubisScope(partitionIdentity(),
                                   Executors.newCachedThreadPool(),
                                   uuidGenerator(), partition());
        }

        @Override
        public int getMagic() {
            try {
                return Identity.getMagicFromLocalIpAddress();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected Collection<InetSocketAddress> seedHosts()
                                                           throws UnknownHostException {
            return asList(seedContact1(), seedContact2());
        }

        protected String stateName() {
            return "Test Scope";
        }

        protected NoArgGenerator uuidGenerator() {
            return new RandomBasedGenerator(RANDOM);
        }

        InetSocketAddress seedContact1() throws UnknownHostException {
            return new InetSocketAddress("127.0.0.1", testPort1);
        }

        InetSocketAddress seedContact2() throws UnknownHostException {
            return new InetSocketAddress("127.0.0.1", testPort2);
        }
    }

    static int testPort1;

    static int testPort2;

    static {
        String port = System.getProperty("com.hellblazer.jackal.gossip.test.port.1",
                                         "24010");
        testPort1 = Integer.parseInt(port);
        port = System.getProperty("com.hellblazer.jackal.gossip.test.port.2",
                                  "24020");
        testPort2 = Integer.parseInt(port);
    }

    @Override
    protected Class<?>[] getConfigs() {
        return new Class<?>[] { node0.class, node1.class, node2.class,
                node3.class, node4.class, node5.class, node6.class,
                node7.class, node8.class, node9.class };
    }

    @Override
    protected Class<?> getControllerConfig() {
        return MyControllerConfig.class;
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(UdpEndToEndTest.class.getCanonicalName());
    }

    @Override
    protected void setUp() throws Exception {
        testPort1++;
        testPort2++;
        super.setUp();
    }
}
