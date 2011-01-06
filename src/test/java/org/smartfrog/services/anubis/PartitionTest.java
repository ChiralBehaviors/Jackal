package org.smartfrog.services.anubis;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.test.colors.ColorAllocator;
import org.smartfrog.services.anubis.partition.test.mainconsole.Controller;
import org.smartfrog.services.anubis.partition.test.mainconsole.ControllerConfiguration;
import org.smartfrog.services.anubis.partition.test.mainconsole.NodeData;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.anubis.annotations.DeployedPostProcessor;

public class PartitionTest extends TestCase {
    static class MyController extends Controller {
        @Override
        protected NodeData createNode(HeartbeatMsg hb) {
            return new Node(hb, colorAllocator, this, headless);
        }

    }

    @Configuration
    static class MyControllerConfig extends ControllerConfiguration {
        @Override
        @Bean
        public DeployedPostProcessor deployedPostProcessor() {
            return new DeployedPostProcessor();
        }

        @Override
        protected Controller constructController() {
            return new MyController();
        }

        @Override
        protected boolean headless() {
            return true;
        }

    }

    static class Node extends NodeData {
        boolean initial = true;
        CyclicBarrier barrier = INITIAL_BARRIER;
        int cardinality = CONFIGS.length;
        boolean interrupted = false;
        boolean barrierBroken = false;

        public Node(HeartbeatMsg hb, ColorAllocator colorAllocator,
                    Controller controller, boolean headless) {
            super(hb, colorAllocator, controller, headless);
        }

        @Override
        protected void partitionNotification(View partition, int leader) {
            log.fine("Partition notification: " + partition);
            super.partitionNotification(partition, leader);
            if (partition.isStable() && partition.cardinality() == cardinality) {
                interrupted = false;
                barrierBroken = false;
                Thread testThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            barrier.await();
                        } catch (InterruptedException e) {
                            interrupted = true;
                            return;
                        } catch (BrokenBarrierException e) {
                            barrierBroken = true;
                        }
                    }
                }, "Stability test thread for: " + getIdentity());
                testThread.setDaemon(true);
                testThread.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        e.printStackTrace();
                    }
                });
                testThread.start();
            }
        }
    }

    @Configuration
    static class node0 extends BasicConfiguration {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class node1 extends BasicConfiguration {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class node10 extends BasicConfiguration {
        @Override
        public int node() {
            return 10;
        }
    }

    @Configuration
    static class node11 extends BasicConfiguration {
        @Override
        public int node() {
            return 11;
        }
    }

    @Configuration
    static class node12 extends BasicConfiguration {
        @Override
        public int node() {
            return 12;
        }
    }

    @Configuration
    static class node13 extends BasicConfiguration {
        @Override
        public int node() {
            return 13;
        }
    }

    @Configuration
    static class node14 extends BasicConfiguration {
        @Override
        public int node() {
            return 14;
        }
    }

    @Configuration
    static class node15 extends BasicConfiguration {
        @Override
        public int node() {
            return 15;
        }
    }

    @Configuration
    static class node16 extends BasicConfiguration {
        @Override
        public int node() {
            return 16;
        }
    }

    @Configuration
    static class node17 extends BasicConfiguration {
        @Override
        public int node() {
            return 17;
        }
    }

    @Configuration
    static class node18 extends BasicConfiguration {
        @Override
        public int node() {
            return 18;
        }
    }

    @Configuration
    static class node19 extends BasicConfiguration {
        @Override
        public int node() {
            return 19;
        }
    }

    @Configuration
    static class node2 extends BasicConfiguration {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends BasicConfiguration {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends BasicConfiguration {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends BasicConfiguration {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends BasicConfiguration {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends BasicConfiguration {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends BasicConfiguration {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends BasicConfiguration {
        @Override
        public int node() {
            return 9;
        }
    }

    private static final Logger log = Logger.getLogger(PartitionTest.class.getCanonicalName());

    static CyclicBarrier INITIAL_BARRIER;
    @SuppressWarnings("rawtypes")
    final static Class[] CONFIGS = { node0.class, node1.class, node2.class,
                                    node3.class, node4.class, node5.class,
                                    node6.class, node7.class, node8.class,
                                    node9.class, node10.class, node11.class,
                                    node12.class, node13.class, node14.class,
                                    node15.class, node16.class, node17.class,
                                    node18.class, node19.class };

    AnnotationConfigApplicationContext controllerContext;
    List<AnnotationConfigApplicationContext> memberContexts;
    MyController controller;
    List<Node> partition;

    /**
     * Test that a partition can form two asymmetric partitions, with one
     * stabilizing, and then reform the original partition.
     */
    public void testAsymmetricPartition() throws Exception {
        int minorPartitionSize = CONFIGS.length / 2;
        BitView A = new BitView();
        CyclicBarrier barrierA = new CyclicBarrier(minorPartitionSize + 1);
        List<Node> partitionA = new ArrayList<PartitionTest.Node>();

        CyclicBarrier barrierB = new CyclicBarrier(minorPartitionSize + 1);
        List<Node> partitionB = new ArrayList<PartitionTest.Node>();

        int i = 0;
        for (Node member : partition) {
            if (i++ % 2 == 0) {
                partitionB.add(member);
                member.barrier = barrierA;
                member.cardinality = minorPartitionSize;
                A.add(member.getIdentity());
            } else {
                partitionA.add(member);
                member.barrier = barrierB;
                member.cardinality = minorPartitionSize;
            }
        }
        log.info("asymmetric partitioning: " + A);
        controller.asymPartition(A);
        log.info("Awaiting stabilty of minor partition A");
        barrierA.await(30, TimeUnit.SECONDS);
        // The other partition should still be unstable.
        assertEquals(0, barrierB.getNumberWaiting());

        View viewA = partitionA.get(0).getView();
        for (Node member : partitionA) {
            assertEquals(viewA, member.getView());
        }

        // reform
        CyclicBarrier barrier = new CyclicBarrier(CONFIGS.length + 1);
        for (Node node : partition) {
            node.barrier = barrier;
            node.cardinality = CONFIGS.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stabilty of reformed major partition");
        barrier.await(30, TimeUnit.SECONDS);
    }

    /**
     * Test that a partition can form two stable sub partions and then reform
     * the original partition.
     */
    public void testSymmetricPartition() throws Exception {
        int minorPartitionSize = CONFIGS.length / 2;
        BitView A = new BitView();
        CyclicBarrier barrierA = new CyclicBarrier(minorPartitionSize + 1);
        List<Node> partitionA = new ArrayList<PartitionTest.Node>();

        CyclicBarrier barrierB = new CyclicBarrier(minorPartitionSize + 1);
        List<Node> partitionB = new ArrayList<PartitionTest.Node>();

        int i = 0;
        for (Node member : partition) {
            if (i++ % 2 == 0) {
                partitionB.add(member);
                member.barrier = barrierA;
                member.cardinality = minorPartitionSize;
                A.add(member.getIdentity());
            } else {
                partitionA.add(member);
                member.barrier = barrierB;
                member.cardinality = minorPartitionSize;
            }
        }
        log.info("symmetric partitioning: " + A);
        controller.symPartition(A);
        log.info("Awaiting stabilty of minor partition A");
        barrierA.await(30, TimeUnit.SECONDS);
        log.info("Awaiting stabilty of minor partition B");
        barrierB.await(30, TimeUnit.SECONDS);

        View viewA = partitionA.get(0).getView();
        for (Node member : partitionA) {
            assertEquals(viewA, member.getView());
        }

        View viewB = partitionB.get(0).getView();
        for (Node member : partitionB) {
            assertEquals(viewB, member.getView());
        }

        // reform
        CyclicBarrier barrier = new CyclicBarrier(CONFIGS.length + 1);
        for (Node node : partition) {
            node.barrier = barrier;
            node.cardinality = CONFIGS.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stabilty of reformed major partition");
        barrier.await(30, TimeUnit.SECONDS);
    }

    private List<AnnotationConfigApplicationContext> createMembers() {
        ArrayList<AnnotationConfigApplicationContext> contexts = new ArrayList<AnnotationConfigApplicationContext>();
        for (Class<?> config : CONFIGS) {
            contexts.add(new AnnotationConfigApplicationContext(config));
        }
        return contexts;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        log.info("Setting up initial partition");
        INITIAL_BARRIER = new CyclicBarrier(CONFIGS.length + 1);
        controllerContext = new AnnotationConfigApplicationContext(
                                                                   MyControllerConfig.class);
        memberContexts = createMembers();
        controller = (MyController) controllerContext.getBean(Controller.class);
        log.info("Awaiting initial partition stability");
        INITIAL_BARRIER.await(120, TimeUnit.SECONDS);
        log.info("Initial partition stabile");
        partition = new ArrayList<PartitionTest.Node>();
        for (AnnotationConfigApplicationContext context : memberContexts) {
            partition.add((Node) controller.getNode(context.getBean(Identity.class)));
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
        INITIAL_BARRIER = null;
    }
}
