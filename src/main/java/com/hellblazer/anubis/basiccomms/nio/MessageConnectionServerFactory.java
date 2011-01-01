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
package com.hellblazer.anubis.basiccomms.nio;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServer;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class MessageConnectionServerFactory implements
        IOConnectionServerFactory {
    private WireSecurity wireSecurity;
    private SocketOptions options = new SocketOptions();
    private int selectTimeout = 1000;
    private ExecutorService executor;

    @Override
    public IOConnectionServer create(ConnectionAddress address, Identity id,
                                     ConnectionSet cs) throws IOException {
        ServerChannelHandler handler = new ServerChannelHandler();
        handler.setEndpoint(address.asSocketAddress());
        handler.setOptions(options);
        handler.setWireSecurity(wireSecurity);
        handler.setSelectTimeout(selectTimeout);
        handler.setExecutor(executor);
        handler.setIdentity(id);
        handler.setConnectionSet(cs);
        handler.connect();
        return handler;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public SocketOptions getOptions() {
        return options;
    }

    public int getSelectTimeout() {
        return selectTimeout;
    }

    public WireSecurity getWireSecurity() {
        return wireSecurity;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void setOptions(SocketOptions options) {
        this.options = options;
    }

    public void setSelectTimeout(int selectTimeout) {
        this.selectTimeout = selectTimeout;
    }

    public void setWireSecurity(WireSecurity wireSecurity) {
        this.wireSecurity = wireSecurity;
    }
}
