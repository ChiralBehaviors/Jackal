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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;

/**
 * MulticastComms is an abstract class representing an end point
 * for multicast communications.
 *
 * The endpoint is capable of
 * sending and receiving objects, transfered in packets as object streams.
 * Note that there is a limit to the size of an object when transfered as
 * an object stream.
 *
 * The class is a thread and uses a blocking receive loop. Delivery of a
 * received object will be in this thread.
 *
 * This class is extended to define how to deliver an object deliver method.
 */
public class MulticastComms extends Thread {

    /**
     * MAX_SEG_SIZE is a default maximum packet size. This may be small,
     * but any network will be capable of handling this size so its transfer
     * semantics are atomic (no fragmentation in the network).
     */
    static public final int MAX_SEG_SIZE = 1500; // Eathernet standard MTU

    private MulticastAddress groupAddress;
    private byte[] inBytes;
    private DatagramPacket inPacket;
    private MulticastSocket sock;
    protected Logger log = Logger.getLogger(this.getClass().getCanonicalName());
    volatile private boolean terminating;
    Object outObject;

    /**
     * Constructor - uses MulticastAddress to define the multicast
     * group etc.
     */
    public MulticastComms(String threadName, MulticastAddress address) throws IOException {

        super(threadName);
        groupAddress = address;
        sock = new MulticastSocket(address.port);
        sock.joinGroup(address.ipaddress);
        sock.setTimeToLive(address.timeToLive);
        terminating = false;
    }

    /**
     * Constructor - uses MulticastAddress to define the multicast. Use inf to define
     * the network interface to use.
     * group etc.
     */
    public MulticastComms(String threadName, MulticastAddress address,
                          ConnectionAddress inf) throws IOException {

        super(threadName);
        groupAddress = address;
        sock = new MulticastSocket(address.port);
        sock.setInterface(inf.ipaddress);
        sock.joinGroup(address.ipaddress);
        sock.setTimeToLive(address.timeToLive);
        terminating = false;
    }

    /**
     * Get the status of this thread.
     * @return a string representing the status of this thread
     */
    public String getThreadStatusString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.getName()).append(" ............................ ").setLength(
                                                                                          30);
        buffer.append(super.isAlive() ? ".. is Alive " : ".. is Dead ");
        buffer.append(!terminating ? ".. running ....." : ".. terminated ..");
        buffer.append(" address = ").append(groupAddress.ipaddress.toString()).append(
                                                                                      ":").append(
                                                                                                  groupAddress.port);
        return buffer.toString();
    }

    /**
     * the thread performs a blocking receive loop.
     */
    @Override
    public void run() { 
		if (log.isLoggable(Level.FINER)) {
			log.finer("Starting receive processing on: " + groupAddress);
		}
        while (!terminating) {

            try {

                inBytes = new byte[MAX_SEG_SIZE];
                inPacket = new DatagramPacket(inBytes, inBytes.length);
                sock.receive(inPacket);
        		if (log.isLoggable(Level.FINEST)) {
        			log.finest("Received packet from: " + inPacket.getSocketAddress());
        		}
                deliverBytes(inPacket.getData());

            } catch (Exception e) {

                if (!terminating && log.isLoggable(Level.WARNING)) {
                	log.log(Level.WARNING, "Exception processing inbound message", e);
                }

            }
        }
    }

    /**
     * Send an array of bytes.
     * The send function is synchronized to prevent multiple sends concurrent
     * sends (is this necessary??) Can not deadlock - multiple threads will take
     * turns to send, the receive loop (run) will block sends unless it is nested
     * in the deliverObject method, which is ok.
     * @param bytes bytes to send
     */
    public synchronized void sendObject(byte[] bytes) {
        try {
            sock.send(bytesToPacket(bytes, groupAddress.ipaddress,
                                    groupAddress.port));
        } catch (IOException ioe) {
            if (!terminating && log.isLoggable(Level.WARNING)) {
            	log.log(Level.WARNING, "", ioe);
            }
        }
    }

    public void shutdown() {
        terminating = true;
        sock.close();
    }

    @Override
    public void start() {
        super.start();
    }

    private DatagramPacket bytesToPacket(byte[] bytes, InetAddress address,
                                         int port) {
        return new DatagramPacket(bytes, bytes.length, address, port);
    }

    /**
     * convert the input buffer to a string - for debug purposes.
     * @return a stringified inBytes
     */
    @SuppressWarnings("unused")
    private String inputToString() {
        return new String(inBytes);
    }

    /**
     * deliverObject is the method for delivering received
     * objects. Typically this method will cast the object and pass
     * it to an appropriate handler. This may include handing off the
     * delivery to another thread.
     * @param bytes 
     */
    protected void deliverBytes(byte[] bytes) {
        // does nothing by default
    }

}
