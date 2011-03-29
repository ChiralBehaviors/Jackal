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
package com.hellblazer.jackal.nio;

import static java.lang.String.format;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionException;
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
public abstract class ChannelHandler {
    private final static Logger log = Logger.getLogger(ChannelHandler.class.getCanonicalName());

    @SuppressWarnings("unchecked")
    public static <T> T[] newArray(Class<T> c) {
        return (T[]) java.lang.reflect.Array.newInstance(c, 2);
    }

    private final ExecutorService                                   commsExecutor;
    private final ExecutorService                                   dispatchExecutor;
    private final InetSocketAddress                                 localAddress;
    private final Map<CommunicationsHandler, CommunicationsHandler> openHandlers  = new ConcurrentHashMap<CommunicationsHandler, CommunicationsHandler>();
    private final SocketOptions                                     options;
    private final BlockingDeque<CommunicationsHandler>              readQueue;
    private final AtomicBoolean                                     run           = new AtomicBoolean();
    private final Selector                                          selector;
    private final ExecutorService                                   selectService;
    private Future<?>                                               selectTask;
    private final int                                               selectTimeout = 1000;
    private final SelectableChannel                                 server;
    private final BlockingDeque<CommunicationsHandler>              writeQueue;
    private final String                                            name;

    public ChannelHandler(String handlerName, SelectableChannel channel,
                          InetSocketAddress endpointAddress,
                          SocketOptions socketOptions,
                          ExecutorService commsExec,
                          ExecutorService dispatchExec) throws IOException {
        name = handlerName;
        server = channel;
        localAddress = endpointAddress;
        commsExecutor = commsExec;
        dispatchExecutor = dispatchExec;
        options = socketOptions;
        readQueue = new LinkedBlockingDeque<CommunicationsHandler>();
        writeQueue = new LinkedBlockingDeque<CommunicationsHandler>();
        selectService = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(
                                           r,
                                           format("Server channel handler select for %s",
                                                  name));
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
        selector = Selector.open();
        server.configureBlocking(false);
        server.register(selector, SelectionKey.OP_ACCEPT);
    }

    public void dispatch(Runnable command) {
        dispatchExecutor.execute(command);
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
            startService();
            log.info(format("%s is started, local address: %s", name,
                            localAddress));
        }
    }

    public void terminate() {
        if (run.compareAndSet(true, false)) {
            terminateService();
            log.info(format("%s is terminated, local address: %s", name,
                            localAddress));
        }
    }

    protected void addHandler(CommunicationsHandler handler) {
        openHandlers.put(handler, handler);
    }

    /**
     * @throws ClosedChannelException  
     * @throws IOException 
     */
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
                register(handler.getChannel(), handler, SelectionKey.OP_READ);
            } catch (CancelledKeyException e) {
                // ignore and enqueue
                selectForWrite(handler);
            }
        }

        if (log.isLoggable(Level.FINEST)) {
            log.finest("Adding queued write selects");
        }
        selectors = new ArrayList<CommunicationsHandler>(100);
        writeQueue.drainTo(selectors);
        for (CommunicationsHandler handler : selectors) {
            try {
                register(handler.getChannel(), handler, SelectionKey.OP_WRITE);
            } catch (CancelledKeyException e) {
                // ignore and enqueue
                selectForRead(handler);
            }
        }
    }

    protected void register(SocketChannel channel, Object context, int operation) {
        try {
            channel.register(selector, operation, context);
        } catch (NullPointerException e) {
            // apparently the file descriptor can be nulled
            log.log(Level.FINEST, "anamalous null pointer exception", e);
        } catch (ClosedChannelException e) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "channel has been closed", e);
            }
            return;
        }
    }

    protected void closeHandler(CommunicationsHandler handler) {
        openHandlers.remove(handler);
    }

    abstract protected CommunicationsHandler createHandler(SocketChannel accepted);

    protected void dispatch(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            handleAccept(key);
        } else if (key.isReadable()) {
            handleRead(key);
        } else if (key.isWritable()) {
            handleWrite(key);
        } else {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("Unhandled key: " + key);
            }
        }
    }

    protected void handleAccept(SelectionKey key) throws IOException {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Handling accept");
        }
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

    protected void handleRead(SelectionKey key) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Handling read");
        }
        key.cancel();
        final CommunicationsHandler context = (CommunicationsHandler) key.attachment();
        if (!context.getChannel().isOpen()) {
            context.close();
        } else {
            try {
                commsExecutor.execute(new Runnable() {
                    @Override
                    public void run() {
                        context.handleRead();
                    }
                });
            } catch (RejectedExecutionException e) {
                if (log.isLoggable(Level.FINEST)) {
                    log.log(Level.FINEST, "cannot execute read handling", e);
                }
            }
        }
    }

    protected void handleWrite(SelectionKey key) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Handling write");
        }
        key.cancel();
        final CommunicationsHandler context = (CommunicationsHandler) key.attachment();
        try {
            commsExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    context.handleWrite();
                }
            });
        } catch (RejectedExecutionException e) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "cannot execute write handling", e);
            }
        }
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
            selected.remove();
            try {
                dispatch(key);
            } catch (CancelledKeyException e) {
                if (log.isLoggable(Level.FINE)) {
                    log.log(Level.FINE, format("Cancelled Key: %s", key), e);
                }
            }
        }
    }

    protected void selectForRead(CommunicationsHandler handler) {
        readQueue.add(handler);
        wakeup();
    }

    protected void wakeup() {
        try {
            selector.wakeup();
        } catch (NullPointerException e) {
            // Bug in JRE
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "Caught null pointer in selector wakeup",
                        e);
            }
        }
    }

    protected void selectForWrite(CommunicationsHandler handler) {
        writeQueue.add(handler);
        wakeup();
    }

    protected void startService() {
        selectTask = selectService.submit(new Runnable() {
            @Override
            public void run() {
                while (run.get()) {
                    try {
                        select();
                    } catch (ClosedSelectorException e) {
                        if (log.isLoggable(Level.FINE)) {
                            log.log(Level.FINER, "Channel closed", e);
                        }
                    } catch (IOException e) {
                        if (log.isLoggable(Level.FINE)) {
                            log.log(Level.FINE, "Error when selecting: "
                                                + server, e);
                        }
                    } catch (CancelledKeyException e) {
                        if (log.isLoggable(Level.FINE)) {
                            log.log(Level.FINE, "Error when selecting: "
                                                + server, e);
                        }
                    } catch (Throwable e) {
                        log.log(Level.SEVERE,
                                "Runtime exception when selecting", e);
                    }
                }
            }
        });
    }

    protected void terminateService() {
        selector.wakeup();
        try {
            server.close();
        } catch (IOException e) {
            // do not log
        }
        try {
            selector.close();
        } catch (IOException e) {
            // do not log
        }
        selectTask.cancel(true);
        selectService.shutdownNow();
        commsExecutor.shutdownNow();
        dispatchExecutor.shutdownNow();
        for (Iterator<CommunicationsHandler> iterator = openHandlers.keySet().iterator(); iterator.hasNext();) {
            try {
                iterator.next().getChannel().close();
            } catch (IOException e) {
                // do not log
            }
            iterator.remove();
        }
        openHandlers.clear();
    }
}