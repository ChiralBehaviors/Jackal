package com.hellblazer.satellite;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.BasicConfiguration;
import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.AnubisProvider;
import org.smartfrog.services.anubis.locator.AnubisValue;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.PartitionNotification;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.anubis.satellite.Launch;

public class TestLaunch extends TestCase {
    @Configuration
    static class node1 extends BasicConfiguration {

        @Override
        public int getMagic() {
            try {
                return Identity.getMagicFromLocalIpAddress();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public int heartbeatGroupTTL() {
            return 0;
        }

        @Override
        public int node() {
            return 1;
        }
    }

    static class Node extends AnubisListener {
        CountDownLatch latch;

        List<Object>   stateValues = new ArrayList<Object>();

        public Node(String n, CountDownLatch latch) {
            super(n);
            this.latch = latch;
        }

        @Override
        public void newValue(AnubisValue value) {
            stateValues.add(value.getValue());
            latch.countDown();
        }

        @Override
        public void removeValue(AnubisValue value) {
        }

    }

    Launch launch;

    @Override
    protected void setUp() throws Exception {
        launch = new Launch();
        launch.setJavaArgs(Arrays.asList("-Djava.net.preferIPv4Stack=true"));
        launch.setPeriod(3000);
        launch.setLaunchTimeout(90, TimeUnit.SECONDS);
    }

    @Override
    protected void tearDown() throws Exception {
        launch.shutDown();
    }

    public void testLaunch() throws Exception {
        final CyclicBarrier startBarrier = new CyclicBarrier(2);
        final CountDownLatch latch = new CountDownLatch(6);
        launch.setConfigPackage("com.hellblazer.satellite.test");
        AnubisLocator locator0 = launch.getLocator();
        assertNotNull(locator0);
        ApplicationContext ctx = new AnnotationConfigApplicationContext(
                                                                        node1.class);
        AnubisLocator locator1 = ctx.getBean(AnubisLocator.class);

        String stateName = "drink-me";
        Node node0 = new Node(stateName, latch);
        AnubisProvider provider0 = new AnubisProvider(stateName);

        Node node1 = new Node(stateName, latch);
        AnubisProvider provider1 = new AnubisProvider(stateName);

        locator0.registerListener(node0);
        locator1.registerListener(node1);
        locator0.registerProvider(provider0);
        locator1.registerProvider(provider1);

        PartitionManager pm = ctx.getBean(PartitionManager.class);
        pm.register(new PartitionNotification() {

            @Override
            public void partitionNotification(View view, int leader) {
                if (view.isStable() && view.cardinality() == 2) {
                    try {
                        startBarrier.await();
                    } catch (InterruptedException e) {
                        return;
                    } catch (BrokenBarrierException e) {
                        return;
                    }
                }
            }

            @Override
            public void objectNotification(Object obj, int sender, long time) {
            }
        });

        startBarrier.await(30, TimeUnit.SECONDS);
        assertFalse(startBarrier.isBroken());

        provider0.setValue("zero");
        provider1.setValue("one");
        latch.await(30, TimeUnit.SECONDS);
    }
}
