/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.jackal.gossip;

import static com.hellblazer.jackal.nio.ServerSocketChannelHandler.bind;
import static com.hellblazer.jackal.nio.ServerSocketChannelHandler.getLocalAddress;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

import com.hellblazer.jackal.gossip.FailureDetectorFactory;
import com.hellblazer.jackal.gossip.Gossip;
import com.hellblazer.jackal.gossip.HeartbeatState;
import com.hellblazer.jackal.gossip.SystemView;
import com.hellblazer.jackal.gossip.fd.PhiFailureDetectorFactory;
import com.hellblazer.jackal.gossip.tcp.TcpCommunications;
import com.hellblazer.jackal.nio.SocketOptions;

/**
 * Basic end to end testing
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class EndToEndTest extends TestCase {

    public void testEnd2End() throws Exception {
        int membership = 64;
        int maxSeeds = 1;
        Random entropy = new Random();

        Receiver[] receivers = new Receiver[membership];
        for (int i = 0; i < membership; i++) {
            receivers[i] = new Receiver(membership, i);
        }
        List<Gossip> members = new ArrayList<Gossip>();
        Collection<InetSocketAddress> seedHosts = new ArrayList<InetSocketAddress>();
        for (int i = 0; i < membership; i++) {
            members.add(createCommunications(receivers[i], new Identity(666, i,
                                                                        1),
                                             seedHosts));
            if (i == 0) { // always add first member
                seedHosts.add(members.get(0).getLocalAddress());
            } else if (seedHosts.size() < maxSeeds) {
                // add the new member with probability of 25%
                if (entropy.nextDouble() < 0.25D) {
                    seedHosts.add(members.get(i).getLocalAddress());
                }
            }
        }
        System.out.println("Using " + seedHosts.size() + " seed hosts");
        try {
            for (Gossip member : members) {
                member.start();
            }
            for (int i = 0; i < membership; i++) {
                receivers[i].await(60, TimeUnit.SECONDS);
            }
            System.out.println();
            System.out.println("Initial iteration completed");
            for (int i = 0; i < 5; i++) {
                updateAndAwait(i, membership, receivers, members);
                System.out.println();
                System.out.println("Iteration " + (i + 2) + " completed");
            }
        } finally {
            System.out.println();
            for (Gossip member : members) {
                member.terminate();
            }
        }
    }

    protected void updateAndAwait(int iteration, int membership,
                                  Receiver[] receivers, List<Gossip> members)
                                                                             throws InterruptedException {
        int id = 0;
        for (Receiver receiver : receivers) {
            receiver.setLatches(id++);
        }
        id = 0;
        for (Gossip member : members) {
            member.sendHeartbeat(new HeartbeatState(new Identity(666, 0, 0),
                                                    false,
                                                    member.getLocalAddress(),
                                                    iteration + 1,
                                                    new NodeIdSet(), true,
                                                    new Identity(666, id++, 1),
                                                    null, false, null,
                                                    new NodeIdSet(), 0, 0));
        }
        for (int i = 0; i < membership; i++) {
            receivers[i].await(60, TimeUnit.SECONDS);
        }
    }

    protected Gossip createCommunications(HeartbeatReceiver receiver,
                                          Identity localIdentity,
                                          Collection<InetSocketAddress> seedHosts)
                                                                                  throws IOException {
        SocketOptions socketOptions = new SocketOptions();
        ServerSocketChannel channel = bind(socketOptions,
                                           new InetSocketAddress(0));
        TcpCommunications communications = new TcpCommunications(
                                                                 "Gossip Endpoint Handler for "
                                                                         + localIdentity,
                                                                 channel,
                                                                 getLocalAddress(channel),
                                                                 socketOptions,
                                                                 Executors.newFixedThreadPool(3,
                                                                                              new ThreadFactory() {

                                                                                                  @Override
                                                                                                  public Thread newThread(Runnable r) {
                                                                                                      Thread t = new Thread(
                                                                                                                            r);
                                                                                                      t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                                                                                                          @Override
                                                                                                          public void uncaughtException(Thread t,
                                                                                                                                        Throwable e) {
                                                                                                              e.printStackTrace();
                                                                                                          }
                                                                                                      });
                                                                                                      return t;
                                                                                                  }
                                                                                              }),
                                                                 Executors.newFixedThreadPool(2,
                                                                                              new ThreadFactory() {

                                                                                                  @Override
                                                                                                  public Thread newThread(Runnable r) {
                                                                                                      Thread t = new Thread(
                                                                                                                            r);
                                                                                                      t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                                                                                                          @Override
                                                                                                          public void uncaughtException(Thread t,
                                                                                                                                        Throwable e) {
                                                                                                              e.printStackTrace();
                                                                                                          }
                                                                                                      });
                                                                                                      return t;
                                                                                                  }
                                                                                              }));

        SystemView view = new SystemView(new Random(),
                                         communications.getLocalAddress(),
                                         seedHosts, 5000, 500000);
        FailureDetectorFactory fdFactory = new PhiFailureDetectorFactory(11,
                                                                         1000,
                                                                         3000,
                                                                         250,
                                                                         1.0,
                                                                         false);
        Gossip gossip = new Gossip(view, new Random(), communications, 4,
                                   TimeUnit.SECONDS, fdFactory);
        gossip.create(receiver);
        gossip.sendHeartbeat(new HeartbeatState(
                                                new Identity(666, 0, 0),
                                                false,
                                                communications.getLocalAddress(),
                                                0, new NodeIdSet(), true,
                                                localIdentity, null, false,
                                                null, new NodeIdSet(), 0, 0));
        return gossip;
    }

    private static class Receiver implements HeartbeatReceiver {
        private static final AtomicInteger count = new AtomicInteger();

        private final CountDownLatch[]     latches;

        Receiver(int members, int id) {
            super();
            latches = new CountDownLatch[members];
            setLatches(id);
        }

        void setLatches(int id) {
            for (int i = 0; i < latches.length; i++) {
                int count = i == id ? 0 : 1;
                latches[i] = new CountDownLatch(count);
            }
        }

        public void await(int timeout, TimeUnit unit)
                                                     throws InterruptedException {
            for (int i = 0; i < latches.length; i++) {
                latches[i].await(timeout, unit);
            }
        }

        @Override
        public boolean receiveHeartbeat(Heartbeat hb) {
            // System.out.println("Heartbeat received: " + hb);
            int currentCount = count.incrementAndGet();
            if (currentCount % 100 == 0) {
                System.out.print('.');
            } else if (currentCount % 1000 == 0) {
                System.out.println();
            }

            latches[hb.getSender().id].countDown();
            return false;
        }
    }
}
