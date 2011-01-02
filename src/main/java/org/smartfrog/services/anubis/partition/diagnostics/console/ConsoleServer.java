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
package org.smartfrog.services.anubis.partition.diagnostics.console;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.util.Identity;

import com.hellblazer.anubis.basiccomms.nio.CommunicationsHandler;
import com.hellblazer.anubis.basiccomms.nio.ServerChannelHandler;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class ConsoleServer extends ServerChannelHandler {
    private static final Logger log = Logger.getLogger(ConsoleServer.class.getCanonicalName());

    @Override
    protected CommunicationsHandler createHandler(SocketChannel accepted) {
        throw new UnsupportedOperationException();
    }

    public MessageHandler connectTo(ConnectionAddress address, NodeData node,
                                    Identity nodeId, Controller controller)
                                                                           throws InterruptedException {
        SocketChannel channel = null;
        InetSocketAddress toAddress = address.asSocketAddress();
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
            return null;
        }
        MessageHandler handler = new MessageHandler(controller, node, this,
                                                    channel);
        addHandler(handler);
        handler.handleAccept();
        return handler;
    }
}
