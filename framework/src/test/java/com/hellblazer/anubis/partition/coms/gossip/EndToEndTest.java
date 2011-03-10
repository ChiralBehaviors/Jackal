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
package com.hellblazer.anubis.partition.coms.gossip;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

import com.hellblazer.anubis.basiccomms.nio.SocketOptions;

/**
 * Basic end to end testing
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class EndToEndTest extends TestCase {

    public void testEnd2End() throws Exception {
        int membership = 64;
        Receiver[] receivers = new Receiver[membership];
        for (int i = 0; i < membership; i++) {
            receivers[i] = new Receiver(membership, i);
        }
        List<Communications> members = new ArrayList<Communications>();
        Collection<InetSocketAddress> seedHosts = new ArrayList<InetSocketAddress>();
        for (int i = 0; i < membership; i++) {
            members.add(createCommunications(receivers[i], new Identity(666, i,
                                                                        1),
                                             seedHosts));
            if (members.size() == 1) {
                seedHosts.add(members.get(0).getLocalAddress());
            }
        }

        try {
            for (Communications member : members) {
                member.start();
            }
            for (int i = 0; i < membership; i++) {
                receivers[i].await(60, TimeUnit.SECONDS);
            }
        } finally {
            for (Communications member : members) {
                member.terminate();
            }
        }
    }

    protected Communications createCommunications(HeartbeatReceiver receiver,
                                                  Identity localIdentity,
                                                  Collection<InetSocketAddress> seedHosts)
                                                                                          throws IOException {
        Communications communications = new Communications(
                                                           receiver,
                                                           1,
                                                           TimeUnit.SECONDS,
                                                           new InetSocketAddress(
                                                                                 0),
                                                           new SocketOptions(),
                                                           Executors.newFixedThreadPool(3),
                                                           Executors.newSingleThreadExecutor());

        SystemView view = new SystemView(new Random(666),
                                         communications.getLocalAddress(),
                                         seedHosts, 5000, 500000);
        Gossip gossip = new Gossip(communications, view, new Random(666), 11,
                                   localIdentity);
        communications.setGossip(gossip);
        communications.updateHeartbeat(new HeartbeatState(
                                                          new Identity(666, 0,
                                                                       0),
                                                          new NodeIdSet(),
                                                          true,
                                                          localIdentity,
                                                          communications.getLocalAddress(),
                                                          false, null,
                                                          new NodeIdSet(), 0, 0));
        return communications;
    }

    private class Receiver implements HeartbeatReceiver {

        private final CountDownLatch[] latches;

        Receiver(int members, int id) {
            super();
            latches = new CountDownLatch[members];
            for (int i = 0; i < members; i++) {
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
            System.out.println("Heartbeat received: " + hb);
            latches[hb.getSender().id].countDown();
            return false;
        }
    }
}
