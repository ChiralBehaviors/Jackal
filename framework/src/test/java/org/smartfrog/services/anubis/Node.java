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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import org.smartfrog.services.anubis.partition.Partition;
import org.smartfrog.services.anubis.partition.PartitionNotification;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Node {
    static Random RANDOM = new Random(666);

    public static int getIdentity(String instanceString) {
        return Integer.parseInt(instanceString.substring(0,
                                                         instanceString.indexOf('/')));
    }

    private final AnnotationConfigApplicationContext context;
    private final int                                maxSleep;
    private final int                                messagesToSend;
    private final CountDownLatch                     endLatch;
    private final CountDownLatch                     startLatch;
    private final CountDownLatch                     launchLatch;
    private final int                                cardinality;
    private final ArrayList<SendHistory>             sendHistory        = new ArrayList<SendHistory>();
    private final Map<Integer, List<ValueHistory>>   receiveHistory     = new HashMap<Integer, List<ValueHistory>>();

    private final Map<Integer, CountDownLatch>       msgReceivedLatches = new HashMap<Integer, CountDownLatch>();
    private final Partition                          partition;
    private final int                                identity;
    private volatile View                            view;

    public Node(AnnotationConfigApplicationContext context, int c,
                CountDownLatch launchLatch, CountDownLatch startLatch,
                CountDownLatch endLatch, int maxSleep, int messageCount)
                                                                        throws Exception {
        this.context = context;
        cardinality = c;
        this.launchLatch = launchLatch;
        this.startLatch = startLatch;
        this.endLatch = endLatch;
        this.maxSleep = maxSleep;
        messagesToSend = messageCount;
        for (int i = 0; i < cardinality; i++) {
            receiveHistory.put(i, new CopyOnWriteArrayList<ValueHistory>());
            msgReceivedLatches.put(i, new CountDownLatch(messageCount));
        }
        identity = context.getBean(Identity.class).id;
        partition = context.getBean(Partition.class);
        partition.register(new PartitionNotification() {

            @Override
            public void objectNotification(Object obj, int sender, long time) {
                receiveHistory.get(sender).add(new ValueHistory(sender, obj,
                                                                time,
                                                                Action.NEW));
                msgReceivedLatches.get(sender).countDown();
            }

            @Override
            public void partitionNotification(View view, int leader) {
                Node.this.view = view;
                if (view.isStable() && view.cardinality() == cardinality) {
                    // System.out.println("Launching: " + view + " : " + instance);
                    Node.this.launchLatch.countDown();
                } else {
                    // System.out.println("Not launching: " + view + " : " + instance);
                }
            }
        });
    }

    public int getIdentity() {
        return identity;
    }

    public List<SendHistory> getSendHistory() {
        return sendHistory;
    }

    public List<ValueHistory> getValueHistory(int node) {
        return receiveHistory.get(node);
    }

    public boolean isMissingMessages() {
        for (CountDownLatch latch : msgReceivedLatches.values()) {
            if (latch.getCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public String report() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Integer, CountDownLatch> entry : msgReceivedLatches.entrySet()) {
            if (entry.getValue().getCount() > 0) {
                builder.append(String.format("Node %s didn't receive %s messages from %s",
                                             identity,
                                             entry.getValue().getCount(),
                                             entry.getKey()));
                builder.append(", ");
            }
        }
        return builder.toString();
    }

    public void shutDown() {
        context.close();
    }

    public void start() {
        Thread daemon = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startLatch.countDown();
                    startLatch.await();
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    return;
                }
                for (int counter = 0; counter < messagesToSend; counter++) {
                    try {
                        Thread.sleep(RANDOM.nextInt(maxSleep));
                    } catch (InterruptedException e) {
                        return;
                    }
                    SendHistory sent = new SendHistory(
                                                       System.currentTimeMillis(),
                                                       counter);
                    send(counter);
                    sendHistory.add(sent);
                }
                try {
                    for (CountDownLatch latch : msgReceivedLatches.values()) {
                        latch.await();
                    }
                    endLatch.countDown();
                    endLatch.await();
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "Run Thread for: " + context.getBean(Identity.class).toString());

        daemon.setDaemon(true);
        daemon.start();
    }

    private void send(int counter) {
        for (int n : view.toBitSet()) {
            if (identity == n) {
                receiveHistory.get(identity).add(new ValueHistory(
                                                                  identity,
                                                                  counter,
                                                                  System.currentTimeMillis(),
                                                                  Action.NEW));
                msgReceivedLatches.get(identity).countDown();
            } else {
                MessageConnection connection = partition.connect(n);
                if (connection == null) {
                    System.out.println(String.format("Node %s cannot connect to: %s",
                                                     identity, n));
                }
                connection.sendObject(counter);
            }
        }
    }
}