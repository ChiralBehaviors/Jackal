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

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionFactory;
import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionServer;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServer;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

/**
 * When a connection request is received the MessageConnectionServer accepts it,
 * but at that point it doesn't know where it is coming from, and so can not
 * assign it to a message connection. Instead it creates a MessageConnectionImpl
 * and parks it in a set of pending connections. When the MessageConnectionImpl
 * receives the initial message it will remove itself from the pending set.
 * 
 * <p>
 * Title: Anubis Detection Service
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Hewlett-Packard Ltd.
 * </p>
 * 
 * @author Paul Murray
 * @version 1.0
 */
public class MessageConnectionServer extends ConnectionServer implements
        IOConnectionServer, ConnectionFactory {

    private ConnectionSet connectionSet = null;
    private static final Logger log = Logger.getLogger(MessageConnectionServer.class.getCanonicalName());
    private Identity me = null;
    private Set pending = new HashSet();
    private WireSecurity wireSecurity = null;

    /**
     * Constructor - sets this class as the ConnectionFactory.
     * 
     * @param address
     *            - the address for the server socket
     * @param id
     *            - the id of this node
     * @param cs
     *            - the connection set
     * @throws IOException
     * @throws Exception
     *             - if problems with creating the server socket
     */
    public MessageConnectionServer(ConnectionAddress address, Identity id,
                                   ConnectionSet cs, WireSecurity sec)
                                                                      throws IOException {
        super("Anubis: Connection Server (node " + id.id + ")",
              address.ipaddress.getHostName(), address.port);
        me = id;
        connectionSet = cs;
        wireSecurity = sec;
        setConnectionFactory(this);
        setPriority(MAX_PRIORITY);
    }

    /**
     * ConnectionFactory interface Create a new MessageConnectionImpl in
     * response to a connection request. This method is called by the
     * ConnectionServer base class.
     * 
     * @param channel
     * 
     *            public void createConnection(Socket so) {
     *            MessageConnectionImpl impl = new MessageConnectionImpl(me, so,
     *            this, connectionSet); impl.start(); pending.add(impl); }
     */
    @Override
    public void createConnection(SocketChannel channel) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("creating connection for: " + channel);
        }
        MessageConnectionImpl impl = new MessageConnectionImpl(me, channel,
                                                               this,
                                                               connectionSet,
                                                               wireSecurity);
        impl.start();
        pending.add(impl);
    }

    /**
     * Asynchronously initiate a new connection.
     * 
     * @param id
     *            Identity
     * @param con
     *            MessageConnection
     * @param hb
     *            HeartbeatMsg
     */
    @Override
    public void initiateConnection(Identity id, MessageConnection con,
                                   HeartbeatMsg hb) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("Initiating connection to: " + id);
        }
        BlockingConnectionInitiator initiator = null;
        try {
            initiator = new BlockingConnectionInitiator(id, con, connectionSet,
                                                        hb, wireSecurity);
        } catch (Exception ex) {
            log.log(Level.SEVERE, "Error initiating a blocking connection to: "
                                  + id, ex);
            return;
        }
        initiator.setDaemon(true);
        initiator.start();
    }

    /**
     * Remove a MessageConnectionImpl from the pending set - called when an
     * initial message has been received by the impl.
     * 
     * @param con
     *            - the impl
     */
    public void removeConnection(MessageConnectionImpl con) {
        if (log.isLoggable(Level.FINER)) {
            log.finer("removing connection : " + con);
        }
        pending.remove(con);
    }

    /**
     * kill this MessageConnectionServer - stops the server thread. Should also
     * remove any pending implementations
     */
    @Override
    public void terminate() {
        // FIX ME: remove all pending impls
        shutdown();
    }

}
