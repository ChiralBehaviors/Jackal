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
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class SmokeTest extends TestCase {
    static class noTestCfg extends BasicConfiguration {

        @Override
        public boolean getTestable() {
            return false;
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
        public int heartbeatGroupTTL() {
            return 0;
        }

    }

    @Configuration
    static class testA extends noTestCfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class testB extends noTestCfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class testC extends noTestCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class testD extends noTestCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class testE extends noTestCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class testF extends noTestCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class testG extends noTestCfg {
        @Override
        public int node() {
            return 6;
        }
    }

    public void testInProcess() throws Exception {
        String stateName = "Whip It";
        int maxSleep = 500;
        int messageCount = 10;
        ArrayList<Node> nodes = new ArrayList<Node>();
        Class<?>[] configurations = new Class[] { testA.class, testB.class,
                                                 testC.class, testD.class,
                                                 testE.class, testF.class,
                                                 testG.class };
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
                assertNotNull("Received no history from "
                                      + sender.getInstance(), received);
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
}
