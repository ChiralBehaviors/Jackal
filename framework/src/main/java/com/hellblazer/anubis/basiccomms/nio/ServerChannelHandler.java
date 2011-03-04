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
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a full featured non blocking NIO socket server with
 * outbound connection capabilities. The
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

    private final ExecutorService commsExecutor;
    private final ExecutorService dispatchExecutor;
    private final InetSocketAddress endpoint;
    private InetSocketAddress localAddress;
    private final Set<CommunicationsHandler> openHandlers = new HashSet<CommunicationsHandler>();
    private final SocketOptions options;
    private final BlockingDeque<CommunicationsHandler> readQueue;
    private final AtomicBoolean run = new AtomicBoolean();
    protected final Selector selector;
    private final ExecutorService selectService;
    private Future<?> selectTask;
    private final int selectTimeout = 1000;
    private ServerSocketChannel server;
    private ServerSocket serverSocket;
    private final BlockingDeque<CommunicationsHandler> writeQueue;

    public ServerChannelHandler(InetSocketAddress endpointAddress,
                                SocketOptions socketOptions,
                                ExecutorService commsExec,
                                ExecutorService dispatchExec) {
        endpoint = endpointAddress;
        commsExecutor = commsExec;
        dispatchExecutor = dispatchExec;
        options = socketOptions;
        readQueue = new LinkedBlockingDeque<CommunicationsHandler>();
        writeQueue = new LinkedBlockingDeque<CommunicationsHandler>();
        selectService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r, "Server channel handler select");
                daemon.setDaemon(true);
                daemon.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.WARNING,
                                "Uncaught exception on select handler", e);
                    }
                });
                return daemon;
            }
        });
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
        log.finest("Socket is connected");
    }

    public void dispatch(Runnable command) {
        dispatchExecutor.execute(command);
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
        selectTask.cancel(true);
        selectService.shutdownNow();
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
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Adding queued read selects");
        }
        ArrayList<CommunicationsHandler> selectors = new ArrayList<CommunicationsHandler>(
                                                                                          100);
        readQueue.drainTo(selectors);
        for (CommunicationsHandler handler : selectors) {
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

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Adding queued write selects");
        }
        selectors = new ArrayList<CommunicationsHandler>(100);
        writeQueue.drainTo(selectors);
        for (CommunicationsHandler handler : selectors) {
            try {
                handler.getChannel().register(selector, SelectionKey.OP_WRITE,
                                              handler);
            } catch (CancelledKeyException e) {
                // ignore and queue
                selectForWrite(handler);
            } catch (NullPointerException e) {
                // apparently the file descriptor can be nulled
                log.log(Level.FINEST, "anamalous null pointer exception", e);
            }
        }
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

    protected Collection<? extends CommunicationsHandler> getOpenHandlers() {
        synchronized (openHandlers) {
            return new ArrayList<CommunicationsHandler>(openHandlers);
        }
    }

    protected void handleAccept(SelectionKey key,
                                Iterator<SelectionKey> selected)
                                                                throws IOException {
        if (!run.get()) {
            log.info("Ignoring accept as handler is not started");
            return;
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Handling accept");
        }
        selected.remove();
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel accepted = server.accept();
        options.configure(accepted.socket());
        accepted.configureBlocking(false);
        if (log.isLoggable(Level.FINE)) {
            log.fine(String.format("Connection accepted: %s", accepted));
        }
        CommunicationsHandler handler = createHandler(accepted);
        addHandler(handler);
        handler.handleAccept();
    }

    protected void handleRead(SelectionKey key, Iterator<SelectionKey> selected) {
        if (!run.get()) {
            log.info("Ignoring read ready as handler is not started");
            return;
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Handling read");
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
            log.info("Ignoring write ready as handler is not started");
            return;
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Handling write");
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

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Selecting");
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
            } else if (key.isConnectable()) {
                handleConnect(key, selected);
            } else {
                log.warning("Unhandled key: " + key);
            }
        }
    }

    protected void handleConnect(SelectionKey key,
                                 Iterator<SelectionKey> selected) {
        if (!run.get()) {
            log.info("Ignoring connect as handler is not started");
            return;
        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Handling read");
        }
        selected.remove();
        key.cancel();
        if (log.isLoggable(Level.FINE)) {
            log.fine("Dispatching connected action");
        }
        dispatch((Runnable) key.attachment());
    }

    protected void selectForRead(CommunicationsHandler handler) {
        readQueue.add(handler);
        try {
            selector.wakeup();
        } catch (NullPointerException e) {
            // Bug in JRE
            log.log(Level.FINEST, "Caught null pointer in selector wakeup", e);
        }
    }

    protected void selectForWrite(CommunicationsHandler handler) {
        writeQueue.add(handler);
        selector.wakeup();
    }

    protected void selectForConnect(CommunicationsHandler handler,
                                    Runnable connectAction) {
        try {
            handler.getChannel().register(selector, SelectionKey.OP_CONNECT,
                                          connectAction);
        } catch (CancelledKeyException e) {
            log.log(Level.WARNING, String.format("Cancelled key for %s"), e);
            throw new IllegalStateException(e);
        } catch (NullPointerException e) {
            // apparently the file descriptor can be nulled
            log.log(Level.FINEST, "anamalous null pointer exception", e);
        } catch (ClosedChannelException e) {
            log.log(Level.FINEST, "channel has been closed", e);
        }
        try {
            selector.wakeup();
        } catch (NullPointerException e) {
            // Bug in JRE
            log.log(Level.FINEST, "Caught null pointer in selector wakeup", e);
        }
    }

    protected void startSelect() {
        if (!run.get()) {
            log.info("Handler is not started");
            return;
        }
        selectTask = selectService.submit(new Runnable() {
            @Override
            public void run() {
                while (run.get()) {
                    try {
                        select();
                    } catch (ClosedSelectorException e) {
                        log.log(Level.FINER, "Channel closed", e);
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