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

import static com.hellblazer.slp.ServiceScope.SERVICE_TYPE;
import static com.hellblazer.slp.anubis.AnubisScope.MEMBER_IDENTITY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.hellblazer.jackal.testUtil.TestController;
import com.hellblazer.jackal.testUtil.TestNode;
import com.hellblazer.slp.InvalidSyntaxException;
import com.hellblazer.slp.ServiceEvent;
import com.hellblazer.slp.ServiceEvent.EventType;
import com.hellblazer.slp.ServiceListener;
import com.hellblazer.slp.ServiceScope;
import com.hellblazer.slp.ServiceURL;

/**
 * 
 * Functionally test the scope across multiple members in different failure
 * scenarios.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
abstract public class EndToEndTest extends TestCase {

    static class Listener implements ServiceListener {
        final Logger                       log     = LoggerFactory.getLogger(ServiceListener.class);
        ApplicationContext                 context;
        List<ServiceEvent>                 events  = new CopyOnWriteArrayList<ServiceEvent>();
        final Map<Integer, CountDownLatch> latches = new HashMap<Integer, CountDownLatch>();
        int                                member;

        Listener(ApplicationContext context, int cardinality) {
            this.context = context;
            member = context.getBean(Identity.class).id;
            reset(cardinality);
        }

        public void reset(BitView b) {
            events = new CopyOnWriteArrayList<ServiceEvent>();
            latches.clear();
            for (int i : b) {
                latches.put(i, new CountDownLatch(1));
            }
        }

        @Override
        public void serviceChanged(ServiceEvent event) {
            log.trace("updated <" + member + "> with: " + event);
            if (events.add(event)) {
                Integer id = (Integer) event.getReference().getProperties().get(MEMBER_IDENTITY);
                CountDownLatch latch = latches.get(id);
                if (latch == null) {
                    log.trace("uknown update from: " + id);
                } else {
                    latch.countDown();
                }
            } else {
                // System.err.println("recevied duplicate: " + marked);
            }
        }

        @Override
        public String toString() {
            return "Listener [member=" + member + "]";
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            boolean success = true;
            for (CountDownLatch latch : latches.values()) {
                success &= latch.await(timeout, unit);
            }
            return success;
        }

        void register(String query) throws BeansException,
                                   InvalidSyntaxException {
            context.getBean(ServiceScope.class).addServiceListener(this, query);
        }

        void reset(int cardinality) {
            events = new CopyOnWriteArrayList<ServiceEvent>();
            for (int i = 0; i < cardinality; i++) {
                latches.put(i, new CountDownLatch(1));
            }
        }

        void unregister() {
            context.getBean(ServiceScope.class).removeServiceListener(this);
        }
    }

    static final Random                  RANDOM  = new Random(666);

    final Class<?>[]                     configs = getConfigs();
    List<ConnectionSet>                  connectionSets;
    TestController                       controller;
    ConfigurableApplicationContext       controllerContext;
    CountDownLatch                       initialLatch;
    final Logger                         log     = getLogger();
    List<ConfigurableApplicationContext> memberContexts;
    List<TestNode>                       partition;

    public void testAsymmetricPartition() throws Exception {
        int minorPartitionSize = configs.length / 2;
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
        }

        String memberIdKey = "test.member.id";
        String roundKey = "test.round";
        ServiceURL url = new ServiceURL("service:http://foo.bar/drink-me");
        List<Listener> listeners = new ArrayList<Listener>();
        for (ApplicationContext context : memberContexts) {
            Listener listener = new Listener(context, memberContexts.size());
            listeners.add(listener);
            listener.register(getQuery("*"));
        }
        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            properties.put(roundKey, 1);
            context.getBean(ServiceScope.class).register(url, properties);
        }

        for (Listener listener : listeners) {
            assertTrue(String.format("Listener: %s did not receive all events",
                                     listener),
                       listener.await(60, TimeUnit.SECONDS));
        }

        for (Listener listener : listeners) {
            assertEquals("listener <"
                                 + listener.member
                                 + "> has received an invalid number of notifications :"
                                 + listener.events, listeners.size(),
                         listener.events.size());
            HashSet<Integer> sent = new HashSet<Integer>();
            for (ServiceEvent event : listener.events) {
                assertEquals(EventType.REGISTERED, event.getType());
                assertEquals(url, event.getReference().getUrl());
                sent.add((Integer) event.getReference().getProperties().get(memberIdKey));
                assertEquals(event.getReference().getProperties().get(AnubisScope.MEMBER_IDENTITY),
                             event.getReference().getProperties().get(memberIdKey));
            }
            assertEquals("listener <" + listener.member
                         + "> did not receive messages from all members: "
                         + sent, listeners.size(), sent.size());
        }

        i = 0;
        for (Listener listener : listeners) {
            if (i++ % 2 == 0) {
                listener.reset(A);
            } else {
                listener.reset(B);
            }
        }

        log.info("asymmetric partitioning: " + A);
        controller.asymPartition(A);
        log.info("Awaiting stabilty of minor partition A");
        assertTrue("minor partition A did not stabilize",
                   latchA.await(60, TimeUnit.SECONDS));

        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            properties.put(roundKey, 2);
            context.getBean(ServiceScope.class).register(url, properties);
        }

        for (Listener listener : listeners) {
            if (A.contains(listener.member)) {
                assertTrue(String.format("Listener: %s did not receive all events",
                                         listener),
                           listener.await(60, TimeUnit.SECONDS));
            }
        }

        for (Listener listener : listeners) {
            if (A.contains(listener.member)) {
                assertEquals("listener <"
                                     + listener.member
                                     + "> has received more notifications than expected :"
                                     + listener.events, A.cardinality(),
                             listener.events.size());
                HashSet<Integer> sent = new HashSet<Integer>();
                for (ServiceEvent event : listener.events) {
                    assertEquals(EventType.REGISTERED, event.getType());
                    assertEquals(url, event.getReference().getUrl());
                    sent.add((Integer) event.getReference().getProperties().get(memberIdKey));
                    assertEquals(event.getReference().getProperties().get(AnubisScope.MEMBER_IDENTITY),
                                 event.getReference().getProperties().get(memberIdKey));
                }
                assertEquals("listener <" + listener.member
                             + "> did not receive messages from all members: "
                             + sent, A.cardinality(), sent.size());
            }
        }

        // reform
        CountDownLatch latch = new CountDownLatch(configs.length);
        for (TestNode node : partition) {
            node.latch = latch;
            node.cardinality = configs.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stabilty of reformed major partition");
        assertTrue("reformed partition did not stabilize",
                   latch.await(60, TimeUnit.SECONDS));
    }

    public void testSmoke() throws Exception {
        String memberIdKey = "test.member.id";
        ServiceURL url = new ServiceURL("service:http://foo.bar/drink-me");
        List<Listener> listeners = new ArrayList<Listener>();
        for (ApplicationContext context : memberContexts) {
            Listener listener = new Listener(context, memberContexts.size());
            listeners.add(listener);
            listener.register(getQuery("*"));
        }
        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            context.getBean(ServiceScope.class).register(url, properties);
        }

        for (Listener listener : listeners) {
            assertTrue(String.format("Listener: %s did not receive all events",
                                     listener),
                       listener.await(60, TimeUnit.SECONDS));
        }

        for (Listener listener : listeners) {
            assertEquals(listeners.size(), listener.events.size());
            HashSet<Integer> sent = new HashSet<Integer>();
            for (ServiceEvent event : listener.events) {
                assertEquals(EventType.REGISTERED, event.getType());
                assertEquals(url, event.getReference().getUrl());
                sent.add((Integer) event.getReference().getProperties().get(memberIdKey));
                assertEquals(event.getReference().getProperties().get(AnubisScope.MEMBER_IDENTITY),
                             event.getReference().getProperties().get(memberIdKey));
            }
            assertEquals(listeners.size(), sent.size());
        }
    }

    public void testSymmetricPartition() throws Exception {
        int minorPartitionSize = configs.length / 2;
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
        }

        String memberIdKey = "test.member.id";
        String roundKey = "test.round";
        ServiceURL url = new ServiceURL("service:http://foo.bar/drink-me");
        List<Listener> listeners = new ArrayList<Listener>();
        for (ApplicationContext context : memberContexts) {
            Listener listener = new Listener(context, memberContexts.size());
            listeners.add(listener);
            listener.register(getQuery("*"));
        }
        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            properties.put(roundKey, 1);
            context.getBean(ServiceScope.class).register(url, properties);
        }

        for (Listener listener : listeners) {
            assertTrue(String.format("Listener: %s did not receive all events",
                                     listener),
                       listener.await(60, TimeUnit.SECONDS));
        }

        for (Listener listener : listeners) {
            assertEquals("listener <"
                                 + listener.member
                                 + "> has received an invalid number of notifications: "
                                 + listener.events, listeners.size(),
                         listener.events.size());
            HashSet<Integer> sent = new HashSet<Integer>();
            for (ServiceEvent event : listener.events) {
                assertEquals(EventType.REGISTERED, event.getType());
                assertEquals(url, event.getReference().getUrl());
                sent.add((Integer) event.getReference().getProperties().get(memberIdKey));
                assertEquals(event.getReference().getProperties().get(AnubisScope.MEMBER_IDENTITY),
                             event.getReference().getProperties().get(memberIdKey));
            }
            assertEquals("listener <" + listener.member
                         + "> did not receive messages from all members: "
                         + sent, listeners.size(), sent.size());
        }

        i = 0;
        for (Listener listener : listeners) {
            if (i++ % 2 == 0) {
                listener.reset(A);
            } else {
                listener.reset(B);
            }
        }

        log.info("symmetric partitioning: " + A);
        controller.symPartition(A);
        log.info("Awaiting stabilty of minor partition A");
        boolean stability = latchA.await(60, TimeUnit.SECONDS);
        assertTrue("minor partition A did not stabilize", stability);
        log.info("Awaiting stabilty of minor partition B");
        assertTrue("minor partition B did not stabilize",
                   latchB.await(60, TimeUnit.SECONDS));

        for (TestNode member : partitionA) {
            assertEquals(A, member.getPartition());
        }

        for (TestNode member : partitionB) {
            assertEquals(B, member.getPartition());
        }

        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            properties.put(roundKey, 2);
            context.getBean(ServiceScope.class).register(url, properties);
        }

        for (Listener listener : listeners) {
            assertTrue(String.format("Listener: %s did not receive all events",
                                     listener),
                       listener.await(60, TimeUnit.SECONDS));
        }

        for (Listener listener : listeners) {
            assertEquals("listener <"
                                 + listener.member
                                 + "> has received more notifications than expected: "
                                 + listener.events, A.cardinality(),
                         listener.events.size());
            HashSet<Integer> sent = new HashSet<Integer>();
            for (ServiceEvent event : listener.events) {
                assertEquals(EventType.REGISTERED, event.getType());
                assertEquals(url, event.getReference().getUrl());
                sent.add((Integer) event.getReference().getProperties().get(memberIdKey));
                assertEquals(event.getReference().getProperties().get(AnubisScope.MEMBER_IDENTITY),
                             event.getReference().getProperties().get(memberIdKey));
            }
            assertEquals("listener <" + listener.member
                         + "> did not receive messages from all members: "
                         + sent, A.cardinality(), sent.size());
        }

        // reform
        CountDownLatch latch = new CountDownLatch(configs.length);
        for (TestNode node : partition) {
            node.latch = latch;
            node.cardinality = configs.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stabilty of reformed major partition");
        assertTrue("reformed partition did not stabilize",
                   latch.await(60, TimeUnit.SECONDS));
    }

    private List<ConfigurableApplicationContext> createMembers() {
        ArrayList<ConfigurableApplicationContext> contexts = new ArrayList<ConfigurableApplicationContext>();
        for (Class<?> config : configs) {
            contexts.add(new AnnotationConfigApplicationContext(config));
        }
        return contexts;
    }

    private String getQuery(String serviceType) {
        return "(" + SERVICE_TYPE + "=" + serviceType + ")";
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
        controller = controllerContext.getBean(TestController.class);
        controller.cardinality = configs.length;
        controller.latch = initialLatch;
        memberContexts = createMembers();
        log.info("Awaiting initial partition stability");
        boolean success = false;
        connectionSets = new ArrayList<ConnectionSet>();
        for (ConfigurableApplicationContext context : memberContexts) {
            connectionSets.add(context.getBean(ConnectionSet.class));
        }
        try {
            success = initialLatch.await(120, TimeUnit.SECONDS);
            assertTrue("Initial partition did not stabilize", success);
            log.info("Initial partition stable");
            partition = new ArrayList<TestNode>();
            for (ConfigurableApplicationContext context : memberContexts) {
                partition.add((TestNode) controller.getNode(context.getBean(Identity.class)));
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
            for (ConfigurableApplicationContext context : memberContexts) {
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
        Thread.sleep(2000);
    }
}
