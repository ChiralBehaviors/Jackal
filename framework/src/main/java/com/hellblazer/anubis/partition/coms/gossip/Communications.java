/** 
 * (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
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
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsIntf;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

import com.hellblazer.anubis.basiccomms.nio.CommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;
import com.hellblazer.anubis.basiccomms.nio.SocketOptions;

/**
 * The communciations implementation for the gossip protocol
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 */

public class Communications extends ServerChannelHandler implements
        HeartbeatCommsIntf {
    private static final Logger log = Logger.getLogger(Communications.class.getCanonicalName());

    private final Gossip gossip;
    private ScheduledFuture<?> gossipTask;
    private final int interval;
    private final TimeUnit intervalUnit;
    private final ScheduledExecutorService scheduler;

    public Communications(Gossip gossiper, int gossipInterval, TimeUnit unit,
                          InetSocketAddress endpointAddress,
                          SocketOptions socketOptions,
                          ExecutorService commsExec,
                          ExecutorService dispatchExec) {
        super(endpointAddress, socketOptions, commsExec, dispatchExec);
        interval = gossipInterval;
        intervalUnit = unit;
        gossip = gossiper;
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "Anubis: Gossip heartbeat servicing thread");
                daemon.setDaemon(true);
                return daemon;
            }
        });
    }

    @Override
    public String getThreadStatusString() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isIgnoring(Identity id) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void sendHeartbeat(HeartbeatMsg msg) {
        gossip.updateLocalState(new HeartbeatState(msg));
    }

    @Override
    public void setIgnoring(View ignoringUpdate) {
        // TODO Auto-generated method stub

    }

    /**
     * Start the gossiper
     */
    @Override
    public void start() {
        if (gossipTask != null) {
            return;
        }
        gossipTask = scheduler.scheduleWithFixedDelay(gossipTask(), interval,
                                                      interval, intervalUnit);
    }

    @Override
    public void terminate() {
        if (gossipTask != null) {
            scheduler.shutdownNow();
            gossipTask.cancel(true);
            gossipTask = null;
        }
    }

    private Runnable gossipTask() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    gossip.gossip();
                } catch (Throwable e) {
                    log.log(Level.WARNING, "Exception while performing gossip",
                            e);
                }
            }
        };
    }

    protected GossipHandler connect(InetSocketAddress address,
                                    Runnable connectAction) throws IOException {
        SocketChannel channel = SocketChannel.open(address);
        GossipHandler handler = new GossipHandler(gossip, this, channel);
        selectForConnect(handler, connectAction);
        return handler;
    }

    @Override
    protected CommunicationsHandler createHandler(SocketChannel accepted) {
        return new GossipHandler(gossip, this, accepted);
    }
}