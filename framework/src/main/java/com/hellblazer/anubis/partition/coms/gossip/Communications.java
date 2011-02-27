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

import static java.lang.String.format;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hellblazer.anubis.util.Pair;

/**
 * A heartbeat protocol based on gossip, using a failure detector to determine
 * the liveness of an endpoint.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 */

public class Communications implements GossipCommunications {
    private static final byte ACK = 1;
    private static final byte ACK2 = 2;
    private static final Logger log = Logger.getLogger(Communications.class.getCanonicalName());
    private static final int MTU = 1500;
    private static final byte SYN = 0;

    private final Gossip gossip;
    private ScheduledFuture<?> gossipTask;
    private final ExecutorService inboundService;
    private final int interval;
    private final TimeUnit intervalUnit;
    private final int magic;
    private Future<?> receiveTask;
    private final AtomicBoolean running = new AtomicBoolean();
    private final ScheduledExecutorService scheduler;
    private final DatagramSocket socket;

    public Communications(SocketAddress listeningAddress, int magicCookie,
                          Gossip gossiper, int gossipInterval, TimeUnit unit)
                                                                             throws SocketException {
        interval = gossipInterval;
        intervalUnit = unit;
        magic = magicCookie;
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
        inboundService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "Anubis: Gossip heartbeat receive thread");
                daemon.setDaemon(true);
                return daemon;
            }
        });
        socket = new DatagramSocket(listeningAddress);
        socket.setReceiveBufferSize(MTU);
        socket.setReuseAddress(true);
        socket.setSendBufferSize(MTU);
    }

    /* (non-Javadoc)
     * @see com.hellblazer.anubis.partition.coms.gossip.GossipCommunications#send(java.util.List, java.net.InetSocketAddress)
     */
    @Override
    public void send(List<Digest> digests, InetSocketAddress member) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.hellblazer.anubis.partition.coms.gossip.GossipCommunications#send(java.util.Map, java.net.InetSocketAddress)
     */
    @Override
    public void send(Map<InetSocketAddress, Endpoint> deltaState,
                     InetSocketAddress to) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.hellblazer.anubis.partition.coms.gossip.GossipCommunications#send(com.hellblazer.anubis.util.Pair, java.net.InetSocketAddress)
     */
    @Override
    public void send(Pair<List<Digest>, Map<InetSocketAddress, Endpoint>> ack,
                     InetSocketAddress from) {
        // TODO Auto-generated method stub

    }

    /**
     * Start the gossiper
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        gossipTask = scheduler.scheduleWithFixedDelay(getGossipTask(),
                                                      interval, interval,
                                                      intervalUnit);
        receiveTask = inboundService.submit(getReceiveTask());
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            scheduler.shutdownNow();
            inboundService.shutdownNow();

            if (gossipTask != null) {
                gossipTask.cancel(true);
                gossipTask = null;
            }
            if (receiveTask != null) {
                receiveTask.cancel(true);
                receiveTask = null;
            }
        }
    }

    private Runnable getGossipTask() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    gossip.gossip(Communications.this);
                } catch (Throwable e) {
                    log.log(Level.WARNING, "Exception while performing gossip",
                            e);
                }
            }
        };
    }

    private Runnable getReceiveTask() {
        return new Runnable() {
            @Override
            public void run() {
                byte[] bytes = new byte[MTU];
                DatagramPacket packet = new DatagramPacket(bytes, MTU);
                while (running.get()) {
                    try {
                        Arrays.fill(bytes, (byte) 0);
                        receive(packet);
                    } catch (Throwable e) {
                        log.log(Level.WARNING,
                                "Exception while performing gossip", e);
                    }
                }
            }
        };
    }

    private void receive(DatagramPacket packet) {
        ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
        InetSocketAddress from = (InetSocketAddress) packet.getSocketAddress();
        int magicCookie = buffer.getInt();
        if (magic != magicCookie) {
            log.warning(format("Magic number mismatch from %s; received: %s != %s",
                               from, magicCookie, magic));
            return;
        }
        byte msgType = buffer.get();
        switch (msgType) {
            case SYN: {
                Digest[] digests = null;
                Pair<List<Digest>, Map<InetSocketAddress, Endpoint>> ack = gossip.synchronize(digests,
                                                                                              from);
                if (ack != null) {
                    send(ack, from);
                }
                break;
            }
            case ACK: {
                List<Digest> digests = null;
                Map<InetSocketAddress, Endpoint> remoteStates = null;
                Map<InetSocketAddress, Endpoint> deltaState = gossip.ack2(digests,
                                                                          remoteStates,
                                                                          from);
                if (deltaState != null) {
                    send(deltaState, from);
                }
                break;
            }
            case ACK2: {
                Map<InetSocketAddress, Endpoint> remoteStates = null;
                gossip.ack(remoteStates, from);
                break;
            }
            default: {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest(format("invalid message type: %s from: %s",
                                      msgType, from));
                }
            }
        }
    }
}