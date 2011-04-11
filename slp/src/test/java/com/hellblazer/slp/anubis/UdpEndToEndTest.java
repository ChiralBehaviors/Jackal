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
import static java.util.Arrays.asList;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import junit.framework.TestCase;

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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.hellblazer.jackal.annotations.DeployedPostProcessor;
import com.hellblazer.jackal.gossip.configuration.ControllerGossipConfiguration;
import com.hellblazer.jackal.gossip.configuration.GossipConfiguration;
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
public class UdpEndToEndTest extends TestCase {
    static class Event {
        final EventType           type;
        final UUID                registration;
        final ServiceURL          url;
        final Map<String, Object> properties;

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
        int                member;
        CountDownLatch     latch;
        ApplicationContext context;
        Set<Event>         events = new CopyOnWriteArraySet<UdpEndToEndTest.Event>();

        Listener(ApplicationContext context) {
            this.context = context;
            member = context.getBean(Identity.class).id;
        }

        @Override
        public void serviceChanged(ServiceEvent event) {
            log.fine("updated <" + member + "> with: " + event);
            Event marked = new Event(event);
            if (events.add(marked)) {
                if (latch != null) {
                    latch.countDown();
                }
            } else {
                // System.err.println("recevied duplicate: " + marked);
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
        CountDownLatch latch       = INITIAL_LATCH;
        int            cardinality = CONFIGS.length;

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
            return new AnubisScope(stateName(), locator(),
                                   Executors.newSingleThreadExecutor(),
                                   uuidGenerator());
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
            return new InetSocketAddress(contactHost(), testPort1);
        }

        InetSocketAddress seedContact2() throws UnknownHostException {
            return new InetSocketAddress(contactHost(), testPort2);
        }
    }

    static int                           testPort1;

    static int                           testPort2;

    static {
        String port = System.getProperty("com.hellblazer.jackal.gossip.test.port.1",
                                         "24010");
        testPort1 = Integer.parseInt(port);
        port = System.getProperty("com.hellblazer.jackal.gossip.test.port.2",
                                  "24020");
        testPort2 = Integer.parseInt(port);
    }

    private static final Logger          log     = Logger.getLogger(UdpEndToEndTest.class.getCanonicalName());
    static final Random                  RANDOM  = new Random(666);
    static CountDownLatch                INITIAL_LATCH;
    final static Class<?>[]              CONFIGS = { node0.class, node1.class,
            node2.class, node3.class, node4.class, node5.class, node6.class,
            node7.class, node8.class, node9.class };

    ConfigurableApplicationContext       controllerContext;
    List<ConfigurableApplicationContext> memberContexts;
    MyController                         controller;
    List<Node>                           partition;

    public void testSmoke() throws Exception {
        String memberIdKey = "test.member.id";
        ServiceURL url = new ServiceURL("service:http://foo.bar/drink-me");
        List<Listener> listeners = new ArrayList<Listener>();
        for (ApplicationContext context : memberContexts) {
            CountDownLatch latch = new CountDownLatch(memberContexts.size());
            Listener listener = new Listener(context);
            listener.latch = latch;
            listeners.add(listener);
            listener.register(getQuery("*"));
        }
        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            context.getBean(ServiceScope.class).register(url, properties);
        }
        for (Listener listener : listeners) {
            assertTrue("listener <" + listener.member
                               + "> has not received all notifications",
                       listener.latch.await(30, TimeUnit.SECONDS));
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

    public void teztSymmetricPartition() throws Exception {
        int minorPartitionSize = CONFIGS.length / 2;
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
            CountDownLatch latch = new CountDownLatch(memberContexts.size() * 2);
            Listener listener = new Listener(context);
            listener.latch = latch;
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
        assertTrue("minor partition A did not stabilize",
                   latchA.await(30, TimeUnit.SECONDS));
        log.info("Awaiting stabilty of minor partition B");
        assertTrue("minor partition B did not stabilize",
                   latchB.await(30, TimeUnit.SECONDS));

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
        CountDownLatch latch = new CountDownLatch(CONFIGS.length);
        for (Node node : partition) {
            node.latch = latch;
            node.cardinality = CONFIGS.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stabilty of reformed major partition");
        assertTrue("reformed partition did not stabilize",
                   latch.await(30, TimeUnit.SECONDS));

        for (Listener listener : listeners) {
            assertTrue("listener <" + listener.member
                               + "> has not received all notifications",
                       listener.latch.await(30, TimeUnit.SECONDS));
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

    public void teztAsymmetricPartition() throws Exception {
        int minorPartitionSize = CONFIGS.length / 2;
        BitView A = new BitView();
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
            }
        }

        String memberIdKey = "test.member.id";
        String roundKey = "test.round";
        ServiceURL url = new ServiceURL("service:http://foo.bar/drink-me");
        List<Listener> listeners = new ArrayList<Listener>();
        for (ApplicationContext context : memberContexts) {
            CountDownLatch latch = new CountDownLatch(memberContexts.size() * 2);
            Listener listener = new Listener(context);
            listener.latch = latch;
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
        assertTrue("minor partition did not stabilize",
                   latchA.await(30, TimeUnit.SECONDS));

        for (Node member : partitionA) {
            assertEquals(A, member.getPartition());
        }

        for (ApplicationContext context : memberContexts) {
            HashMap<String, Object> properties = new HashMap<String, Object>();
            properties.put(memberIdKey, context.getBean(Identity.class).id);
            properties.put(roundKey, 2);
            context.getBean(ServiceScope.class).register(url, properties);
        }

        // reform
        CountDownLatch latch = new CountDownLatch(CONFIGS.length);
        for (Node node : partition) {
            node.latch = latch;
            node.cardinality = CONFIGS.length;
        }

        controller.clearPartitions();
        log.info("Awaiting stabilty of reformed major partition");
        assertTrue("reformed partition did not stabilize",
                   latch.await(30, TimeUnit.SECONDS));

        for (Listener listener : listeners) {
            assertTrue("listener <" + listener.member
                               + "> has not received all notifications",
                       listener.latch.await(30, TimeUnit.SECONDS));
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

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        log.info("Setting up initial partition");
        INITIAL_LATCH = new CountDownLatch(CONFIGS.length);
        controllerContext = new AnnotationConfigApplicationContext(
                                                                   MyControllerConfig.class);
        memberContexts = createMembers();
        controller = (MyController) controllerContext.getBean(Controller.class);
        log.info("Awaiting initial partition stability");
        boolean success = false;
        try {
            success = INITIAL_LATCH.await(120, TimeUnit.SECONDS);
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
        INITIAL_LATCH = null;
        Thread.sleep(2000);
    }

    private List<ConfigurableApplicationContext> createMembers() {
        ArrayList<ConfigurableApplicationContext> contexts = new ArrayList<ConfigurableApplicationContext>();
        for (Class<?> config : CONFIGS) {
            contexts.add(new AnnotationConfigApplicationContext(config));
        }
        return contexts;
    }

    private String getQuery(String serviceType) {
        return "(" + SERVICE_TYPE + "=" + serviceType + ")";
    }
}
