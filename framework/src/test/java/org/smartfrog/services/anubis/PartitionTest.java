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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.hellblazer.jackal.testUtil.TestController;
import com.hellblazer.jackal.testUtil.TestNode;
import com.hellblazer.jackal.testUtil.TestNodeCfg;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
abstract public class PartitionTest extends TestCase {

    @SuppressWarnings("rawtypes")
    final Class[]                            configs = getConfigs();
    TestController                           controller;
    AnnotationConfigApplicationContext       controllerContext;
    CountDownLatch                           initialLatch;
    final Logger                             log     = getLogger();
    List<AnnotationConfigApplicationContext> memberContexts;
    List<TestNode>                           partition;

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
        List<TestNode> partitionA = new ArrayList<TestNode>();

        CountDownLatch latchB = new CountDownLatch(minorPartitionSize);
        List<TestNode> partitionB = new ArrayList<TestNode>();

        int i = 0;
        for (TestNode member : partition) {
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
        assertTrue("Partition A did not stabilize",
                   latchA.await(60, TimeUnit.SECONDS));
        // The other partition should still be unstable.
        assertEquals(configs.length / 2, latchB.getCount());

        for (TestNode member : partitionA) {
            assertEquals(A, member.getPartition());
        }

        // reform
        CountDownLatch latch = new CountDownLatch(configs.length);
        for (TestNode node : partition) {
            node.latch = latch;
            node.cardinality = configs.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stability of reformed major partition");
        assertTrue("Partition did not reform",
                   latch.await(60, TimeUnit.SECONDS));

        for (TestNode member : partition) {
            assertEquals(All, member.getPartition());
        }
    }

    /**
     * Test that a partition can form two stable sub partions and then reform
     * the original partition.
     */
    public void testSymmetricPartition() throws Exception {
        int minorPartitionSize = configs.length / 2;
        BitView fullPartition = new BitView();
        BitView A = new BitView();
        BitView B = new BitView();
        CountDownLatch latchA = new CountDownLatch(minorPartitionSize);
        List<TestNode> partitionA = new ArrayList<TestNode>();

        CountDownLatch latchB = new CountDownLatch(minorPartitionSize);
        List<TestNode> partitionB = new ArrayList<TestNode>();

        int i = 0;
        for (TestNode member : partition) {
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
            fullPartition.add(member.getIdentity());
        }
        log.info("symmetric partitioning: " + A);
        controller.symPartition(A);
        log.info("Awaiting stability of minor partition A");
        assertTrue("Partition A did not stabilize",
                   latchA.await(60, TimeUnit.SECONDS));
        log.info("Awaiting stability of minor partition B");
        assertTrue("Partition B did not stabilize",
                   latchB.await(60, TimeUnit.SECONDS));

        for (TestNode member : partitionA) {
            assertEquals(A, member.getPartition());
        }

        for (TestNode member : partitionB) {
            assertEquals(B, member.getPartition());
        }

        // reform
        CountDownLatch latch = new CountDownLatch(configs.length);
        for (TestNode node : partition) {
            node.latch = latch;
            node.cardinality = configs.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stability of reformed major partition");
        assertTrue("Partition did not reform",
                   latch.await(60, TimeUnit.SECONDS));

        for (TestNode member : partition) {
            assertEquals(fullPartition, member.getPartition());
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
        TestNodeCfg.nextMagic();
        super.setUp();
        log.info("Setting up initial partition");
        initialLatch = new CountDownLatch(configs.length);
        controllerContext = new AnnotationConfigApplicationContext(
                                                                   getControllerConfig());
        controller = controllerContext.getBean(TestController.class);
        controller.cardinality = configs.length;
        controller.latch = initialLatch;
        memberContexts = createMembers();
        log.info("Awaiting initial partition stability");
        boolean success = false;
        try {
            success = initialLatch.await(60, TimeUnit.SECONDS);
            assertTrue("Initial partition did not acheive stability", success);
            log.info("Initial partition stable");
            partition = new ArrayList<TestNode>();
            for (AnnotationConfigApplicationContext context : memberContexts) {
                TestNode member = (TestNode) controller.getNode(context.getBean(Identity.class));
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
