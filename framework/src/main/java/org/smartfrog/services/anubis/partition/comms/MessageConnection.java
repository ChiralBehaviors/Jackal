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
package org.smartfrog.services.anubis.partition.comms;

import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocol;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolAdapter;
import org.smartfrog.services.anubis.partition.protocols.leader.Candidate;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.Close;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.msg.MessageMsg;
import org.smartfrog.services.anubis.partition.wire.msg.TimedMsg;

public class MessageConnection extends HeartbeatProtocolAdapter implements
        Connection, HeartbeatProtocol, Candidate {

    private static class Closed implements SendBehavior {

        @Override
        public boolean assignImpl(IOConnection impl) {
            return false;
        }

        @Override
        public SendBehavior connect() {
            return this;
        }

        @Override
        public void disconnect() {
        }

        @Override
        public boolean hasPending() {
            return false;
        }

        @Override
        public void send(TimedMsg msg) {
        }

        @Override
        public void setIgnoring(boolean ignoring) {
        }

        @Override
        public void terminate() {
        }
    }

    private class Established implements SendBehavior {

        @Override
        public boolean assignImpl(IOConnection impl) {
            if (log.isInfoEnabled()) {
                log.info(String.format("Attempt to assign a new implementation when one exists for %s",
                                       this));
            }
            return false;
        }

        @Override
        public SendBehavior connect() {
            return this;
        }

        @Override
        public void disconnect() {
            connectionSet.disconnect(getSender());
        }

        @Override
        public boolean hasPending() {
            return false;
        }

        @Override
        public void send(TimedMsg msg) {
            /**
             * If the connection has been terminated then just return. In time
             * the User will be notified that the connection, and therefore the
             * remote node, has terminated.
             */
            if (!connectionImpl.connected()) {
                if (log.isTraceEnabled()) {
                    log.trace(String.format("Message dropped due to closed connection: %s",
                                            this));
                }
                return;
            }

            if (!(msg instanceof Heartbeat)) {
                if (log.isTraceEnabled()) {
                    log.trace(String.format("Sending msg on: %s", this));
                }
            }
            connectionImpl.sendTimed(msg);
        }

        @Override
        public void setIgnoring(boolean ignoring) {
            connectionImpl.setIgnoring(ignoring);
        }

        @Override
        public void terminate() {
            connectionImpl.terminate();
        }

    }

    private class Pending implements SendBehavior {
        private Established                established;
        private final LinkedList<TimedMsg> msgQ = new LinkedList<TimedMsg>();

        @Override
        public boolean assignImpl(IOConnection impl) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Assigning impl: %s", this));
            }
            connectionImpl = impl;
            connectionImpl.setIgnoring(ignoring); // indicate if it should ignore messages
            return true;
        }

        @Override
        public synchronized SendBehavior connect() {
            established = new Established();
            for (TimedMsg msg : msgQ) {
                connectionImpl.sendTimed(msg);
            }
            return established;
        }

        @Override
        public void disconnect() {
            if (msgQ.isEmpty()) {
                connectionSet.disconnect(getSender());
            }
        }

        @Override
        public boolean hasPending() {
            return !msgQ.isEmpty();
        }

        @Override
        public synchronized void send(TimedMsg msg) {
            if (established == null) {
                if (log.isTraceEnabled()) {
                    log.trace(String.format("Queueing msg on: %s", this));
                }
                msgQ.addLast(msg);
            } else {
                established.send(msg);
            }
        }

        @Override
        public void setIgnoring(boolean ignoring) {
            // can't do anything
        }

        @Override
        public void terminate() {
        }
    }

    private interface SendBehavior {

        boolean assignImpl(IOConnection impl);

        SendBehavior connect();

        void disconnect();

        boolean hasPending();

        void send(TimedMsg msg);

        void setIgnoring(boolean ignoring);

        void terminate();

    }

    private static final Logger   log               = LoggerFactory.getLogger(MessageConnection.class.getCanonicalName());
    private volatile IOConnection closingImpl       = null;
    private volatile IOConnection connectionImpl    = null;
    private final ConnectionSet   connectionSet;
    private volatile boolean      disconnectPending = false;

    private boolean               ignoring          = false;

    private final Identity        me;

    private volatile SendBehavior send              = new Pending();

    private volatile boolean      terminated        = false;

    /**
     * Constructor used to create a MessageConnection when the implementation is
     * yet to be built. If this end is initiating then the implementation should
     * be constructed asynchronously by MessageConnectionInititor using its own
     * thread - done to avoid blocking the connectionSet. If this end is not
     * initiating then this object will wait around until the
     * MessageConnectionServer implements its implementation in response to a
     * connection request from the other end.
     * 
     * @param id
     *            - the identity of this node
     * @param cs
     *            - the connection set
     * @param can
     *            - the heartbeat connection (used to get details of the
     *            connection)
     */
    public MessageConnection(Identity id, ConnectionSet cs,
                             HeartbeatProtocol hbp, Candidate can) {
        super(hbp, can);
        me = id;
        connectionSet = cs;
        if (connectionSet.isIgnoring(getId())) {
            ignoring = true;
        }
    }

    /**
     * Inform the messageConnection that an implementation of the connection has
     * been created. When a new connection is completed any outstanding messages
     * should be sent. If there is a disconnect pending then immediately inform
     * the connectionSet to disconnect.
     * 
     * @param impl
     * @return boolean
     */
    public boolean assignImpl(IOConnection impl) {

        if (terminated) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Not assigning impl as connection is terminated: %s",
                                        this));
            }
            return false;
        }
        if (!send.assignImpl(impl)) {
            return false;
        }

        send = send.connect();

        if (disconnectPending) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Disconnecting: %s", this));
            }
            connectionSet.disconnect(getSender());
        }

        return true;
    }

    /**
     * closing() is used by the transport to indicate that it is closing. If the
     * transport detects an error condition it will close itself and call this
     * method.
     */
    public void closing() {
        send = new Closed();
        super.terminate();
        terminated = true;
        connectionSet.removeConnection(this);
    }

    /**
     * Indicate to the MessageConnection that a connect has been called. The
     * purpose is to cancel a pending disconnect.
     */
    public void connect() {
        disconnectPending = false;
    }

    /**
     * General deliver method - any messaage received by the transport will be
     * delivered using this method. The message will be a valid TimedMsg!
     * 
     * @param msg
     *            - the message
     */
    public void deliver(TimedMsg msg) {

        if (msg instanceof HeartbeatMsg) {

            HeartbeatMsg hbmsg = (HeartbeatMsg) msg;

            /**
             * pass off to heartbeat protocol
             */
            super.receiveHeartbeat(hbmsg);

            /**
             * do the checks specific to a heartbeat connection.
             */
            if (connectionSet.thisEndInitiatesConnectionsTo(getSender())) {
                checkInitiatingClose(hbmsg);
            } else {
                checkRespondingClose(hbmsg);
            }

        } else if (msg instanceof MessageMsg) {

            MessageMsg mmsg = (MessageMsg) msg;

            /**
             * The heartbeat protocol is extended to advance the time if any
             * message arrives. This is because receiving any message counts as
             * indication that the other end is still alive
             */
            if (mmsg.getTime() > super.getTime()) {
                super.setTime(mmsg.getTime());
            }
            connectionSet.receiveObject(mmsg.getMessageObject(),
                                        mmsg.getSender(), mmsg.getTime());

        } else if (msg == null) {
            log.error(me + "connection transport delivered null message from "
                      + getSender());
        } else {
            log.error(me
                      + "connection transport delivered unknown message type from "
                      + getSender() + " message=" + msg);
        }
    }

    /**
     * Instruct the messageConnection to disconnect. If there are no messages
     * waiting to be sent then this can be done immediately (by informing the
     * connectionSet of the disconnect - it is actually done via a check on
     * heartbeat delivery, not now). If there are messages pending it implies
     * that the connection has not been created yet and there are messages to
     * send, so we still want to connect even if the application layer no longer
     * wants to send. In this case that there is a disconnect pending but don't
     * tell the connectionSet to disconnect yet.
     */
    public void disconnect() {
        disconnectPending = true;
        if (log.isTraceEnabled()) {
            log.trace(String.format("%s disconnecting from %s", me, getSender()));
        }
        send.disconnect();
    }

    @Override
    public boolean isNotTimely(long timenow, long timebound) {
        return super.isNotTimelyMsgConnection(timenow, timebound);
    }

    @Override
    public boolean isSelf() {
        return false;
    }

    public void logClose(String reason, Throwable throwable) {

        if (log.isTraceEnabled()) {
            log.trace(me + " message connection transport for " + getSender()
                      + " shutdown:" + reason, throwable);
        }

    }

    /**
     * Over-ride the receiveHeartbeat() method. Message connections ignore
     * heartbeats received out of band (i.e. delivered from the multicast
     * heartbeat). Only heartbeats received in-band on their own connection
     * counts.
     * 
     * @param hb
     *            - the heartbeat
     * @return - always false (not valid)
     */
    @Override
    public boolean receiveHeartbeat(Heartbeat hb) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("Non message connection heartbeat rejected from: %s at: %s",
                                    hb.getSender(), me));
        }
        return false;
    }

    public void sendMsg(Heartbeat heartbeat) {
        sendMsg((TimedMsg) HeartbeatMsg.toHeartbeatMsg(heartbeat));
    }

    public void sendMsg(TimedMsg msg) {

        if (msg == null) {
            Exception e = new Exception();
            e.fillInStackTrace();
            log.error(String.format("SendBytes(WireMsg) called with null parameter on: %s",
                                    this), e);
            return;
        }
        send.send(msg);

    }

    /**
     * sendObject() creates a message to transport an object and calls sendMsg()
     * to send it. It also time-stamps the message. The object must be
     * serializable.
     * 
     * @param obj
     *            - the object to transport
     */
    public void sendObject(Object obj) {
        MessageMsg msg = new MessageMsg(me, obj);
        msg.setTime(System.currentTimeMillis());
        sendMsg(msg);
    }

    /**
     * for testing - can set the connection to ignore messages - they will be
     * received, but just dropped
     * 
     * @param ignoring
     */
    public void setIgnoring(boolean ignoring) {
        this.ignoring = ignoring;
        send.setIgnoring(ignoring);
    }

    /**
     * Connection interface - to terminate (kill as opposed to close) the
     * connection. There will be no callback to closing() as a result of
     * terminating the transport.
     */
    @Override
    public void terminate() {
        super.terminate();
        terminated = true;
        send.terminate();
    }

    @Override
    public String toString() {
        return "MessageConnection [from: " + me.id + " to: " + getId().id + "]";
    }

    private void checkInitiatingClose(HeartbeatMsg msg) {
        // System.out.println(me + " initiator close check on link to " + getSender() );
        /**
         * If the connection is already closing check for the returned close
         * heartbeat - note that normal messages and heartbeats are accepted up
         * to the actual return close message.
         */
        if (closingImpl != null) {
            if (msg instanceof Close) {
                closingImpl.terminate();
                closingImpl = null;

                /**
                 * if we have new messages or either end wants the connection
                 * back - reinitiate the connection by re-creating the
                 * implementation.
                 */
                if (send.hasPending() || msg.getMsgLinks().contains(me.id)
                    || connectionSet.wantsMsgLinkTo(getSender())) {

                    // System.out.println(me + " connection closed but re-opening ");
                    // if( !msgQ.isEmpty() ) System.out.println(me + "  -- the message queue is not empty");
                    // if( msg.getMsgLinks().get(me.id) ) System.out.println(me + "  -- the other end appears to want the connection");
                    // if( connectionSet.wantsMsgLinkTo( getSender() ) ) System.out.println(me + "  -- this end appears to want the connection");

                    connectionSet.getConnectionServer().initiateConnection(me,
                                                                           this,
                                                                           connectionSet.getHeartbeat());
                }

                /**
                 * If the connection is to stay closed replace it with a
                 * heartbeat connection in the connection set.
                 */
                else {
                    // System.out.println(me + " connection fully closed - converting back to heartbeat connection");
                    connectionSet.convertToHeartbeatConnection(this);
                }
            } else {
                // System.out.println(me + " connection closing but not a CloseMsg heartbeat");
            }
        }

        /**
         * If the connection is not already closing but it is time to close
         * (i.e. neither end wants the connection) initiate a close - but not if
         * we have some messages to send!!!! (if there are messages hang around
         * until they are sent - clears up case of rapid connect, send,
         * disconnect)
         */
        else if (!msg.getMsgLinks().contains(me.id)
                 && !connectionSet.wantsMsgLinkTo(getSender())
                 && !send.hasPending()) {
            // System.out.println(me + " entering close on connection to " +  getSender() );
            closingImpl = connectionImpl;
            closingImpl.silent();
            send = new Closed();
            connectionImpl = null;
            try {
                closingImpl.send(connectionSet.getHeartbeat().toClose());
            } catch (Exception ex) {
                log.error(String.format("%s failed to marshall close message - not sent to %s",
                                        me, getSender()), ex);
            }
        }
    }

    private void checkRespondingClose(HeartbeatMsg msg) {
        // System.out.println(me + " responder close check on link to " +  getSender() );
        /**
         * If we have a close message then immediately drop the connection. We
         * need to tell the connection impl to be silent - this means don't tell
         * me when you terminate. This is because the initiating end will
         * eventually shutdown the link and the impl will close - when that
         * happens we don't want it cause this messageConnection to terminate
         * too!
         */
        if (msg instanceof Close) {
            // System.out.println(me + " received CloseMsg - closing connection to " +  getSender() );
            sendMsg(connectionSet.getHeartbeat().toClose());
            connectionImpl.silent();
            send = new Closed();
            connectionImpl = null;
            if (!connectionSet.wantsMsgLinkTo(getSender())) {
                // System.out.println(me + " converting back to heartbeat connection");
                connectionSet.convertToHeartbeatConnection(this);
            }
        }
    }
}
