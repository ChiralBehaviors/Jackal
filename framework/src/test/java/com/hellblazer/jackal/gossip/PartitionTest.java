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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.NodeData;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.annotations.DeployedPostProcessor;
import com.hellblazer.jackal.gossip.configuration.ControllerGossipConfiguration;
import com.hellblazer.jackal.gossip.configuration.GossipConfiguration;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class PartitionTest extends TestCase {
    static class MyController extends Controller {

        public MyController(Timer timer, long checkPeriod, long expirePeriod,
                            Identity partitionIdentity, long heartbeatTimeout,
                            long heartbeatInterval) {
            super(timer, checkPeriod, expirePeriod, partitionIdentity,
                  heartbeatTimeout, heartbeatInterval);
        }

        @Override
        protected NodeData createNode(Heartbeat hb) {
            return new Node(hb, this);
        }

    }

    @Configuration
    static class MyControllerConfig extends ControllerGossipConfiguration {

        @Override
        @Bean
        public DeployedPostProcessor deployedPostProcessor() {
            return new DeployedPostProcessor();
        }

        @Override
        public int magic() {
            try {
                return Identity.getMagicFromLocalIpAddress();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected Controller constructController() throws UnknownHostException {
            return new MyController(timer(), 1000, 300000, partitionIdentity(),
                                    heartbeatTimeout(), heartbeatInterval());
        }

        @Override
        protected Collection<InetSocketAddress> seedHosts()
                                                           throws UnknownHostException {
            return asList(seedContact1(), seedContact2());
        }

        InetSocketAddress seedContact1() throws UnknownHostException {
            return new InetSocketAddress(contactHost(), testPort1);
        }

        InetSocketAddress seedContact2() throws UnknownHostException {
            return new InetSocketAddress(contactHost(), testPort2);
        }

    }

    static class Node extends NodeData {
        private CountDownLatch latch       = INITIAL_LATCH;
        int                    cardinality = CONFIGS.length;

        public Node(Heartbeat hb, Controller controller) {
            super(hb, controller);
        }

        @Override
        protected void partitionNotification(View partition, int leader) {
            super.partitionNotification(partition, leader);
            if (partition.isStable() && partition.cardinality() == cardinality) {
                log.fine(String.format("Partition notification is stable, counting down latch: %s",
                                       partition));
                getLatch().countDown();
            } else {
                log.fine(String.format("Not counting down latch: %s", partition));
            }
        }

        void setLatch(CountDownLatch latch) {
            this.latch = latch;
        }

        CountDownLatch getLatch() {
            return latch;
        }
    }

    @Configuration
    static class node0 extends nodeCfg {
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
    static class node1 extends nodeCfg {
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
    static class node10 extends nodeCfg {
        @Override
        public int node() {
            return 10;
        }
    }

    @Configuration
    static class node11 extends nodeCfg {
        @Override
        public int node() {
            return 11;
        }
    }

    @Configuration
    static class node12 extends nodeCfg {
        @Override
        public int node() {
            return 12;
        }
    }

    @Configuration
    static class node13 extends nodeCfg {
        @Override
        public int node() {
            return 13;
        }
    }

    @Configuration
    static class node14 extends nodeCfg {
        @Override
        public int node() {
            return 14;
        }
    }

    @Configuration
    static class node15 extends nodeCfg {
        @Override
        public int node() {
            return 15;
        }
    }

    @Configuration
    static class node16 extends nodeCfg {
        @Override
        public int node() {
            return 16;
        }
    }

    @Configuration
    static class node17 extends nodeCfg {
        @Override
        public int node() {
            return 17;
        }
    }

    @Configuration
    static class node18 extends nodeCfg {
        @Override
        public int node() {
            return 18;
        }
    }

    @Configuration
    static class node19 extends nodeCfg {
        @Override
        public int node() {
            return 19;
        }
    }

    @Configuration
    static class node2 extends nodeCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends nodeCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends nodeCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends nodeCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends nodeCfg {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends nodeCfg {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends nodeCfg {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends nodeCfg {
        @Override
        public int node() {
            return 9;
        }
    }

    static class nodeCfg extends GossipConfiguration {

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

        InetSocketAddress seedContact1() throws UnknownHostException {
            return new InetSocketAddress(contactHost(), testPort1);
        }

        InetSocketAddress seedContact2() throws UnknownHostException {
            return new InetSocketAddress(contactHost(), testPort2);
        }
    }

    @SuppressWarnings("rawtypes")
    final static Class[]                     CONFIGS = { node0.class,
            node1.class, node2.class, node3.class, node4.class, node5.class,
            node6.class, node7.class, node8.class, node9.class, node10.class,
            node11.class, node12.class, node13.class, node14.class,
            node15.class, node16.class, node17.class, node18.class,
            node19.class                            };
    static CountDownLatch                    INITIAL_LATCH;
    static int                               testPort1;
    static int                               testPort2;
    private static final Logger              log     = Logger.getLogger(PartitionTest.class.getCanonicalName());
    static {
        String port = System.getProperty("com.hellblazer.jackal.gossip.test.port.1",
                                         "24010");
        testPort1 = Integer.parseInt(port);
        port = System.getProperty("com.hellblazer.jackal.gossip.test.port.2",
                                  "24020");
        testPort2 = Integer.parseInt(port);
    }

    MyController                             controller;
    AnnotationConfigApplicationContext       controllerContext;
    List<AnnotationConfigApplicationContext> memberContexts;
    List<Node>                               partition;

    /**
     * Test that a partition can form two asymmetric partitions, with one
     * stabilizing, and then reform the original partition.
     */
    public void testAsymmetricPartition() throws Exception {
        int minorPartitionSize = CONFIGS.length / 2;
        BitView A = new BitView();
        CountDownLatch latchA = new CountDownLatch(minorPartitionSize);
        List<Node> partitionA = new ArrayList<PartitionTest.Node>();

        CountDownLatch latchB = new CountDownLatch(minorPartitionSize);
        List<Node> partitionB = new ArrayList<PartitionTest.Node>();

        int i = 0;
        for (Node member : partition) {
            if (i++ % 2 == 0) {
                partitionA.add(member);
                member.setLatch(latchA);
                member.cardinality = minorPartitionSize;
                A.add(member.getIdentity());
            } else {
                partitionB.add(member);
                member.setLatch(latchB);
                member.cardinality = minorPartitionSize;
            }
        }
        log.info("asymmetric partitioning: " + A);
        controller.asymPartition(A);
        log.info("Awaiting stability of minor partition A");
        assertTrue("minor partition A did not achieve stability",
                   latchA.await(180, TimeUnit.SECONDS));

        for (Node member : partitionA) {
            assertEquals(A, member.getPartition());
        }

        // reform
        CountDownLatch latch = new CountDownLatch(CONFIGS.length);
        for (Node node : partition) {
            node.setLatch(latch);
            node.cardinality = CONFIGS.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stability of reformed major partition");
        assertTrue("reformed partition did not achieve stability",
                   latch.await(60, TimeUnit.SECONDS));
    }

    /**
     * Test that a partition can form two stable sub partions and then reform
     * the original partition.
     */
    public void testSymmetricPartition() throws Exception {
        int minorPartitionSize = CONFIGS.length / 2;
        BitView A = new BitView();
        BitView B = new BitView();
        CountDownLatch latchA = new CountDownLatch(minorPartitionSize);
        List<Node> partitionA = new ArrayList<PartitionTest.Node>();

        CountDownLatch latchB = new CountDownLatch(minorPartitionSize);
        List<Node> partitionB = new ArrayList<PartitionTest.Node>();

        int i = 0;
        for (Node member : partition) {
            if (i++ % 2 == 0) {
                partitionA.add(member);
                member.setLatch(latchA);
                member.cardinality = minorPartitionSize;
                A.add(member.getIdentity());
            } else {
                partitionB.add(member);
                member.setLatch(latchB);
                member.cardinality = minorPartitionSize;
                B.add(member.getIdentity());
            }
        }
        log.info("symmetric partitioning: " + A);
        controller.symPartition(A);
        log.info("Awaiting stability of minor partition A");
        assertTrue("minor partition A did not achieve stability",
                   latchA.await(120, TimeUnit.SECONDS));
        log.info("Awaiting stability of minor partition B");
        assertTrue("minor partition B did not achieve stability",
                   latchB.await(120, TimeUnit.SECONDS));

        for (Node member : partitionA) {
            assertEquals(A, member.getPartition());
        }

        for (Node member : partitionB) {
            assertEquals(B, member.getPartition());
        }

        // reform
        CountDownLatch latch = new CountDownLatch(CONFIGS.length);
        for (Node node : partition) {
            node.setLatch(latch);
            node.cardinality = CONFIGS.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stability of reformed major partition");
        assertTrue("reformed partition did not achieve stability",
                   latch.await(60, TimeUnit.SECONDS));
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        log.info("Setting up initial partition");
        INITIAL_LATCH = new CountDownLatch(CONFIGS.length);
        controllerContext = new AnnotationConfigApplicationContext(
                                                                   MyControllerConfig.class);
        memberContexts = createMembers();
        controller = (MyController) controllerContext.getBean(Controller.class);
        log.info("Test interface controller ID: "
                 + controllerContext.getBean(Identity.class));
        log.info("Awaiting initial partition stability");
        boolean success = false;
        try {
            success = INITIAL_LATCH.await(120, TimeUnit.SECONDS);
            assertTrue("Initial partition did not acheive stability", success);
            log.info("Initial partition stable");
            partition = new ArrayList<PartitionTest.Node>();
            for (AnnotationConfigApplicationContext context : memberContexts) {
                Node member = (Node) controller.getNode(context.getBean(Identity.class));
                assertNotNull("Can't find node: "
                                      + context.getBean(Identity.class), member);
                partition.add(member);
            }
        } finally {
            if (!success) {
                tearDown();
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        testPort1++;
        testPort2++;
        if (controllerContext != null) {
            try {
                controllerContext.close();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        controllerContext = null;
        if (memberContexts != null) {
            for (AnnotationConfigApplicationContext context : memberContexts) {
                try {
                    context.close();
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
        memberContexts = null;
        controller = null;
        partition = null;
        INITIAL_LATCH = null;
        Thread.sleep(1000);
    }

    private List<AnnotationConfigApplicationContext> createMembers() {
        ArrayList<AnnotationConfigApplicationContext> contexts = new ArrayList<AnnotationConfigApplicationContext>();
        for (Class<?> config : CONFIGS) {
            contexts.add(new AnnotationConfigApplicationContext(config));
        }
        return contexts;
    }
}
