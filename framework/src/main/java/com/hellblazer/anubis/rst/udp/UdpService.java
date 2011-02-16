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
package com.hellblazer.anubis.rst.udp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hellblazer.anubis.rst.Channel;
import com.hellblazer.anubis.rst.Node;
import com.hellblazer.anubis.util.HexDump;

/**
 * A UDP message protocol implementation for maintaining a Rooted Static Tree
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class UdpService {
    private static final int SELECT_TIMEOUT = 1000;
    private static final Logger log = Logger.getLogger(UdpService.class.getCanonicalName());
    /**
     * MAX_SEG_SIZE is a default maximum packet size. This may be small, but any
     * network will be capable of handling this size so its transfer semantics
     * are atomic (no fragmentation in the network).
     */
    public static final int MAX_SEG_SIZE = 1500; // Ethernet standard MTU

    private static String toHex(byte[] data, int offset, int length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        PrintStream stream = new PrintStream(baos);
        HexDump.hexdump(stream, data, offset, length);
        stream.close();
        return baos.toString();
    }

    private final DatagramChannel socketChannel;
    private final InetSocketAddress address;
    private final AtomicBoolean running = new AtomicBoolean();
    private final Executor serviceEvaluator = Executors.newSingleThreadExecutor();
    private final Node self;

    private final Map<Integer, MemberChannel> members;

    public UdpService(int id, InetSocketAddress myAddress,
                      Map<Integer, InetSocketAddress> M) throws SocketException {
        super();
        address = myAddress;
        DatagramSocket socket = new DatagramSocket();
        socket.setReceiveBufferSize(MAX_SEG_SIZE);
        socket.setReuseAddress(true);
        socket.setSendBufferSize(MAX_SEG_SIZE);
        socketChannel = socket.getChannel();
        members = getMembership(M);
        HashMap<Integer, Channel> channels = new HashMap<Integer, Channel>();
        channels.putAll(members);
        LocalChannel myChannel = new LocalChannel(id, this, address);
        channels.put(id, myChannel);
        self = new Node(myChannel, channels);
    }

    /**
     * Start the service
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            serviceEvaluator.execute(service());
        }
    }

    /**
     * Stop the service
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {

        }
    }

    Map<Integer, MemberChannel> getMembership(Map<Integer, InetSocketAddress> members) {
        HashMap<Integer, MemberChannel> channels = new HashMap<Integer, MemberChannel>();
        for (Entry<Integer, InetSocketAddress> member : members.entrySet()) {
            channels.put(member.getKey(),
                         new MemberChannel(member.getKey(), this,
                                           member.getValue()));
        }
        return channels;
    }

    /**
     * Send the datagram across the net
     * 
     * @param packet
     * @throws IOException
     */
    void send(DatagramPacket packet) throws IOException {
        // socket.send(packet);
    }

    /**
     * The service loop.
     * 
     * @return the Runnable action implementing the service loop.
     */
    private Runnable service() {
        return new Runnable() {
            @Override
            public void run() {
                Selector selector;
                try {
                    socketChannel.connect(address);
                    selector = Selector.open();
                } catch (Throwable e) {
                    log.log(Level.SEVERE, "Cannot connect", e);
                    running.set(false);
                    return;
                }
                while (running.get()) {
                    try {
                        selector.select(SELECT_TIMEOUT);
                        /*
                        Arrays.fill(inBytes, (byte) 0);
                        if (log.isLoggable(Level.FINEST)) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append(new SimpleDateFormat().format(new Date()));
                            sb.append(" - ");
                            sb.append(packet.getAddress().getHostAddress());
                            sb.append(" - ");
                            sb.append('\n');
                            sb.append(toHex(packet.getData(),
                                            packet.getOffset(),
                                            packet.getLength()));
                            log.finest(sb.toString());
                        } else if (log.isLoggable(Level.FINE)) {
                            log.fine("Received packet from: "
                                     + packet.getSocketAddress());
                        }
                        serviceInbound(ByteBuffer.wrap(packet.getData()));
                        */
                    } catch (Throwable e) {
                        if (log.isLoggable(Level.WARNING)) {
                            log.log(Level.WARNING,
                                    "Exception processing inbound message", e);
                        }

                    }
                }
            }
        };
    }

    private void serviceInbound(ByteBuffer buffer) {
        self.evaluateProtocol();
    }
}
