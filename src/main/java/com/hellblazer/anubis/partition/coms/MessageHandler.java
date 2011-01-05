/** (C) Copyright 2010 Hal Hildebrand, all rights reserved.

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
 */
package com.hellblazer.anubis.partition.coms;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.Connection;
import org.smartfrog.services.anubis.partition.comms.IOConnection;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.WireMsg;
import org.smartfrog.services.anubis.partition.wire.WireSizes;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.msg.TimedMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurityException;

import com.hellblazer.anubis.basiccomms.nio.AbstractCommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class MessageHandler extends AbstractCommunicationsHandler implements
        WireSizes, IOConnection {
    private static Logger log = Logger.getLogger(MessageHandler.class.getCanonicalName());
    private boolean announceTerm = true;
    private final ConnectionSet connectionSet;
    private boolean ignoring = false;
    private final Identity me;
    private MessageConnection messageConnection;
    private long receiveCount = INITIAL_MSG_ORDER;
    private long sendCount = INITIAL_MSG_ORDER;
    private final WireSecurity wireSecurity;
    private String toString;

    public MessageHandler(Identity id, ConnectionSet cs, WireSecurity sec,
                          SocketChannel chan, ServerChannelHandler h) {
        super(h, chan);
        me = id;
        connectionSet = cs;
        wireSecurity = sec;
    }

    public MessageHandler(Identity id, ConnectionSet cs, WireSecurity sec,
                          SocketChannel chan, ServerChannelHandler h,
                          MessageConnection mc) {
        super(h, chan);
        me = id;
        connectionSet = cs;
        messageConnection = mc;
        wireSecurity = sec;
        toString = "Anubis: Message Handler (node " + me.id + ", remote node "
                   + messageConnection.getSender().id + ")";
    }

    /* (non-Javadoc)
     * @see com.hellblazer.anubis.basiccomms.nio.CommunicationsHandler#close()
     */
    @Override
    public void close() {
        if (announceTerm && messageConnection != null) {
            messageConnection.closing();
        }
        super.close();
    }

    @Override
    public void send(TimedMsg tm) {
        send(tm, false);
    }

    @Override
    public void setIgnoring(boolean ignoring) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(this + "set ignoring: " + ignoring);
        }
        this.ignoring = ignoring;
    }

    @Override
    public void silent() {
        announceTerm = false;
    }

    @Override
    public void terminate() {
        shutdown();
    }

    @Override
    public String toString() {
        return toString;
    }

    @Override
    protected void closing() {
        if (announceTerm && messageConnection != null) {
            announceTerm = false;
            messageConnection.closing();
        }
    }

    @Override
    protected void deliver(byte[] bytes) {
        if (ignoring) {
            return;
        }

        WireMsg msg = null;
        try {
            msg = wireSecurity.fromWireForm(bytes);
        } catch (WireSecurityException ex) {
            log.severe(me
                       + "connection transport encountered security violation unmarshalling message - ignoring "); // + this.getSender() );
            return;
        } catch (Exception ex) {
            log.severe(me
                       + "connection transport unable to unmarshall message "); // + this.getSender() );
            shutdown();
            return;
        }

        if (!(msg instanceof TimedMsg)) {
            log.severe(me + "connection transport received non timed message "); // + this.getSender() );
            shutdown();
            return;
        }

        final TimedMsg tm = (TimedMsg) msg;

        if (tm.getOrder() != receiveCount) {
            log.severe(String.format("%s connection transport from %s has delivered a message out of order.  Expected: %s  actual: %s - shutting down",
                                     me, tm.getSender(), receiveCount,
                                     tm.getOrder()));
            shutdown();
            return;
        }

        /**
         * handle the message. We do not increment the order for the initial
         * heartbeat message opening a new connection.
         */
        if (messageConnection == null) {
            initialMsg(tm);
        } else {
            receiveCount++;
            handler.dispatch(new Runnable() {
                @Override
                public void run() {
                    messageConnection.deliver(tm);
                }
            });
        }
    }

    @Override
    protected void logClose(String reason, IOException throwable) {
        if (ignoring) {
            return;
        }
        if (messageConnection == null) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE,
                        me
                                + " shutdown unassigned message connection transport:"
                                + reason, throwable);
            }
        } else {
            messageConnection.logClose(reason, throwable);
        }
    }

    protected void send(TimedMsg timedMessage, boolean initial) {
        if (!initial && log.isLoggable(Level.FINEST)) {
            log.finest(String.format("Sending message %s to %s", timedMessage,
                                     messageConnection.getSender()));
        } else if (initial && log.isLoggable(Level.FINE)) {
            log.fine(String.format("Sending initial message %s to %s",
                                   timedMessage, messageConnection.getSender()));
        }
        try {
            timedMessage.setOrder(sendCount);
            if (!initial) {
                sendCount++;
            }
            send(wireSecurity.toWireForm(timedMessage));
        } catch (Exception ex) {
            log.log(Level.SEVERE, me + " failed to marshall timed message: "
                                  + timedMessage
                                  + " - shutting down connection", ex);
            shutdown();
        }
    }

    private void initialMsg(final TimedMsg tm) {

        /**
         * must be a heartbeat message
         */
        if (!(tm instanceof HeartbeatMsg)) {
            log.log(Level.SEVERE,
                    me
                            + " did not receive a heartbeat message as first message - shutdown",
                    new Exception());
            shutdown();
            return;
        }

        HeartbeatMsg hbmsg = (HeartbeatMsg) tm;

        /**
         * There must be a valid connection (heartbeat connection)
         */
        if (!connectionSet.getView().contains(hbmsg.getSender())) {
            log.severe(me + " did not have incoming connection from "
                       + hbmsg.getSender().toString()
                       + " in the connection set");
            shutdown();
            return;
        }

        Connection con = connectionSet.getConnection(hbmsg.getSender());

        /**
         * If it is a message connection then attempt to assign this impl to
         * that connection. If successful then record the message connection so
         * all further messages go directly to it. If not successful then
         * shutdown the this implementation object and abort.
         */
        if (con instanceof MessageConnection) {
            if (((MessageConnection) con).assignImpl(this)) {
                messageConnection = (MessageConnection) con;
                toString = "Anubis: Message Handler (node " + me.id
                           + ", remote node " + con.getSender().id + ")";
                messageConnection.deliver(tm);
            } else {
                log.severe(me + " failed to assign incoming connection from "
                           + con.getSender().toString());
                shutdown();
            }
            return;
        }

        /**
         * By now we should be left with a heartbeat connection - sanity check
         */
        if (!(con instanceof HeartbeatConnection)) {
            log.severe(me
                       + " ?!? incoming connection from "
                       + con.getSender().toString()
                       + " is in connection set, but not heartbeat or message type");
            shutdown();
            return;
        }
        HeartbeatConnection hbcon = (HeartbeatConnection) con;

        /**
         * If the connection is a heartbeat connection then the other end must
         * be setting up the connection without this end having requested it.
         * That means the other end must want it, so check the msgLink field for
         * this end is set - this is a sanity check.
         * 
         * *********************************************************************
         * 
         * The case can happen, so the above comment is incorrect. If the user
         * does a connect and then disconnect without sending a message, then
         * the other end could initiate a connection neither end needs in
         * response to the initial connect. Do not count this as an error, but
         * do log its occurance.
         */
        if (!hbmsg.getMsgLinks().contains(me.id)) {
            log.severe(me
                       + " VALID CASE - FOR INFORMATION ONLY:=> incoming connection from "
                       + con.getSender().toString()
                       + " when neither end wants the connection");
            // next two lines removed to allow this case
            // shutdown();
            // return;
        }

        /**
         * Now we are left with a valid heartbeat connection and the other end
         * is initiating a message connection, so create this end.
         * 
         * Note that the connection set only finds out about the newly created
         * message connection when it is informed by the call to
         * connectionSet.useNewMessageConnection(), so it can not terminate the
         * connection before the call to messageConnection.assignImpl(). Also,
         * we created the message connection, so we know it does not yet have an
         * impl. Hence we can assume it will succeed in assigning the impl.
         */
        messageConnection = new MessageConnection(me, connectionSet,
                                                  hbcon.getProtocol(),
                                                  hbcon.getCandidate());
        messageConnection.assignImpl(this);
        messageConnection.deliver(tm);

        /**
         * if the call to connectionSet.useNewMessageConnection() then a
         * connection has been created since we checked for it above with
         * connectionSet.getConnection(). The other end will not make two
         * connection attempts at the same time, but if this thread is delayed
         * during the last 20 lines of code for long enough for the following to
         * happen: 1. other end time out connection + 2. quiesence period + 3.
         * this end rediscover other end in multicast heartbeats + 4. other end
         * initiates new connection attempt + 5. new connection attempt gets
         * accepted (new thread created for it) + 6. read first heartbeat and
         * get through this code in the new thread. Then it could beat this
         * thread to it. If all this happens (and based on the premise
         * "if it can happen it will happen") then this thread should rightly
         * comit suicide in disgust!!!!
         */
        if (!connectionSet.useNewMessageConnection(messageConnection)) {
            log.severe(me + "Concurrent creation of message connections from "
                       + messageConnection.getSender());
            shutdown();
            return;
        }
        toString = "Anubis: Message Handler (node " + me.id + ", remote node "
                   + messageConnection.getSender().id + ")";
    }
}
