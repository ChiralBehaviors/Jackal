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
package org.smartfrog.services.anubis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.NodeData;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.hellblazer.pinkie.SocketOptions;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
abstract public class PartitionTest extends TestCase {
    public static class MyController extends Controller {
        int            cardinality;
        CountDownLatch latch;

        public MyController(Timer timer, long checkPeriod, long expirePeriod,
                            Identity partitionIdentity, long heartbeatTimeout,
                            long heartbeatInterval,
                            SocketOptions socketOptions,
                            ExecutorService dispatchExecutor,
                            WireSecurity wireSecurity) throws IOException {
            super(timer, checkPeriod, expirePeriod, partitionIdentity,
                  heartbeatTimeout, heartbeatInterval, socketOptions,
                  dispatchExecutor, wireSecurity);
        }

        @Override
        protected NodeData createNode(Heartbeat hb) {
            Node node = new Node(hb, this);
            node.cardinality = cardinality;
            node.latch = latch;
            return node;
        }

    }

    public static class Node extends NodeData {
        static final Logger log = Logger.getLogger(Node.class.getCanonicalName());

        int                 cardinality;
        CountDownLatch      latch;

        public Node(Heartbeat hb, Controller controller) {
            super(hb, controller);
        }

        @Override
        protected void partitionNotification(View partition, int leader) {
            log.fine("Partition notification: " + partition);
            super.partitionNotification(partition, leader);
            if (partition.isStable() && partition.cardinality() == cardinality) {
                latch.countDown();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    final Class[]                            configs = getConfigs();
    MyController                             controller;
    AnnotationConfigApplicationContext       controllerContext;
    CountDownLatch                           initialLatch;
    final Logger                             log     = getLogger();
    List<AnnotationConfigApplicationContext> memberContexts;
    List<Node>                               partition;

    /**
     * Test that a partition can form two stable sub partions and then reform
     * the original partition.
     */
    public void testSymmetricPartition() throws Exception {
        int minorPartitionSize = configs.length / 2;
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
                member.latch = latchA;
                member.cardinality = minorPartitionSize;
                A.add(member.getIdentity());
            } else {
                partitionB.add(member);
                member.latch = latchB;
                member.cardinality = minorPartitionSize;
                B.add(member.getIdentity());
            }
        }
        log.info("symmetric partitioning: " + A);
        controller.symPartition(A);
        log.info("Awaiting stability of minor partition A");
        latchA.await(60, TimeUnit.SECONDS);
        log.info("Awaiting stability of minor partition B");
        latchB.await(60, TimeUnit.SECONDS);

        for (Node member : partitionA) {
            assertEquals(A, member.getPartition());
        }

        for (Node member : partitionB) {
            assertEquals(B, member.getPartition());
        }

        // reform
        CountDownLatch latch = new CountDownLatch(configs.length);
        for (Node node : partition) {
            node.latch = latch;
            node.cardinality = configs.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stability of reformed major partition");
        latch.await(60, TimeUnit.SECONDS);
    }

    /**
     * Test that a partition can form two asymmetric partitions, with one
     * stabilizing, and then reform the original partition.
     */
    public void testAsymmetricPartition() throws Exception {
        int minorPartitionSize = configs.length / 2;
        BitView A = new BitView();
        BitView B = new BitView();
        BitView All = new BitView();
        CountDownLatch latchA = new CountDownLatch(minorPartitionSize);
        List<Node> partitionA = new ArrayList<PartitionTest.Node>();

        CountDownLatch latchB = new CountDownLatch(minorPartitionSize);
        List<Node> partitionB = new ArrayList<PartitionTest.Node>();

        int i = 0;
        for (Node member : partition) {
            All.add(member.getIdentity());
            if (i++ % 2 == 0) {
                partitionA.add(member);
                member.latch = latchA;
                member.cardinality = minorPartitionSize;
                A.add(member.getIdentity());
            } else {
                partitionB.add(member);
                member.latch = latchB;
                member.cardinality = minorPartitionSize;
                B.add(member.getIdentity());
            }
        }
        log.info("asymmetric partitioning: " + A);
        controller.asymPartition(A);
        log.info("Awaiting stability of minor partition A");
        latchA.await(60, TimeUnit.SECONDS);
        // The other partition should still be unstable.
        assertEquals(configs.length / 2, latchB.getCount());

        for (Node member : partitionA) {
            assertEquals(A, member.getPartition());
        }

        // reform
        CountDownLatch latch = new CountDownLatch(configs.length);
        for (Node node : partition) {
            node.latch = latch;
            node.cardinality = configs.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stability of reformed major partition");
        latch.await(60, TimeUnit.SECONDS);

        for (Node member : partition) {
            assertEquals(All, member.getPartition());
        }
    }

    private List<AnnotationConfigApplicationContext> createMembers() {
        ArrayList<AnnotationConfigApplicationContext> contexts = new ArrayList<AnnotationConfigApplicationContext>();
        for (Class<?> config : configs) {
            contexts.add(new AnnotationConfigApplicationContext(config));
        }
        return contexts;
    }

    abstract protected Class<?>[] getConfigs();

    abstract protected Class<?> getControllerConfig();

    abstract protected Logger getLogger();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        log.info("Setting up initial partition");
        initialLatch = new CountDownLatch(configs.length);
        controllerContext = new AnnotationConfigApplicationContext(
                                                                   getControllerConfig());
        controller = (MyController) controllerContext.getBean(Controller.class);
        controller.cardinality = configs.length;
        controller.latch = initialLatch;
        memberContexts = createMembers();
        log.info("Awaiting initial partition stability");
        boolean success = false;
        try {
            success = initialLatch.await(120, TimeUnit.SECONDS);
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
        initialLatch = null;
    }
}
