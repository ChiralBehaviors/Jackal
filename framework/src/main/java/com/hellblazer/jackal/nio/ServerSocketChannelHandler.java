package com.hellblazer.jackal.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ServerSocketChannelHandler extends ChannelHandler {

    private static Logger log = Logger.getLogger(ServerSocketChannelHandler.class.getCanonicalName());

    public static ServerSocketChannel bind(SocketOptions options,
                                           InetSocketAddress endpointAddress)
                                                                             throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        ServerSocket serverSocket = server.socket();
        serverSocket.bind(endpointAddress, options.getBacklog());
        return server;
    }

    public static InetSocketAddress getLocalAddress(ServerSocketChannel channel) {

        return new InetSocketAddress(channel.socket().getInetAddress(),
                                     channel.socket().getLocalPort());
    }

    public ServerSocketChannelHandler(String handlerName,
                                      SelectableChannel channel,
                                      InetSocketAddress endpointAddress,
                                      SocketOptions socketOptions,
                                      ExecutorService commsExec,
                                      ExecutorService dispatchExec)
                                                                   throws IOException {
        super(handlerName, channel, endpointAddress, socketOptions, commsExec,
              dispatchExec);
    }

    @Override
    protected void dispatch(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            handleAccept(key);
        } else if (key.isReadable()) {
            handleRead(key);
        } else if (key.isWritable()) {
            handleWrite(key);
        } else if (key.isConnectable()) {
            handleConnect(key);
        } else {
            if (log.isLoggable(Level.WARNING)) {
                log.warning("Unhandled key: " + key);
            }
        }
    }

    protected void handleConnect(SelectionKey key) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Handling read");
        }
        key.cancel();
        try {
            ((SocketChannel) key.channel()).finishConnect();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to finish connection", e);
        }
        if (log.isLoggable(Level.FINE)) {
            log.fine("Dispatching connected action");
        }
        try {
            dispatch((Runnable) key.attachment());
        } catch (RejectedExecutionException e) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, "cannot execute connect action", e);
            }
        }
    }

    protected void selectForConnect(CommunicationsHandler handler,
                                    Runnable connectAction) {
        try {
            register(handler.getChannel(), connectAction,
                     SelectionKey.OP_CONNECT);
        } catch (CancelledKeyException e) {
            if (log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING,
                        String.format("Cancelled key for %s", handler), e);
            }
            throw new IllegalStateException(e);
        }
        wakeup();
    }
}
