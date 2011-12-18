/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.partition.comms;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.IOConnectionServer;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.pinkie.CommunicationsHandler;
import com.hellblazer.pinkie.CommunicationsHandlerFactory;
import com.hellblazer.pinkie.ServerSocketChannelHandler;
import com.hellblazer.pinkie.SocketOptions;

/**
 * 
 * @author hhildebrand
 * 
 */
public class ConnectionServer implements IOConnectionServer {
    private final static Logger log = Logger.getLogger(ConnectionServer.class.getCanonicalName());

    private class HandlerFactory implements CommunicationsHandlerFactory {

        @Override
        public CommunicationsHandler createCommunicationsHandler(SocketChannel channel) {
            return new MessageHandler(wireSecurity, me, connectionSet);
        }

    }

    private final ConnectionSet              connectionSet;
    private final ServerSocketChannelHandler handler;
    private final Identity                   me;
    private final WireSecurity               wireSecurity;
    private final Executor                   dispatcher;

    public ConnectionServer(Executor commsExec,
                            InetSocketAddress endpointAddress,
                            SocketOptions socketOptions, Identity id,
                            ConnectionSet connectionSet,
                            WireSecurity wireSecurity) throws IOException {
        this.connectionSet = connectionSet;
        this.wireSecurity = wireSecurity;
        this.me = id;
        handler = new ServerSocketChannelHandler(
                                                 String.format("Connection server for %s",
                                                               id),
                                                 socketOptions,
                                                 endpointAddress, commsExec,
                                                 new HandlerFactory());
        dispatcher = Executors.newSingleThreadExecutor();
    }

    @Override
    public InetSocketAddress getAddress() {
        return handler.getLocalAddress();
    }

    @Override
    public String getThreadStatusString() {
        return String.format("Connection server for %s", me);
    }

    @Override
    public void initiateConnection(Identity id, MessageConnection con,
                                   Heartbeat hb) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Initiating connection to " + con.getSender());
        }
        InetSocketAddress conAd = con.getSenderAddress();
        try {
            handler.connectTo(conAd,
                              new MessageHandler(
                                                 dispatcher,
                                                 wireSecurity,
                                                 me,
                                                 connectionSet,
                                                 con,
                                                 new ConnectionInitiator(
                                                                         con,
                                                                         HeartbeatMsg.toHeartbeatMsg(hb),
                                                                         wireSecurity,
                                                                         dispatcher)));
        } catch (ClosedByInterruptException e) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, "Connection closed", e);
            }
        } catch (Exception e) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, "Cannot start connection", e);
            }
        }
    }

    @Override
    public void start(Heartbeat initialHeartbeat) {
        handler.start();
    }

    @Override
    public void terminate() {
        handler.terminate();
    }

}
