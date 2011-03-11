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
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsIntf;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;

import com.hellblazer.anubis.basiccomms.nio.SocketOptions;

/**
 * The factory for creating the gossip based heartbeat communications subsystem
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class CommunicationsFactory implements HeartbeatCommsFactory {
    private final int                           gossipInterval;
    private final TimeUnit                      unit;
    private final InetSocketAddress             endpointAddress;
    private final SocketOptions                 socketOptions;
    private final ExecutorService               commsExec;
    private final ExecutorService               dispatchExec;
    private final Random                        entropy;
    private final Collection<InetSocketAddress> seedHosts;
    private final int                           quarantineDelay;
    private final int                           unreachableDelay;
    private final double                        phiConvictThreshold;
    private Identity                            localIdentity;

    public CommunicationsFactory(int gossipInterval, TimeUnit unit,
                                 InetSocketAddress endpointAddress,
                                 ExecutorService commsExec,
                                 ExecutorService dispatchExec,
                                 Collection<InetSocketAddress> seedHosts,
                                 int quarantineDelay, int unreachableDelay,
                                 double phiConvictThreshold,
                                 Identity localIdentity) {
        this(gossipInterval, unit, endpointAddress, new SocketOptions(),
             commsExec, dispatchExec, new Random(), seedHosts, quarantineDelay,
             unreachableDelay, phiConvictThreshold, localIdentity);

    }

    public CommunicationsFactory(int gossipInterval, TimeUnit unit,
                                 InetSocketAddress endpointAddress,
                                 SocketOptions socketOptions,
                                 ExecutorService commsExec,
                                 ExecutorService dispatchExec, Random entropy,
                                 Collection<InetSocketAddress> seedHosts,
                                 int quarantineDelay, int unreachableDelay,
                                 double phiConvictThreshold,
                                 Identity localIdentity) {
        this.gossipInterval = gossipInterval;
        this.unit = unit;
        this.endpointAddress = endpointAddress;
        this.socketOptions = socketOptions;
        this.commsExec = commsExec;
        this.dispatchExec = dispatchExec;
        this.entropy = entropy;
        this.seedHosts = seedHosts;
        this.quarantineDelay = quarantineDelay;
        this.unreachableDelay = unreachableDelay;
        this.phiConvictThreshold = phiConvictThreshold;
        this.localIdentity = localIdentity;
    }

    @Override
    public HeartbeatCommsIntf create(HeartbeatReceiver cs) throws IOException {
        Communications communications = new Communications(cs, gossipInterval,
                                                           unit,
                                                           endpointAddress,
                                                           socketOptions,
                                                           commsExec,
                                                           dispatchExec);
        SystemView view = new SystemView(entropy,
                                         communications.getLocalAddress(),
                                         seedHosts, quarantineDelay,
                                         unreachableDelay);
        Gossip gossip = new Gossip(communications, view, entropy,
                                   phiConvictThreshold, localIdentity);
        communications.setGossip(gossip);
        return communications;
    }

}
