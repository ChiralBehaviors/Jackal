/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

 */
package org.smartfrog.services.anubis.basiccomms.multicasttransport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hellblazer.jackal.util.ByteBufferPool;

/**
 * MulticastComms is an abstract class representing an end point for multicast
 * communications.
 * 
 * The endpoint is capable of sending and receiving objects, transfered in
 * packets as object streams. Note that there is a limit to the size of an
 * object when transfered as an object stream.
 * 
 * The class is a thread and uses a blocking receive loop. Delivery of a
 * received object will be in this thread.
 * 
 * This class is extended to define how to deliver an object deliver method.
 */
public class MulticastComms extends Thread {

    /**
     * MAX_SEG_SIZE is a default maximum packet size. This may be small, but any
     * network will be capable of handling this size so its transfer semantics
     * are atomic (no fragmentation in the network).
     */
    static public final int        MAX_SEG_SIZE = 1500;
    private static final Logger    log          = LoggerFactory.getLogger(MulticastComms.class.getCanonicalName());

    private MulticastAddress       groupAddress;
    private MulticastSocket        sock;
    private final AtomicBoolean    terminating  = new AtomicBoolean();
    protected final ByteBufferPool bufferPool   = new ByteBufferPool(
                                                                     "Multicast Comms",
                                                                     100);

    /**
     * Constructor - uses MulticastAddress to define the multicast group etc.
     */
    public MulticastComms(String threadName, MulticastAddress address)
                                                                      throws IOException {
        super(threadName);
        groupAddress = address;
        sock = new MulticastSocket(address.port);
        sock.joinGroup(address.ipaddress);
        sock.setTimeToLive(address.timeToLive);
        setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                log.warn("Uncaught exception", e);
            }
        });
    }

    /**
     * Constructor - uses MulticastAddress to define the multicast. Use inf to
     * define the network interface to use. group etc.
     */
    public MulticastComms(String threadName, MulticastAddress address,
                          InetAddress inf) throws IOException {

        super(threadName);
        groupAddress = address;
        sock = new MulticastSocket(address.port);
        sock.setInterface(inf);
        sock.joinGroup(address.ipaddress);
        sock.setTimeToLive(address.timeToLive);
    }

    /**
     * Get the status of this thread.
     * 
     * @return a string representing the status of this thread
     */
    public String getStatusString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.getName()).append(" ............................ ").setLength(30);
        buffer.append(super.isAlive() ? ".. is Alive " : ".. is Dead ");
        buffer.append(!terminating.get() ? ".. running ....."
                                        : ".. terminated ..");
        buffer.append(" address = ").append(groupAddress.ipaddress.toString()).append(":").append(groupAddress.port);
        return buffer.toString();
    }

    /**
     * the thread performs a blocking receive loop.
     */
    @Override
    public void run() {
        if (log.isTraceEnabled()) {
            log.trace("Starting receive processing on: " + groupAddress);
        }
        ByteBuffer packetBytes = ByteBuffer.allocate(MAX_SEG_SIZE);
        while (!terminating.get()) {
            try {
                byte[] inBytes = packetBytes.array();
                DatagramPacket inPacket = new DatagramPacket(inBytes,
                                                             inBytes.length);
                sock.receive(inPacket);
                if (log.isTraceEnabled()) {
                    log.trace("Received packet from: "
                              + inPacket.getSocketAddress());
                }
                deliverBytes(packetBytes);
                packetBytes.clear();
            } catch (Throwable e) {

                if (!terminating.get() && log.isWarnEnabled()) {
                    log.warn("Exception processing inbound message", e);
                }

            }
        }
    }

    /**
     * Send a ByteBuffer. The send function is synchronized to prevent multiple
     * sends concurrent sends (is this necessary??) Can not deadlock - multiple
     * threads will take turns to send, the receive loop (run) will block sends
     * unless it is nested in the deliverObject method, which is ok.
     * 
     * @param bytes
     *            ByteBuffer to send
     */
    public synchronized void sendObject(ByteBuffer bytes) {
        try {
            sock.send(bytesToPacket(bytes, groupAddress.ipaddress,
                                    groupAddress.port));
            // BUFFER_CACHE.get().recycle(bytes);
        } catch (IOException ioe) {
            if (!terminating.get() && log.isWarnEnabled()) {
                log.warn("", ioe);
            }
        } finally {
            bufferPool.free(bytes);
        }
    }

    public void shutdown() {
        terminating.set(true);
        sock.close();
        log.info(bufferPool.toString());
    }

    private DatagramPacket bytesToPacket(ByteBuffer msg, InetAddress address,
                                         int port) {
        byte[] bytes = msg.array();
        return new DatagramPacket(bytes, msg.capacity(), address, port);
    }

    /**
     * deliverObject is the method for delivering received objects. Typically
     * this method will cast the object and pass it to an appropriate handler.
     * This may include handing off the delivery to another thread.
     */
    protected void deliverBytes(ByteBuffer bytes) {
        // does nothing by default
    }

}
