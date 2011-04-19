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
import java.util.Set;
import java.util.Timer;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.NodeData;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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
    static class Event {
        final Map<String, Object> properties;
        final UUID                registration;
        final EventType           type;
        final ServiceURL          url;

        Event(ServiceEvent event) {
            type = event.getType();
            url = event.getReference().getUrl();
            properties = new HashMap<String, Object>(
                                                     event.getReference().getProperties());
            registration = ((ServiceReferenceImpl) event.getReference()).getRegistration();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Event other = (Event) obj;
            if (registration == null) {
                if (other.registration != null) {
                    return false;
                }
            } else if (!registration.equals(other.registration)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                     + (registration == null ? 0 : registration.hashCode());
            return result;
        }

        @Override
        public String toString() {
            return "Event [type=" + type + ", registration=" + registration
                   + ", url=" + url + ", properties=" + properties + "]";
        }
    }

    static class Listener implements ServiceListener {
        final Logger                       log     = Logger.getLogger(ServiceListener.class.getCanonicalName());
        ApplicationContext                 context;
        Set<Event>                         events  = new CopyOnWriteArraySet<Event>();
        final Map<Integer, CountDownLatch> latches = new HashMap<Integer, CountDownLatch>();
        int                                member;

        Listener(ApplicationContext context, int cardinality, int expectedCount) {
            this.context = context;
            member = context.getBean(Identity.class).id;
            for (int i = 0; i < cardinality; i++) {
                latches.put(i, new CountDownLatch(expectedCount));
            }
        }

        @Override
        public void serviceChanged(ServiceEvent event) {
            log.fine("updated <" + member + "> with: " + event);
            Event marked = new Event(event);
            if (events.add(marked)) {
                Integer id = (Integer) event.getReference().getProperties().get(MEMBER_IDENTITY);
                CountDownLatch latch = latches.get(id);
                if (latch == null) {
                    throw new IllegalStateException("unknown identity: " + id);
                }
                latch.countDown();
            } else {
                // System.err.println("recevied duplicate: " + marked);
            }
        }

        void await(long timeout, TimeUnit unit) throws InterruptedException {
            for (CountDownLatch latch : latches.values()) {
                latch.await(timeout, unit);
            }
        }

        void register(String query) throws BeansException,
                                   InvalidSyntaxException {
            context.getBean(ServiceScope.class).addServiceListener(this, query);
        }

        void unregister() {
            context.getBean(ServiceScope.class).removeServiceListener(this);
        }
    }

    static class MyController extends Controller {
        int            cardinality;
        CountDownLatch latch;

        public MyController(Timer timer, long checkPeriod, long expirePeriod,
                            Identity partitionIdentity, long heartbeatTimeout,
                            long heartbeatInterval) {
            super(timer, checkPeriod, expirePeriod, partitionIdentity,
                  heartbeatTimeout, heartbeatInterval);
        }

        @Override
        protected NodeData createNode(Heartbeat hb) {
            Node node = new Node(hb, this);
            node.cardinality = cardinality;
            node.latch = latch;
            return node;
        }

    }

    static class Node extends NodeData {
        int            cardinality;
        CountDownLatch latch;
        final Logger   log = Logger.getLogger(Node.class.getCanonicalName());

        public Node(Heartbeat hb, Controller controller) {
            super(hb, controller);
        }

        @Override
        protected void partitionNotification(View partition, int leader) {
            if (partition.isStable() && partition.cardinality() == cardinality) {
                log.fine("Stabilized: " + partition);
                latch.countDown();
            } else {
                log.fine(String.format("Notification: %s, stable: %s",
                                       partition, partition.isStable()));
            }
            super.partitionNotification(partition, leader);
        }
    }

    static final Random                  RANDOM  = new Random(666);
    final Class<?>[]                     configs = getConfigs();
    List<ConnectionSet>                  connectionSets;

    MyController                         controller;
    ConfigurableApplicationContext       controllerContext;
    CountDownLatch                       initialLatch;
    final Logger                         log     = getLogger();

    List<ConfigurableApplicationContext> memberContexts;

    List<Node>                           partition;

    public void testAsymmetricPartition() throws Exception {
        int minorPartitionSize = configs.length / 2;
        BitView A = new BitView();
        BitView B = new BitView();
        CountDownLatch latchA = new CountDownLatch(minorPartitionSize);
        List<Node> partitionA = new ArrayList<Node>();

        CountDownLatch latchB = new CountDownLatch(minorPartitionSize);
        List<Node> partitionB = new ArrayList<Node>();

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

        String memberIdKey = "test.member.id";
        String roundKey = "test.round";
        ServiceURL url = new ServiceURL("service:http://foo.bar/drink-me");
        List<Listener> listeners = new ArrayList<Listener>();
        for (ApplicationContext context : memberContexts) {
            Listener listener = new Listener(context, memberContexts.size(), 2);
            listeners.add(listener);
            listener.register(getQuery("*"));
        }
        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            properties.put(roundKey, 1);
            context.getBean(ServiceScope.class).register(url, properties);
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

        // reform
        CountDownLatch latch = new CountDownLatch(configs.length);
        for (Node node : partition) {
            node.latch = latch;
            node.cardinality = configs.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stabilty of reformed major partition");
        assertTrue("reformed partition did not stabilize",
                   latch.await(30, TimeUnit.SECONDS));

        for (Listener listener : listeners) {
            listener.await(30, TimeUnit.SECONDS);
            assertEquals("listener <"
                                 + listener.member
                                 + "> has received more notifications than expected ",
                         listeners.size() * 2, listener.events.size());
            HashSet<Integer> sent = new HashSet<Integer>();
            for (Event event : listener.events) {
                assertEquals(EventType.REGISTERED, event.type);
                assertEquals(url, event.url);
                sent.add((Integer) event.properties.get(memberIdKey));
                assertEquals(event.properties.get(AnubisScope.MEMBER_IDENTITY),
                             event.properties.get(memberIdKey));
            }
            assertEquals("listener <" + listener.member
                         + "> did not receive messages from all members: "
                         + sent, listeners.size(), sent.size());
        }
    }

    public void testSmoke() throws Exception {
        String memberIdKey = "test.member.id";
        ServiceURL url = new ServiceURL("service:http://foo.bar/drink-me");
        List<Listener> listeners = new ArrayList<Listener>();
        for (ApplicationContext context : memberContexts) {
            Listener listener = new Listener(context, memberContexts.size(), 1);
            listeners.add(listener);
            listener.register(getQuery("*"));
        }
        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            context.getBean(ServiceScope.class).register(url, properties);
        }
        for (Listener listener : listeners) {
            listener.await(30, TimeUnit.SECONDS);
            assertEquals(listeners.size(), listener.events.size());
            HashSet<Integer> sent = new HashSet<Integer>();
            for (Event event : listener.events) {
                assertEquals(EventType.REGISTERED, event.type);
                assertEquals(url, event.url);
                sent.add((Integer) event.properties.get(memberIdKey));
                assertEquals(event.properties.get(AnubisScope.MEMBER_IDENTITY),
                             event.properties.get(memberIdKey));
            }
            assertEquals(listeners.size(), sent.size());
        }
    }

    public void testSymmetricPartition() throws Exception {
        int minorPartitionSize = configs.length / 2;
        BitView A = new BitView();
        BitView B = new BitView();
        CountDownLatch latchA = new CountDownLatch(minorPartitionSize);
        List<Node> partitionA = new ArrayList<Node>();

        CountDownLatch latchB = new CountDownLatch(minorPartitionSize);
        List<Node> partitionB = new ArrayList<Node>();

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

        String memberIdKey = "test.member.id";
        String roundKey = "test.round";
        ServiceURL url = new ServiceURL("service:http://foo.bar/drink-me");
        List<Listener> listeners = new ArrayList<Listener>();
        for (ApplicationContext context : memberContexts) {
            Listener listener = new Listener(context, memberContexts.size(), 2);
            listeners.add(listener);
            listener.register(getQuery("*"));
        }
        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            properties.put(roundKey, 1);
            context.getBean(ServiceScope.class).register(url, properties);
        }
        log.info("symmetric partitioning: " + A);
        controller.symPartition(A);
        log.info("Awaiting stabilty of minor partition A");
        boolean stability = latchA.await(60, TimeUnit.SECONDS);
        assertTrue("minor partition A did not stabilize", stability);
        log.info("Awaiting stabilty of minor partition B");
        assertTrue("minor partition B did not stabilize",
                   latchB.await(60, TimeUnit.SECONDS));

        for (Node member : partitionA) {
            assertEquals(A, member.getPartition());
        }

        for (Node member : partitionB) {
            assertEquals(B, member.getPartition());
        }

        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            properties.put(roundKey, 2);
            context.getBean(ServiceScope.class).register(url, properties);
        }

        // reform
        CountDownLatch latch = new CountDownLatch(configs.length);
        for (Node node : partition) {
            node.latch = latch;
            node.cardinality = configs.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stabilty of reformed major partition");
        assertTrue("reformed partition did not stabilize",
                   latch.await(30, TimeUnit.SECONDS));

        for (Listener listener : listeners) {
            listener.await(60, TimeUnit.SECONDS);
            assertEquals("listener <"
                                 + listener.member
                                 + "> has received more notifications than expected ",
                         listeners.size() * 2, listener.events.size());
            HashSet<Integer> sent = new HashSet<Integer>();
            for (Event event : listener.events) {
                assertEquals(EventType.REGISTERED, event.type);
                assertEquals(url, event.url);
                sent.add((Integer) event.properties.get(memberIdKey));
                assertEquals(event.properties.get(AnubisScope.MEMBER_IDENTITY),
                             event.properties.get(memberIdKey));
            }
            assertEquals("listener <" + listener.member
                         + "> did not receive messages from all members: "
                         + sent, listeners.size(), sent.size());
        }
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
        controller = (MyController) controllerContext.getBean(Controller.class);
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
            partition = new ArrayList<Node>();
            for (ConfigurableApplicationContext context : memberContexts) {
                partition.add((Node) controller.getNode(context.getBean(Identity.class)));
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
