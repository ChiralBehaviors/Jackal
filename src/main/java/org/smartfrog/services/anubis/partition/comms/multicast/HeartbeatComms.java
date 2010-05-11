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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
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

    private HeartbeatReceiver connectionSet = null;
    /**
     * for testing purposes
     */
    private View ignoring = null;
    private Object ingnoringMonitor = new Object();
    private Logger log = Logger.getLogger(this.getClass().toString());
    private Identity me = null;

    private Map messageHandlers = Collections.synchronizedMap(new HashMap());
    private WireSecurity wireSecurity = null;

    /**
        * Set up multicast coms specifying the network interface
        * @param address - multicast address
        * @param inf - network interface to use (specified as a connection address)
        * @param cs - connection set
        * @param threadName - name for multicast server thread
        * @param id - local identify
        * @param sec - security module
        * @throws Exception
        */
    public HeartbeatComms(MulticastAddress address, ConnectionAddress inf,
                          HeartbeatReceiver cs, String threadName, Identity id,
                          WireSecurity sec) throws Exception {
        super(threadName, address, inf);
        me = id;
        connectionSet = cs;
        wireSecurity = sec;
        setPriority(Thread.MAX_PRIORITY);
    }

    /**
    * Set up multicast comms - uses default network interface
    * @param address - multicast address
    * @param cs - connection set
    * @param threadName - name for multicast server thread
    * @param id - local identify
    * @param sec - security module
    * @throws Exception
    */
    public HeartbeatComms(MulticastAddress address, HeartbeatReceiver cs,
                          String threadName, Identity id, WireSecurity sec) throws Exception {
        super(threadName, address);
        me = id;
        connectionSet = cs;
        wireSecurity = sec;
        setPriority(Thread.MAX_PRIORITY);
    }

    public void deregisterMessageHandler(Class type) {
        messageHandlers.remove(type);
    }

    public boolean isIgnoring(Identity id) {
        synchronized (ingnoringMonitor) {
            return ignoring != null && ignoring.contains(id);
        }
    }

    public void registerMessageHandler(Class type, MessageHandler handler) {
        messageHandlers.put(type, handler);
    }

    /**
     * Send a heartbeat message.
     * Logs a failure at the error level.
     * @param msg message to send.
     */
    public void sendHeartbeat(HeartbeatMsg msg) {
        try {
            super.sendObject(wireSecurity.toWireForm(msg));
        } catch (Exception ex) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE, "Error sending heartbeat message", ex);
            }
        }
    }

    /**
     * determine which nodes to ignore if any.
     * This will be called from the connection set - ignoring is used in a
     * critical section in deliverObject() that uses connectionSet as its
     * monitor.
     * @param ignoringUpdate a view
     */
    public void setIgnoring(View ignoringUpdate) {
        synchronized (ingnoringMonitor) {
            ignoring = ignoringUpdate.isEmpty() ? null : ignoringUpdate;
        }
    }

    /**
     * Change of interface - connection users send objects or messages.
     * sendObject(Object) is not permitted because the only heartbeats
     * can be sent on a heartbeat connection. But the method of the base class
     * MulticastComms to send is to take an arbitrary Object.
     * sendObject().
     *
     * @param obj
     */
    //public void sendObject(Object obj) { return; }

    public void terminate() {
        shutdown();
    }

    private void handleHeartbeat(Heartbeat hb) {
        /**
         * if not right magic discard it
         */
        if (!hb.getSender().equalMagic(me)) {
            return;
        }

        /**
         * for testing purposes - can ignore messages from
         * specified senders
         */
        if (isIgnoring(hb.getSender())) {
            return;
        }

        /**
         * Deliver new heartbeat message - this needs to be synchronized
         * on the connection set so that nothing changes in the process.
         *
         * The connection returned by getConnection may or may not be active
         * (i.e. it may be quiescing) if it is not active it is up to the
         * connection itself to deal with the heartbeat.
         */
        connectionSet.receiveHeartbeat(hb);
    }

    private void handleNonHeartbeat(Object msg) {
        MessageHandler handler = (MessageHandler) messageHandlers.get(msg.getClass());
        if (handler != null) {
            handler.deliverObject(msg);
        } else if (log.isLoggable(Level.SEVERE)) {
            log.severe(me + " No handler found for message: " + msg);
        }
    }

    @Override
    protected void deliverBytes(byte[] bytes) {

        Object obj = null;
        try {

            obj = wireSecurity.fromWireForm(bytes);

        } catch (WireSecurityException ex) {

            if (log.isLoggable(Level.SEVERE)) {
                log.severe(me
                           + "multicast transport encountered security violation receiving message - ignoring the message "); // + this.getSender() );
            }
            return;

        } catch (Exception ex) {
            if (log.isLoggable(Level.SEVERE)) {
                log.log(Level.SEVERE,
                        me + "Error reading wire form message - ignoring", ex);
            }
            return;
        }

        if (obj instanceof Heartbeat) {
            handleHeartbeat((Heartbeat) obj);
        } else {
            handleNonHeartbeat(obj);
        }
    }

}
