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
package org.smartfrog.services.anubis.partition.comms.blocking;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionComms;
import org.smartfrog.services.anubis.partition.comms.Connection;
import org.smartfrog.services.anubis.partition.comms.IOConnection;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.WireMsg;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.msg.TimedMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurityException;
import static java.lang.String.format;

public class MessageConnectionImpl extends ConnectionComms implements
        IOConnection {

    private boolean                 announceTerm      = true;
    private ConnectionSet           connectionSet     = null;
    /**
     * for testing purposes - can set to ignoring incoming messages
     */
    private boolean                 ignoring          = false;
    private static final Logger     log               = Logger.getLogger(MessageConnectionImpl.class.getCanonicalName());
    private Identity                me                = null;
    private MessageConnection       messageConnection = null;
    private long                    receiveCount      = INITIAL_MSG_ORDER;
    private long                    sendCount         = INITIAL_MSG_ORDER;
    private MessageConnectionServer server            = null;
    private WireSecurity            wireSecurity      = null;

    public MessageConnectionImpl(Identity id, ConnectionSet cs,
                                 InetSocketAddress address,
                                 MessageConnection mc, WireSecurity sec) {
        super(format("Anubis: Connection Comms (node %s, remote node %s)",
                     id.id, mc.getSender()), address);
        me = id;
        connectionSet = cs;
        messageConnection = mc;
        wireSecurity = sec;
        setPriority(MAX_PRIORITY);
    }

    public MessageConnectionImpl(Identity id, SocketChannel channel,
                                 MessageConnectionServer mcs, ConnectionSet cs,
                                 WireSecurity sec) {
        super(format("Anubis: %s Connection Comms (node %s)", id, id.id),
              channel);
        me = id;
        server = mcs;
        connectionSet = cs;
        wireSecurity = sec;
        setPriority(MAX_PRIORITY);
    }

    /**
     * Close down the connection.
     * 
     * Closing is called by {@link #shutdown()}. shutdown is used to terminate
     * the connection both here (from the "outside") and in the implementation
     * of ConnectionComms (from the "inside"). The connection closes itself from
     * the inside if there is some kind of error on the connection.
     * 
     * Here closing is used to clean up by telling the server socket to remove
     * any record it has of this connection and telling the messageConnection
     * that the connection has closed. The later can be disabled (as is done in
     * terminate()).
     */
    @Override
    public void closing() {
        if (server != null) {
            server.removeConnection(this);
        }
        if (announceTerm && messageConnection != null) {
            messageConnection.closing();
        }
    }

    @Override
    public void deliver(byte[] bytes) {

        if (ignoring) {
            return;
        }

        WireMsg msg = null;
        try {

            msg = wireSecurity.fromWireForm(bytes);

        } catch (WireSecurityException ex) {
            log.log(Level.SEVERE,
                    format("%s connection transport encountered security violation unmarshalling message - ignoring ",
                           me), ex);
            return;

        } catch (Exception ex) {
            log.log(Level.SEVERE,
                    format("%s connection transport unable to unmarshall message",
                           me), ex);
            shutdown();
            return;
        }

        if (!(msg instanceof TimedMsg)) {
            log.severe(me + "connection transport received non timed message ");
            shutdown();
            return;
        }

        TimedMsg tm = (TimedMsg) msg;

        if (tm.getOrder() != receiveCount) {
            log.severe(format("%s connection transport has delivered a message out of order - shutting down",
                              me));
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
            messageConnection.deliver(tm);
        }
    }

    @Override
    public void logClose(String reason, Throwable throwable) {

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

    @Override
    public void send(TimedMsg tm) {
        try {
            tm.setOrder(sendCount);
            sendCount++;
            super.send(wireSecurity.toWireForm(tm));
        } catch (Exception ex) {
            log.log(Level.SEVERE,
                    format("%s failed to marshall timed message: %s - shutting down connection",
                           me, tm), ex);
            shutdown();
        }
    }

    @Override
    public void send(Heartbeat heartbeat) {
        send((TimedMsg) HeartbeatMsg.toHeartbeatMsg(heartbeat));
    }

    /**
     * set ignoring value to determine if connections should be ignored
     * 
     * @param ignoring
     */
    @Override
    public void setIgnoring(boolean ignoring) {
        this.ignoring = ignoring;
    }

    @Override
    public void silent() {
        announceTerm = false;
    }

    /**
     * Shut down the connection. Terminate is used to instruct the
     * implementation to shutdown the connection. announceTerm is set to false
     * so that the closing() method does not call back to the messageConnection.
     */
    @Override
    public void terminate() {
        announceTerm = false;
        shutdown();
    }

    private void initialMsg(TimedMsg tm) {

        Object obj = tm;
        TimedMsg bytes = tm;

        /**
         * must be a heartbeat message
         */
        if (!(obj instanceof HeartbeatMsg)) {
            log.log(Level.SEVERE,
                    format("%s did not receive a heartbeat message first - shutdown",
                           me), new Exception());
            shutdown();
            return;
        }

        HeartbeatMsg hbmsg = (HeartbeatMsg) obj;

        /**
         * There must be a valid connection (heartbeat connection)
         */
        if (!connectionSet.getView().contains(hbmsg.getSender())) {
            log.severe(format("%s did not have incoming connection from %s in the connection set",
                              hbmsg));
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
                setName(format("Anubis: Connection Comms (node %s, remote node %s)",
                               me.id, con.getSender().id));
                messageConnection.deliver(bytes);
            } else {
                log.severe(format("%s failed to assign incoming connection from %s",
                                  me, con.getSender()));
                shutdown();
            }
            return;
        }

        /**
         * By now we should be left with a heartbeat connection - sanity check
         */
        if (!(con instanceof HeartbeatConnection)) {
            log.severe(format("%s ?!? incoming connection from %s is in connection set, but not heartbeat or message type",
                              me, con.getSender()));
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
            log.info(format("%s VALID CASE - FOR INFORMATION ONLY:=> incoming connection from %s when neither end wants the connection",
                            me, con.getSender()));
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
        messageConnection.deliver(bytes);

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
        setName(format("Anubis: Connection Comms (node %s, remote node %s)",
                       me.id, messageConnection.getSender().id));
    }
}
