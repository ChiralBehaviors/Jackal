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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsIntf;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

import com.hellblazer.anubis.basiccomms.nio.CommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;
import com.hellblazer.anubis.basiccomms.nio.SocketOptions;

/**
 * The communciations implementation for the gossip protocol
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 */

public class Communications extends ServerChannelHandler implements
        GossipCommunications, HeartbeatCommsIntf {
    private static final Logger log = Logger.getLogger(Communications.class.getCanonicalName());

    public static InetSocketAddress readInetAddress(ByteBuffer msg)
                                                                   throws UnknownHostException {
        int length = msg.get();
        if (length == 0) {
            return null;
        }

        byte[] address = new byte[length];
        msg.get(address);
        int port = msg.getInt();

        InetAddress inetAddress = InetAddress.getByAddress(address);
        return new InetSocketAddress(inetAddress, port);
    }

    public static void writeInetAddress(InetSocketAddress ipaddress,
                                        ByteBuffer bytes) {
        if (ipaddress == null) {
            bytes.put((byte) 0);
            return;
        }
        byte[] address = ipaddress.getAddress().getAddress();
        bytes.put((byte) address.length);
        bytes.put(address);
        bytes.putInt(ipaddress.getPort());
    }

    private Gossip                         gossip;
    private ScheduledFuture<?>             gossipTask;
    private final int                      interval;
    private final TimeUnit                 intervalUnit;
    private final ScheduledExecutorService scheduler;
    private final HeartbeatReceiver        receiver;
    private final AtomicReference<View>    ignoring = new AtomicReference<View>();

    public Communications(HeartbeatReceiver heartbeatReceiver,
                          int gossipInterval, TimeUnit unit,
                          InetSocketAddress endpointAddress,
                          SocketOptions socketOptions,
                          ExecutorService commsExec,
                          ExecutorService dispatchExec) throws IOException {
        super(endpointAddress, socketOptions, commsExec, dispatchExec);
        interval = gossipInterval;
        intervalUnit = unit;
        receiver = heartbeatReceiver;
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
    public GossipHandler connect(InetSocketAddress address, Endpoint endpoint,
                                 Runnable connectAction) throws IOException {
        SocketChannel channel = SocketChannel.open(address);
        channel.configureBlocking(false);
        GossipHandler handler = new GossipHandler(gossip, this, channel);
        addHandler(handler);
        endpoint.setCommunications(handler);
        if (channel.finishConnect()) {
            dispatch(connectAction);
        } else {
            selectForConnect(handler, connectAction);
        }
        return handler;
    }

    @Override
    public String getThreadStatusString() {
        return "Anubis: Gossip heartbeat/discovery, running: " + isRunning();
    }

    @Override
    public boolean isIgnoring(Identity id) {
        final View theShunned = ignoring.get();
        if (theShunned == null) {
            return false;
        }
        return theShunned.contains(id);
    }

    @Override
    public void notifyUpdate(final HeartbeatState state) {
        if (state == null || isIgnoring(state.getSender())) {
            return;
        }
        dispatch(new Runnable() {
            @Override
            public void run() {
                receiver.receiveHeartbeat(state);
            }
        });
    }

    @Override
    public void sendHeartbeat(Heartbeat heartbeat) {
        gossip.updateLocalState(HeartbeatState.toHeartbeatState(heartbeat));
    }

    public void setGossip(Gossip gossip) {
        this.gossip = gossip;
    }

    @Override
    public void setIgnoring(View ignoringUpdate) {
        ignoring.set(ignoringUpdate);
    }

    @Override
    protected CommunicationsHandler createHandler(SocketChannel accepted) {
        return new GossipHandler(gossip, this, accepted);
    }

    protected Runnable gossipTask() {
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

    /**
     * Start the gossiper
     */
    @Override
    protected void startService() {
        super.startService();
        gossipTask = scheduler.scheduleWithFixedDelay(gossipTask(), interval,
                                                      interval, intervalUnit);
    }

    @Override
    protected void terminateService() {
        super.terminateService();
        scheduler.shutdownNow();
        gossipTask.cancel(true);
        gossipTask = null;
    }
}