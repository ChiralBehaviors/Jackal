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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServer;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class ServerChannelHandler implements IOConnectionServer {
    private final static Logger log = Logger.getLogger(ServerChannelHandler.class.getCanonicalName());

    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(Class<T> c) {
        return (T[]) java.lang.reflect.Array.newInstance(c, 2);
    }

    protected InetSocketAddress endpoint;
    protected SocketOptions options = new SocketOptions();
    protected volatile int queueIndex = 0;
    protected final ArrayList<MessageHandler>[] readQueue;
    protected ExecutorService executor;
    protected Selector selector;
    protected int selectTimeout = 1000;
    protected ServerSocketChannel server;
    protected ServerSocket serverSocket;
    protected AtomicBoolean run = new AtomicBoolean();
    private WireSecurity wireSecurity;
    protected final ArrayList<MessageHandler>[] writeQueue;
    protected volatile int writeQueueIndex = 0;
    private Identity identity;
    private ConnectionSet connectionSet;
    private final Set<MessageHandler> openHandlers = new HashSet<MessageHandler>();
    private InetSocketAddress localAddress;

    @SuppressWarnings("unchecked")
    public ServerChannelHandler() {
        ArrayList<MessageHandler> proto = new ArrayList<MessageHandler>();
        readQueue = newArray(proto.getClass());
        readQueue[0] = new ArrayList<MessageHandler>();
        readQueue[1] = new ArrayList<MessageHandler>();

        writeQueue = newArray(proto.getClass());
        writeQueue[0] = new ArrayList<MessageHandler>();
        writeQueue[1] = new ArrayList<MessageHandler>();
    }

    @Override
    public ConnectionAddress getAddress() {
        return new ConnectionAddress(localAddress);
    }

    public ConnectionSet getConnectionSet() {
        return connectionSet;
    }

    public InetAddress getEndpoint() {
        return endpoint.getAddress();
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public Identity getIdentity() {
        return identity;
    }

    public SocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketOptions getOptions() {
        return options;
    }

    public int getSelectTimeout() {
        return selectTimeout;
    }

    @Override
    public String getThreadStatusString() {
        return "ServerChannelHandler for " + localAddress + " running: "
               + run.get();
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
        synchronized (openHandlers) {
            openHandlers.add(handler);
        }
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

    public void setEndpoint(InetSocketAddress endpoint) {
        this.endpoint = endpoint;
    }

    public void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    public void setIdentity(Identity id) {
        identity = id;
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

    @Override
    public void start() {
        if (run.compareAndSet(false, true)) {
            startSelect();
        }
    }

    @Override
    public void terminate() {
        if (!run.compareAndSet(true, false)) {
            return;
        }
        selector.wakeup();
        disconnect();
        executor.shutdownNow();
        for (MessageHandler handler : openHandlers) {
            try {
                handler.getChannel().close();
            } catch (IOException e) {
            }
        }
        openHandlers.clear();
    }

    protected void addQueuedSelects() throws ClosedChannelException,
                                     IOException {
        int myQueueIndex = queueIndex;

        if (queueIndex == 0) {
            queueIndex = 1;
        } else {
            queueIndex = 0;
        }

        if (log.isLoggable(Level.FINE)) {
            log.fine("Adding queued read selects");
        }
        for (MessageHandler context : readQueue[myQueueIndex]) {
            try {
                context.getChannel().register(selector, SelectionKey.OP_READ,
                                              context);
            } catch (CancelledKeyException e) {
                // ignore and queue
                selectForRead(context);
            } catch (NullPointerException e) {
                // apparently the file descriptor can be nulled
                log.log(Level.FINEST, "anamalous null pointer exception", e);
            }
        }
        readQueue[myQueueIndex].clear();

        if (log.isLoggable(Level.FINE)) {
            log.fine("Adding queued write selects");
        }
        for (MessageHandler context : writeQueue[myQueueIndex]) {
            try {
                context.getChannel().register(selector, SelectionKey.OP_WRITE,
                                              context);
            } catch (CancelledKeyException e) {
                // ignore and queue
                selectForWrite(context);
            } catch (NullPointerException e) {
                // apparently the file descriptor can be nulled
                log.log(Level.FINE, "anamalous null pointer exception", e);
            }
        }
        writeQueue[myQueueIndex].clear();
    }

    protected void closeHandler(MessageHandler handler) {
        synchronized (openHandlers) {
            openHandlers.remove(handler);
        }
    }

    protected void connect() throws IOException {
        server = ServerSocketChannel.open();
        serverSocket = server.socket();
        serverSocket.bind(endpoint, options.getBacklog());
        localAddress = new InetSocketAddress(server.socket().getInetAddress(),
                                             server.socket().getLocalPort());
        server.configureBlocking(false);
        selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        log.fine("Socket is connected");
    }

    protected void disconnect() {
        run.set(false);
        try {
            server.close();
        } catch (IOException e) {
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
        }
        try {
            selector.close();
        } catch (IOException e) {
        }
    }

    protected void handleAccept(SelectionKey key,
                                Iterator<SelectionKey> selected)
                                                                throws IOException {
        if (!run.get()) {
            log.fine("Ignoring accept as handler is not started");
            return;
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("Handling accept");
        }
        selected.remove();
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel accepted = server.accept();
        options.configure(accepted.socket());
        accepted.configureBlocking(false);
        MessageHandler handler = new MessageHandler(identity, connectionSet,
                                                    wireSecurity, accepted,
                                                    this);
        synchronized (openHandlers) {
            openHandlers.add(handler);
        }
        handler.handleAccept();
    }

    protected void handleRead(SelectionKey key, Iterator<SelectionKey> selected) {
        if (!run.get()) {
            log.fine("Ignoring read ready as handler is not started");
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("Handling read");
        }
        selected.remove();
        key.cancel();
        final MessageHandler context = (MessageHandler) key.attachment();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                context.handleRead();
            }
        });
    }

    protected void handleWrite(SelectionKey key, Iterator<SelectionKey> selected) {
        if (!run.get()) {
            log.fine("Ignoring write ready as handler is not started");
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("Handling write");
        }
        selected.remove();
        key.cancel();
        final MessageHandler context = (MessageHandler) key.attachment();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                context.handleWrite();
            }
        });
    }

    protected void select() throws IOException {
        selector.selectNow();
        addQueuedSelects();

        if (log.isLoggable(Level.FINE)) {
            log.fine("Selecting");
        }
        selector.select(selectTimeout);

        // get an iterator over the set of selected keys
        Iterator<SelectionKey> selected;
        try {
            selected = selector.selectedKeys().iterator();
        } catch (ClosedSelectorException e) {
            return;
        }

        while (run.get() && selected.hasNext()) {
            SelectionKey key = selected.next();
            if (key.isAcceptable()) {
                handleAccept(key, selected);
            } else if (key.isReadable()) {
                handleRead(key, selected);
            } else if (key.isWritable()) {
                handleWrite(key, selected);
            } else {
                log.warning("Unhandled key: " + key);
            }
        }
    }

    protected void selectForRead(MessageHandler handler) {
        ArrayList<MessageHandler> myReadQueue = readQueue[queueIndex];
        synchronized (myReadQueue) {
            myReadQueue.add(handler);
        }
        try {
            selector.wakeup();
        } catch (NullPointerException e) {
            // Bug in JRE
            log.log(Level.FINE, "Caught null pointer in selector wakeup", e);
        }
    }

    protected void selectForWrite(MessageHandler handler) {
        ArrayList<MessageHandler> myWriteQueue = writeQueue[queueIndex];
        synchronized (myWriteQueue) {
            myWriteQueue.add(handler);
        }
        selector.wakeup();
    }

    protected void startSelect() {
        if (!run.get()) {
            log.fine("Handler is not started");
            return;
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                while (run.get()) {
                    try {
                        select();
                    } catch (ClosedSelectorException e) {
                        log.log(Level.FINE, "Channel closed", e);
                    } catch (IOException e) {
                        log.log(Level.FINE, "IOException when selecting: "
                                            + server, e);
                    } catch (Throwable e) {
                        log.log(Level.SEVERE,
                                "Runtime exception when selecting", e);
                    }
                }
            }
        });
    }
}