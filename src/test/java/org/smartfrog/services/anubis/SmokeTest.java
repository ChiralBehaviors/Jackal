package org.smartfrog.services.anubis;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

public class SmokeTest extends TestCase {
    @Configuration
    static class testA extends BasicConfiguration {
        @Override
        public int getNode() throws UnknownHostException {
            return 0;
        }
    }

    @Configuration
    static class testB extends BasicConfiguration {
        @Override
        public int getNode() throws UnknownHostException {
            return 1;
        }
    }

    @Configuration
    static class testC extends BasicConfiguration {
        @Override
        public int getNode() throws UnknownHostException {
            return 2;
        }
    }

    @Configuration
    static class testD extends BasicConfiguration {
        @Override
        public int getNode() throws UnknownHostException {
            return 3;
        }
    }

    @Configuration
    static class testE extends BasicConfiguration {
        @Override
        public int getNode() throws UnknownHostException {
            return 4;
        }
    }

    @Configuration
    static class testF extends BasicConfiguration {
        @Override
        public int getNode() throws UnknownHostException {
            return 5;
        }
    }

    @Configuration
    static class testG extends BasicConfiguration {
        @Override
        public int getNode() throws UnknownHostException {
            return 6;
        }
    }

    Node getNode(Class<?> config, String stateName, int messageCount,
                 int maxSleep, CyclicBarrier startBarrier,
                 CyclicBarrier endBarrier) throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                                                                                        config);
        Node node = new Node(ctx, stateName);
        node.setStartBarrier(startBarrier);
        node.setEndBarrier(endBarrier);
        node.setMaxSleep(maxSleep);
        node.setMessagesToSend(messageCount);
        return node;
    }

    public void testInProcess() throws Exception {
        String stateName = "Whip It";
        int maxSleep = 500;
        int messageCount = 10;
        ArrayList<Node> nodes = new ArrayList<Node>();
        Class<?>[] configurations = new Class[] { testA.class, testB.class,
                                                 testC.class, testD.class,
                                                 testE.class, testF.class,
                                                 testG.class};
        CyclicBarrier startBarrier = new CyclicBarrier(configurations.length);
        CyclicBarrier endBarrier = new CyclicBarrier(configurations.length + 1);
        for (Class<?> config : configurations) {
            nodes.add(getNode(config, stateName, messageCount, maxSleep,
                              startBarrier, endBarrier));
        }
        for (Node node : nodes) {
            node.start();
        }
        endBarrier.await(2, TimeUnit.MINUTES);
        for (Node node : nodes) {
            node.shutDown();
        }
        for (Node sender : nodes) {
            List<SendHistory> sent = sender.getSendHistory();
            for (Node receiver : nodes) {
                List<ValueHistory> received = receiver.getValueHistory(sender.getInstance());
                assertNotNull("Received no history from " + sender.getInstance(), received);
                int lastCounter = -1;
                boolean first = true;
                boolean second = false;
                for (ValueHistory msg : received) {
                    assertEquals(Action.NEW, msg.action);
                    if (first) {
                        first = false;
                        second = true;
                        // First message is the existence of the holder with no
                        // value set
                        assertNull(msg.value);
                    } else if (second) {
                        second = false;
                        lastCounter = (Integer) msg.value;
                    } else {
                        int counter = (Integer) msg.value;
                        assertEquals(String.format("invalid msg received by %s : %s",
                                                   receiver.getInstance(), msg),
                                     lastCounter + 1, counter);
                        lastCounter = counter;
                    }
                }
                assertEquals(sent.size() + 1, received.size());
                for (int i = 0; i < sent.size(); i++) {
                    assertEquals(sent.get(i).value, received.get(i + 1).value);
                }
            }
        }
    }
}
