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

import static com.hellblazer.anubis.rst.udp.MsgTypes.ADD_CHILD;
import static com.hellblazer.anubis.rst.udp.MsgTypes.REMOVE_CHILD;
import static com.hellblazer.anubis.rst.udp.MsgTypes.SET_COLOR;
import static com.hellblazer.anubis.rst.udp.MsgTypes.SET_ROOT;
import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
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
    private static final Logger log = Logger.getLogger(UdpService.class.getCanonicalName());
    /**
     * MAX_SEG_SIZE is a default maximum packet size. This may be small, but any
     * network will be capable of handling this size so its transfer semantics
     * are atomic (no fragmentation in the network).
     */
    private static final int MAX_SEG_SIZE = 1500; // Ethernet standard MTU
    private static final int MAGIC_NUMBER = 24051967;

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
    private final MemberChannel[] members;

    public UdpService(int id, InetSocketAddress myAddress, InetSocketAddress[] M)
                                                                                 throws SocketException {
        super();
        address = myAddress;
        DatagramSocket socket = new DatagramSocket();
        socket.setReceiveBufferSize(MAX_SEG_SIZE);
        socket.setReuseAddress(true);
        socket.setSendBufferSize(MAX_SEG_SIZE);
        socketChannel = socket.getChannel();
        members = getMembership(M);
        Channel[] channels = Arrays.copyOf(members,
                                           Math.max(members.length, id));
        LocalChannel myChannel = new LocalChannel(id, this, address);
        channels[id] = myChannel;
        self = new Node(myChannel, channels);
    }

    /**
     * Start the service
     */
    public void start() throws IOException {
        if (running.compareAndSet(false, true)) {
            try {
                socketChannel.connect(address);
            } catch (IOException e) {
                log.log(Level.SEVERE,
                        "Cannot connect to socket, shutting down", e);
                running.set(false);
                throw e;
            }
            serviceEvaluator.execute(service());
        }
    }

    /**
     * Stop the service
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                log.log(Level.FINEST, "Exception closing the socket", e);
            }
        }
    }

    MemberChannel[] getMembership(InetSocketAddress[] members) {
        MemberChannel[] channels = new MemberChannel[members.length];
        int index = 0;
        for (InetSocketAddress member : members) {
            channels[index] = new MemberChannel(index, this, member);
        }
        return channels;
    }

    void sendAddChild(int id, MemberChannel parent) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[4]);
        buffer.putInt(MAGIC_NUMBER);
        buffer.put(ADD_CHILD);
        buffer.putInt(id);
        buffer.flip();
        try {
            socketChannel.send(buffer, parent.getAddress());
        } catch (IOException e) {
            parent.markRed();
        }
    }

    void sendRemoveChild(int id, MemberChannel parent) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[4]);
        buffer.putInt(MAGIC_NUMBER);
        buffer.put(REMOVE_CHILD);
        buffer.putInt(id);
        buffer.flip();
        try {
            socketChannel.send(buffer, parent.getAddress());
        } catch (IOException e) {
            if (log.isLoggable(Level.INFO)) {
                log.info(format("Error removing self as a child from: %s",
                                parent));
            }
            parent.markRed();
            self.evaluateProtocol();
        }
    }

    void updateRootValue(int id, int root) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[4]);
        buffer.putInt(MAGIC_NUMBER);
        buffer.put(SET_ROOT);
        buffer.putInt(root);
        buffer.flip();
        boolean failures = false;
        for (MemberChannel member : members) {
            try {
                socketChannel.send(buffer, member.getAddress());
            } catch (IOException e) {
                if (log.isLoggable(Level.WARNING)) {
                    log.warning(format("Error updating root value for: %s",
                                       member.getAddress()));
                }
                member.markRed();
                failures = true;
            }
        }
        if (failures) {
            self.evaluateProtocol();
        }
    }

    void updateToGreen(int id) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[4]);
        buffer.putInt(MAGIC_NUMBER);
        buffer.put(SET_COLOR);
        buffer.putInt(1);
        buffer.flip();
        boolean failures = false;
        for (MemberChannel member : members) {
            try {
                socketChannel.send(buffer, member.getAddress());
            } catch (IOException e) {
                if (log.isLoggable(Level.WARNING)) {
                    log.warning(format("Error updating color value for: %s",
                                       member.getAddress()));
                }
                member.markRed();
                failures = true;
            }
        }
        if (failures) {
            self.evaluateProtocol();
        }
    }

    private String prettyPrint(SocketAddress sender, byte[] bytes) {
        final StringBuilder sb = new StringBuilder(bytes.length * 2);
        sb.append(new SimpleDateFormat().format(new Date()));
        sb.append(" - ");
        sb.append(sender);
        sb.append(" - ");
        sb.append('\n');
        sb.append(toHex(bytes, 0, bytes.length));
        return sb.toString();
    }

    /**
     * Process the inbound message
     * 
     * @param buffer
     *            - the message bytes
     */
    private void processInbound(ByteBuffer buffer) {
        byte msgType = buffer.get();
        int nodeId = buffer.getInt();
        if (nodeId >= members.length || nodeId < 0) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning(format("Unknown member: %s", nodeId));
            }
            return;
        }
        MemberChannel sender = members[nodeId];
        if (sender == null) {
            if (log.isLoggable(Level.WARNING)) {
                log.warning(format("No corresponding channelfor member: %s",
                                   nodeId));
                return;
            }
        }
        switch (msgType) {
            case ADD_CHILD:
                self.addChild(sender);
                break;
            case REMOVE_CHILD:
                self.removeChild(sender);
                break;
            case SET_ROOT:
                sender.setRoot(buffer.getInt());
                break;
            case SET_COLOR:
                sender.setColor(buffer.get());
                break;
            default:
                if (log.isLoggable(Level.WARNING)) {
                    log.warning(format("Invalid message type: %s", msgType));
                }
                return;
        }
        self.evaluateProtocol();
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
                byte[] inBytes = new byte[MAX_SEG_SIZE];
                ByteBuffer buffer = ByteBuffer.wrap(inBytes);
                buffer.order(ByteOrder.BIG_ENDIAN);
                while (running.get()) {
                    try {
                        Arrays.fill(inBytes, (byte) 0);
                        buffer.clear();
                        service(buffer);
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

    /**
     * Service the next inbound datagram
     * 
     * @param buffer
     *            - the buffer to use to receive the datagram
     * @throws IOException
     */
    private void service(ByteBuffer buffer) throws IOException {
        SocketAddress sender = socketChannel.receive(buffer);
        buffer.flip();
        if (log.isLoggable(Level.FINEST)) {
            log.finest(prettyPrint(sender, buffer.array()));
        } else if (log.isLoggable(Level.FINE)) {
            log.fine("Received packet from: " + sender);
        }
        int magic = buffer.getInt();
        if (MAGIC_NUMBER == magic) {
            try {
                processInbound(buffer);
            } catch (BufferOverflowException e) {
                if (log.isLoggable(Level.WARNING)) {
                    log.warning(format("Invalid message: %s", buffer.array()));
                }
            }
        } else {
            if (log.isLoggable(Level.FINEST)) {
                log.finest(format("Msg with invalid MAGIC header [%s] discarded",
                                  magic));
            }
        }
    }
}
