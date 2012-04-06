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

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.hellblazer.jackal.testUtil.TestNodeCfg;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
abstract public class SmokeTest extends TestCase {
    private final Class<?>[] configurations = getConfigurations();

    public void testInProcess() throws Exception {
        TestNodeCfg.nextMagic();
        String stateName = "Whip It";
        int maxSleep = 500;
        int messageCount = 10;
        ArrayList<Node> nodes = new ArrayList<Node>();
        CountDownLatch launchLatch = new CountDownLatch(configurations.length);
        CountDownLatch startLatch = new CountDownLatch(configurations.length);
        CountDownLatch endLatch = new CountDownLatch(configurations.length);
        for (Class<?> config : configurations) {
            nodes.add(getNode(config, stateName, messageCount, maxSleep,
                              launchLatch, startLatch, endLatch,
                              configurations.length));
        }
        boolean stabilized = launchLatch.await(120, TimeUnit.SECONDS);
        assertTrue("Partition did not stabilize", stabilized);
        System.out.println("Partition stabilized");
        for (Node node : nodes) {
            node.start();
        }
        boolean started = startLatch.await(60, TimeUnit.SECONDS);
        assertTrue("Not all nodes started", started);
        System.out.println("Partition started");
        boolean ended = endLatch.await(60, TimeUnit.SECONDS);
        assertTrue("Not all messages were received: " + findMissing(nodes),
                   ended);
        for (Node node : nodes) {
            node.shutDown();
        }
        for (Node sender : nodes) {
            List<SendHistory> sent = sender.getSendHistory();
            assertEquals(messageCount, sent.size());
            for (Node receiver : nodes) {
                List<ValueHistory> received = receiver.getValueHistory(sender.getIdentity());
                assertNotNull("Received no history from "
                                      + sender.getIdentity(), received);
                int lastCounter = -1;
                boolean first = true;
                List<ValueHistory> filtered = new ArrayList<ValueHistory>();
                for (ValueHistory msg : received) {
                    if (msg.value == null) {
                        continue;
                    }
                    filtered.add(msg);
                    assertEquals(Action.NEW, msg.action);
                    if (first) {
                        first = false;
                        lastCounter = (Integer) msg.value;
                    } else {
                        int counter = (Integer) msg.value;
                        assertEquals(String.format("invalid msg received by %s : %s",
                                                   receiver.getIdentity(), msg),
                                     lastCounter + 1, counter);
                        lastCounter = counter;
                    }
                }
                for (int i = 0; i < sent.size(); i++) {
                    assertEquals(sent.get(i).value, filtered.get(i).value);
                }
            }
        }
    }

    private String findMissing(ArrayList<Node> nodes) {
        StringBuilder builder = new StringBuilder();
        for (Node node : nodes) {
            if (node.isMissingMessages()) {
                builder.append(node.report());
            }
        }
        return builder.toString();
    }

    abstract protected Class<?>[] getConfigurations();

    Node getNode(Class<?> config, String stateName, int messageCount,
                 int maxSleep, CountDownLatch launchLatch,
                 CountDownLatch startLatch, CountDownLatch endLatch,
                 int cardinality) throws Exception {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                                                                                        config);
        Node node = new Node(ctx, cardinality, launchLatch, startLatch,
                             endLatch, maxSleep, messageCount);
        return node;
    }
}
