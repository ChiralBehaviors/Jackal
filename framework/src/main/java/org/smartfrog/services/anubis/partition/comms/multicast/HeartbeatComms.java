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
package org.smartfrog.services.anubis.partition.comms.multicast;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastComms;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatReceiver;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurityException;

public class HeartbeatComms extends MulticastComms implements
        HeartbeatCommsIntf {
    private static final Logger         log      = LoggerFactory.getLogger(HeartbeatComms.class.getCanonicalName());
    private final HeartbeatReceiver     connectionSet;
    /**
     * for testing purposes
     */
    private final AtomicReference<View> ignoring = new AtomicReference<View>();
    private final Identity              me;
    private final WireSecurity          wireSecurity;

    /**
     * Set up multicast comms - uses default network interface
     * 
     * @param address
     *            - multicast address
     * @param cs
     *            - connection set
     * @param threadName
     *            - name for multicast server thread
     * @param id
     *            - local identify
     * @param sec
     *            - security module
     * @throws Exception
     */
    public HeartbeatComms(MulticastAddress address, HeartbeatReceiver cs,
                          String threadName, Identity id, WireSecurity sec)
                                                                           throws IOException {
        super(threadName, address);
        me = id;
        connectionSet = cs;
        wireSecurity = sec;
        // setPriority(Thread.MAX_PRIORITY);
    }

    /**
     * Set up multicast coms specifying the network interface
     * 
     * @param address
     *            - multicast address
     * @param inf
     *            - network interface to use (specified as a connection address)
     * @param cs
     *            - connection set
     * @param threadName
     *            - name for multicast server thread
     * @param id
     *            - local identify
     * @param sec
     *            - security module
     * @throws IOException
     * @throws Exception
     */
    public HeartbeatComms(MulticastAddress address, InetAddress inf,
                          HeartbeatReceiver cs, String threadName, Identity id,
                          WireSecurity sec) throws IOException {
        super(threadName, address, inf);
        me = id;
        connectionSet = cs;
        wireSecurity = sec;
        // setPriority(Thread.MAX_PRIORITY);
    }

    @Override
    public boolean isIgnoring(Identity id) {
        View test = ignoring.get();
        return test != null && test.contains(id);
    }

    @Override
    public void requestConnect(Heartbeat heartbeat, Identity node) {
        sendHeartbeat(heartbeat);
    }

    /**
     * Send a heartbeat message. Logs a failure at the error level.
     * 
     * @param msg
     *            message to send.
     */
    @Override
    public void sendHeartbeat(Heartbeat heartbeat) {
        HeartbeatMsg msg = HeartbeatMsg.toHeartbeatMsg(heartbeat);
        try {
            super.sendObject(wireSecurity.toWireForm(msg, bufferPool));
        } catch (Exception ex) {
            log.error("Error sending heartbeat message", ex);
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
        ignoring.set(ignoringUpdate.isEmpty() ? null : ignoringUpdate);
    }

    @Override
    public void start(Heartbeat initialHeartbeat) {
        super.start();
    }

    /**
     * Change of interface - connection users send objects or messages.
     * sendObject(Object) is not permitted because the only heartbeats can be
     * sent on a heartbeat connection. But the method of the base class
     * MulticastComms to send is to take an arbitrary Object. sendObject().
     * 
     * @param obj
     */
    // public void sendObject(Object obj) { return; }

    @Override
    public void terminate() {
        shutdown();
    }

    private void handleHeartbeat(Heartbeat hb) {
        /**
         * if not right magic discard it
         */
        if (!hb.getSender().equalMagic(me)) {
            if (log.isTraceEnabled()) {
                log.trace("heartbeat discarded due to invalid magic number: "
                          + hb);
            }
            return;
        }

        /**
         * for testing purposes - can ignore messages from specified senders
         */
        if (isIgnoring(hb.getSender())) {
            if (log.isTraceEnabled()) {
                log.trace("Ignoring heart beat sender: " + hb);
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
        if (log.isTraceEnabled()) {
            log.trace("Delivering heart beat: " + hb);
        }
        connectionSet.receiveHeartbeat(hb);
    }

    @Override
    protected void deliverBytes(ByteBuffer bytes) {
        Object obj = null;
        try {

            obj = wireSecurity.fromWireForm(bytes);

        } catch (WireSecurityException ex) {

            log.error(me
                      + "multicast transport encountered security violation receiving message - ignoring the message ");
            return;

        } catch (Exception ex) {
            log.error(me + "Error reading wire form message - ignoring", ex);
            return;
        }

        if (obj instanceof Heartbeat) {
            handleHeartbeat((Heartbeat) obj);
        } else {
            if (log.isInfoEnabled()) {
                log.info(me + "Error reading wire form message - ignoring");
            }
        }
    }

}
