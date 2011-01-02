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
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public abstract class ServerChannelHandler {
    private final static Logger log = Logger.getLogger(ServerChannelHandler.class.getCanonicalName());

    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(Class<T> c) {
        return (T[]) java.lang.reflect.Array.newInstance(c, 2);
    }

    private InetSocketAddress endpoint;
    private SocketOptions options = new SocketOptions();
    private volatile int queueIndex = 0;
    private final ArrayList<CommunicationsHandler>[] readQueue;
    private ExecutorService commsExecutor;
    private ExecutorService dispatchExecutor;
    private Selector selector;
    private int selectTimeout = 1000;
    private ServerSocketChannel server;
    private ServerSocket serverSocket;
    private AtomicBoolean run = new AtomicBoolean();
    private final ArrayList<CommunicationsHandler>[] writeQueue;
    private final Set<CommunicationsHandler> openHandlers = new HashSet<CommunicationsHandler>();
    private InetSocketAddress localAddress;

    @SuppressWarnings("unchecked")
    public ServerChannelHandler() {
        ArrayList<CommunicationsHandler> proto = new ArrayList<CommunicationsHandler>();
        readQueue = newArray(proto.getClass());
        readQueue[0] = new ArrayList<CommunicationsHandler>();
        readQueue[1] = new ArrayList<CommunicationsHandler>();

        writeQueue = newArray(proto.getClass());
        writeQueue[0] = new ArrayList<CommunicationsHandler>();
        writeQueue[1] = new ArrayList<CommunicationsHandler>();
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot open a selector");
        }
    }

    public void connect() throws IOException {
        server = ServerSocketChannel.open();
        serverSocket = server.socket();
        serverSocket.bind(endpoint, options.getBacklog());
        localAddress = new InetSocketAddress(server.socket().getInetAddress(),
                                             server.socket().getLocalPort());
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
        log.fine("Socket is connected");
    }

    public void dispatch(Runnable command) {
        dispatchExecutor.execute(command);
    }

    public ExecutorService getCommsExecutor() {
        return commsExecutor;
    }

    public ExecutorService getDispatchExecutor() {
        return dispatchExecutor;
    }

    public InetAddress getEndpoint() {
        return endpoint.getAddress();
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public SocketOptions getOptions() {
        return options;
    }

    public int getSelectTimeout() {
        return selectTimeout;
    }

    public boolean isRunning() {
        return run.get();
    }

    public void setCommsExecutor(ExecutorService executor) {
        commsExecutor = executor;
    }

    public void setDispatchExecutor(ExecutorService dispatchExecutor) {
        this.dispatchExecutor = dispatchExecutor;
    }

    public void setEndpoint(InetSocketAddress endpoint) {
        this.endpoint = endpoint;
    }

    public void setOptions(SocketOptions options) {
        this.options = options;
    }

    public void setSelectTimeout(int selectTimeout) {
        this.selectTimeout = selectTimeout;
    }

    public void start() {
        if (run.compareAndSet(false, true)) {
            startSelect();
        }
    }

    public void terminate() {
        if (!run.compareAndSet(true, false)) {
            return;
        }
        selector.wakeup();
        disconnect();
        try {
            selector.close();
        } catch (IOException e) {
        }
        commsExecutor.shutdownNow();
        dispatchExecutor.shutdownNow();
        for (CommunicationsHandler handler : openHandlers) {
            try {
                handler.getChannel().close();
            } catch (IOException e) {
            }
        }
        openHandlers.clear();
    }

    protected void addHandler(CommunicationsHandler handler) {
        synchronized (openHandlers) {
            openHandlers.add(handler);
        }
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
        for (CommunicationsHandler handler : readQueue[myQueueIndex]) {
            try {
                handler.getChannel().register(selector, SelectionKey.OP_READ,
                                              handler);
            } catch (CancelledKeyException e) {
                // ignore and queue
                selectForRead(handler);
            } catch (NullPointerException e) {
                // apparently the file descriptor can be nulled
                log.log(Level.FINEST, "anamalous null pointer exception", e);
            }
        }
        readQueue[myQueueIndex].clear();

        if (log.isLoggable(Level.FINE)) {
            log.fine("Adding queued write selects");
        }
        for (CommunicationsHandler handler : writeQueue[myQueueIndex]) {
            try {
                handler.getChannel().register(selector, SelectionKey.OP_WRITE,
                                              handler);
            } catch (CancelledKeyException e) {
                // ignore and queue
                selectForWrite(handler);
            } catch (NullPointerException e) {
                // apparently the file descriptor can be nulled
                log.log(Level.FINE, "anamalous null pointer exception", e);
            }
        }
        writeQueue[myQueueIndex].clear();
    }

    protected void closeHandler(CommunicationsHandler handler) {
        synchronized (openHandlers) {
            openHandlers.remove(handler);
        }
    }

    abstract protected CommunicationsHandler createHandler(SocketChannel accepted);

    protected void disconnect() {
        if (server == null) {
            return; // not connected
        }
        try {
            server.close();
        } catch (IOException e) {
        }
        try {
            serverSocket.close();
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
        CommunicationsHandler handler = createHandler(accepted);
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
        final CommunicationsHandler context = (CommunicationsHandler) key.attachment();
        commsExecutor.execute(new Runnable() {
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
        final CommunicationsHandler context = (CommunicationsHandler) key.attachment();
        commsExecutor.execute(new Runnable() {
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

    protected void selectForRead(CommunicationsHandler handler) {
        ArrayList<CommunicationsHandler> myReadQueue = readQueue[queueIndex];
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

    protected void selectForWrite(CommunicationsHandler handler) {
        ArrayList<CommunicationsHandler> myWriteQueue = writeQueue[queueIndex];
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
        commsExecutor.execute(new Runnable() {
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

    protected Collection<? extends CommunicationsHandler> getOpenHandlers() {
        synchronized (openHandlers) {
            return new ArrayList<CommunicationsHandler>(openHandlers);
        }
    }
}