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
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServer;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.anubis.basiccomms.nio.CommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class ConnectionServer extends ServerChannelHandler implements
        IOConnectionServer {
    private Logger log = Logger.getLogger(ConnectionServer.class.getCanonicalName());
    protected Identity identity;
    protected ConnectionSet connectionSet;
    protected WireSecurity wireSecurity;

    @Override
    public ConnectionAddress getAddress() {
        return new ConnectionAddress(getLocalAddress());
    }

    public ConnectionSet getConnectionSet() {
        return connectionSet;
    }

    public Identity getIdentity() {
        return identity;
    }

    @Override
    public String getThreadStatusString() {
        return "ServerChannelHandler for " + getLocalAddress() + " running: "
               + isRunning();
    }

    public WireSecurity getWireSecurity() {
        return wireSecurity;
    }

    @Override
    public void initiateConnection(Identity id, MessageConnection connection,
                                   HeartbeatMsg heartbeat) {
        SocketChannel channel = null;
        InetSocketAddress toAddress = connection.getSenderAddress().asSocketAddress();
        try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(toAddress);
            while (!channel.finishConnect()) {
                Thread.sleep(10);
            }
        } catch (IOException ex) {
            log.log(Level.WARNING, "Cannot open connection to: " + toAddress,
                    ex);
            try {
                if (channel != null) {
                    channel.close();
                }
            } catch (Exception ex2) {
            }
            return;
        } catch (InterruptedException e) {
            return;
        }
        MessageHandler handler = new MessageHandler(id, connectionSet,
                                                    wireSecurity, channel,
                                                    this, connection);
        addHandler(handler);
        handler.send(heartbeat, true);
        if (connection.assignImpl(handler)) {
            handler.handleAccept();
        } else {
            handler.terminate();
        }
    }

    public void setConnectionSet(ConnectionSet cs) {
        connectionSet = cs;
    }

    public void setIdentity(Identity id) {
        identity = id;
    }

    public void setWireSecurity(WireSecurity wireSecurity) {
        this.wireSecurity = wireSecurity;
    }

    @Override
    protected CommunicationsHandler createHandler(SocketChannel accepted) {
        MessageHandler handler = new MessageHandler(identity, connectionSet,
                                                    wireSecurity, accepted,
                                                    this);
        addHandler(handler);
        return handler;
    }

}
