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
package com.hellblazer.anubis.partition.coms.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsIntf;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurityException;

/** 
 * A Unicast UDP heartbeat implementation.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Heartbeat implements HeartbeatCommsIntf {
    private static final Logger log = Logger.getLogger(Heartbeat.class.getCanonicalName());
    /**
     * MAX_SEG_SIZE is a default maximum packet size. This may be small, but any
     * network will be capable of handling this size so its transfer semantics
     * are atomic (no fragmentation in the network).
     */
    public static final int MAX_SEG_SIZE = 1500; // Ethernet standard MTU

    private volatile boolean terminating;
    private final HeartbeatReceiver connectionSet;
    private View ignoring;
    private final Object ingnoringMonitor = new Object();
    private final Identity me;
    private final WireSecurity wireSecurity;
    private DatagramSocket socket;
    private final List<InetSocketAddress> membership = new CopyOnWriteArrayList<InetSocketAddress>();
    private Thread heartbeatThread;
    private final ExecutorService deliveryService;
    private final InetSocketAddress address;

    public Heartbeat(HeartbeatReceiver cs, Identity identity, WireSecurity ws,
                     ExecutorService ds, InetSocketAddress sa)
                                                              throws SocketException {
        connectionSet = cs;
        me = identity;
        wireSecurity = ws;
        deliveryService = ds;
        address = sa;
        socket = new DatagramSocket(sa);
        socket.setReceiveBufferSize(MAX_SEG_SIZE);
        socket.setReuseAddress(true);
        socket.setSendBufferSize(MAX_SEG_SIZE);
    }

    public void addMember(InetSocketAddress socketAddress) {
        membership.add(socketAddress);
    }

    @Override
    public String getThreadStatusString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(heartbeatThread.getName()).append(" ............................ ").setLength(30);
        buffer.append(heartbeatThread.isAlive() ? ".. is Alive "
                                               : ".. is Dead ");
        buffer.append(!terminating ? ".. running ....." : ".. terminated ..");
        buffer.append(" address = ").append(address);
        return buffer.toString();
    }

    @Override
    public boolean isIgnoring(Identity id) {
        synchronized (ingnoringMonitor) {
            return ignoring != null && ignoring.contains(id);
        }
    }

    public void removeMember(InetSocketAddress socketAddress) {
        membership.remove(socketAddress);
    }

    @Override
    public void sendHeartbeat(HeartbeatMsg msg) {
        byte[] bytes;
        try {
            bytes = wireSecurity.toWireForm(msg);
        } catch (WireFormException e) {
            log.log(Level.SEVERE,
                    String.format("Unable to create wire form of %s", msg), e);
            return;
        }
        DatagramPacket heartbeat = new DatagramPacket(bytes, bytes.length);
        for (InetSocketAddress address : membership) {
            heartbeat.setSocketAddress(address);
            try {
                socket.send(heartbeat);
            } catch (IOException e) {
                log.log(Level.SEVERE,
                        String.format("Unable to send heartbeat to %s", address),
                        e);
                return;
            }
        }
    }

    /**
     * determine which nodes to ignore if any. This will be called from the
     * connection set - ignoring is used in a critical section in
     * deliverObject() that uses connectionSet as its monitor.
     * 
     * @param ignoringUpdate
     *            a view
     */
    @Override
    public void setIgnoring(View ignoringUpdate) {
        synchronized (ingnoringMonitor) {
            ignoring = ignoringUpdate.isEmpty() ? null : ignoringUpdate;
        }
    }

    @Override
    public void start() {
        terminating = false;
        heartbeatThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!terminating) {
                    try {
                        byte[] inBytes = new byte[MAX_SEG_SIZE];
                        final DatagramPacket inPacket = new DatagramPacket(
                                                                           inBytes,
                                                                           inBytes.length);
                        socket.receive(inPacket);
                        if (log.isLoggable(Level.FINEST)) {
                            log.finest("Received packet from: "
                                       + inPacket.getSocketAddress());
                        }
                        deliveryService.execute(new Runnable() {
                            @Override
                            public void run() {
                                deliver(inPacket.getData());
                            }
                        });
                    } catch (Throwable e) {
                        if (!terminating && log.isLoggable(Level.WARNING)) {
                            log.log(Level.WARNING,
                                    "Exception processing inbound message", e);
                        }

                    }
                }
            }
        }, "Anubis: UDP Heartbeat communications");
        heartbeatThread.start();
    }

    @Override
    public void terminate() {
        terminating = true;
        if (heartbeatThread != null) {
            heartbeatThread.interrupt();
        }
        deliveryService.shutdownNow();
    }

    protected void deliver(byte[] data) {
        HeartbeatMsg hb = unwrap(data);
        if (hb == null) {
            return;
        }
        /**
         * if not right magic discard it
         */
        if (!hb.getSender().equalMagic(me)) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("heartbeat discarded due to invalid magic number: "
                           + hb);
            }
            return;
        }

        /**
         * for testing purposes - can ignore messages from specified senders
         */
        if (isIgnoring(hb.getSender())) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("Ignoring heart beat sender: " + hb);
            }
            return;
        }

        /**
         * Deliver new heartbeat message - this needs to be synchronized on the
         * connection set so that nothing changes in the process.
         * 
         * The connection returned by getConnection may or may not be active
         * (i.e. it may be quiescing) if it is not active it is up to the
         * connection itself to deal with the heartbeat.
         */
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Delivering heart beat: " + hb);
        }
        connectionSet.receiveHeartbeat(hb);
    }

    protected HeartbeatMsg unwrap(byte[] data) {
        Object obj = null;
        try {
            obj = wireSecurity.fromWireForm(data);
        } catch (WireSecurityException ex) {
            log.severe(me
                       + "multicast transport encountered security violation receiving message - ignoring the message ");
            return null;
        } catch (Exception ex) {
            log.log(Level.SEVERE,
                    me + " Error reading wire form message - ignoring", ex);
            return null;
        }
        if (!(obj instanceof HeartbeatMsg)) {
            if (log.isLoggable(Level.FINE)) {
                log.fine(String.format("%s Ignoring non-heartbeat message: %s",
                                       me, obj));
            }
        }
        HeartbeatMsg hb = (HeartbeatMsg) obj;
        return hb;
    }
}
