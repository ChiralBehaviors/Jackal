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
package org.smartfrog.services.anubis.partition.comms.nonblocking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.IOConnectionServer;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.jackal.util.SocketOptions;

public class MessageNioServer extends Thread implements IOConnectionServer {
    private InetSocketAddress                           connAdd            = null;
    private ConnectionSet                               connectionSet      = null;
    private RxQueue<MessageNioHandler>                  connectQueue       = null;
    private Vector<SelectionKey>                        deadKeys           = null;

    private Identity                                    me                 = null;
    volatile private boolean                            open               = false;

    private Hashtable<SocketChannel, MessageNioHandler> pendingNewChannels = null;
    private final int                                   RX_WORKERS         = 4;
    private RxQueue<RxJob>[]                            rxQueue            = null;
    private int                                         rxQueueCounter     = 0;
    private Selector                                    selector           = null;
    private ServerSocketChannel                         server             = null;
    private static final Logger                         syncLog            = Logger.getLogger(MessageNioServer.class.getCanonicalName());
    private Logger                                      asyncLog           = syncLog;                                                    // need to wrap with async log wrapper
    private WireSecurity                                wireSecurity       = null;
    private SocketOptions                               socketOptions;

    private Vector<SelectionKey>                        writePendingKeys   = null;

    /**
     * This is a single threaded TCP socket server (de)multiplexing many
     * socketChannels. Rx deserialized objects are put on a decoupling queue so
     * that the selector thread can return as quickly as possible to its task of
     * managing channels. A pre-defined number of worker threads deal with those
     * RX deserialized jobs and deliver them to the anubis layers. By default
     * there is only 1 decoupling thread.
     * 
     * @param socketOptions
     */
    @SuppressWarnings("unchecked")
    public MessageNioServer(InetSocketAddress address, Identity id,
                            ConnectionSet cs, WireSecurity sec,
                            SocketOptions socketOptions) {
        this.socketOptions = socketOptions;
        setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                syncLog.log(Level.WARNING, "Uncaught exception", e);
            }
        });

        if (asyncLog.isLoggable(Level.FINER)) {
            asyncLog.finer("MNS: constructing a new server");
        }

        me = id;
        connectionSet = cs;
        wireSecurity = sec;

        // create a server
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            socketOptions.configure(server.socket());
            server.socket().bind(address, socketOptions.getBacklog());
            connAdd = (InetSocketAddress) server.socket().getLocalSocketAddress();
            if (asyncLog.isLoggable(Level.INFO)) {
                asyncLog.info("MNS: server bound to: " + connAdd);
            }
            selector = Selector.open();
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (Exception e) {
            if (asyncLog.isLoggable(Level.WARNING)) {
                asyncLog.log(Level.WARNING, "error creating server", e);
            }
        }
        setName("Anubis: Nio Message Server (node " + me.id + ")");
        setPriority(MAX_PRIORITY);
        pendingNewChannels = new Hashtable<SocketChannel, MessageNioHandler>();
        deadKeys = new Vector<SelectionKey>();
        writePendingKeys = new Vector<SelectionKey>();
        open = true;

        // create as many queues as needed
        int workerNumber = RX_WORKERS;
        RxQueueWorker[] queueWorker = new RxQueueWorker[workerNumber];
        rxQueue = new RxQueue[workerNumber];
        for (int i = 0; i < workerNumber; ++i) {
            rxQueue[i] = new RxQueue<RxJob>();
            queueWorker[i] = new RxQueueWorker(rxQueue[i]);
            queueWorker[i].setName("Anubis: Nio RxQueueWorker #" + i
                                   + " (node " + me.id + ")");
            queueWorker[i].setPriority(MAX_PRIORITY);
            queueWorker[i].setDaemon(true);
            queueWorker[i].start();
        }

        // create a a thread to finish the connection of pending connections
        // new queue, RxQueue turns out to be a generic queue not specific to rxJobs...
        connectQueue = new RxQueue<MessageNioHandler>();
        ConnectWorker connectWorker = new ConnectWorker(connectQueue);
        connectWorker.setName("Anubis: Nio Connection Assign (node " + me.id
                              + ")");
        connectWorker.setDaemon(true);
        connectWorker.start();
    }

    @Override
    public InetSocketAddress getAddress() {
        if (asyncLog.isLoggable(Level.FINER)) {
            asyncLog.finer("MNS: getAddress is called");
        }
        return connAdd;
    }

    @Override
    public String getThreadStatusString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(super.getName()).append(" ............................ ").setLength(30);
        buffer.append(super.isAlive() ? ".. is Alive " : ".. is Dead ");
        buffer.append(open ? ".. running ....." : ".. terminated ..");
        if (connAdd != null) {
            buffer.append(" address = ").append(connAdd.getAddress().getHostName()).append(":").append(connAdd.getPort());
        }
        return buffer.toString();
    }

    @Override
    public void initiateConnection(Identity id, MessageConnection con,
                                   Heartbeat hb) {
        if (syncLog.isLoggable(Level.FINEST)) {
            syncLog.finest("Initiating connection to " + con.getSender());
        }
        NonBlockingConnectionInitiator initiator = null;
        try {
            initiator = new NonBlockingConnectionInitiator(
                                                           con,
                                                           HeartbeatMsg.toHeartbeatMsg(hb),
                                                           wireSecurity);
        } catch (Exception ex) {
            if (asyncLog.isLoggable(Level.WARNING)) {
                asyncLog.log(Level.WARNING,
                             String.format("Cannot initiate connection to %s",
                                           con.getSender()), ex);
            }
            return;
        }
        startConnection(con.getSenderAddress(), me, connectionSet, con,
                        initiator);
    }

    @Override
    public void run() {
        try {
            if (syncLog.isLoggable(Level.FINE)) {
                syncLog.fine(me.toString() + getName() + " thread started");
            }

            runLoop();

            if (syncLog.isLoggable(Level.FINE)) {
                syncLog.fine(me.toString() + getName()
                             + " thread has terminated without throwable");
            }
        } catch (Throwable thr) {
            syncLog.log(Level.SEVERE,
                        me.toString() + getName()
                                + " THREAD HAS TERMINATED WITH THROWABLE", thr);
        }
    }

    public void startConnection(InetSocketAddress conAd, Identity me,
                                ConnectionSet cs, MessageConnection con,
                                NonBlockingConnectionInitiator mci) {
        if (asyncLog.isLoggable(Level.FINER)) {
            asyncLog.finer("MNS: startConnection is called: " + conAd);
        }
        MessageNioHandler mnh = null;
        try {
            SocketChannel sendingChannel = SocketChannel.open();
            socketOptions.configure(sendingChannel.socket());
            sendingChannel.configureBlocking(false);
            mnh = new MessageNioHandler(selector, sendingChannel, deadKeys,
                                        writePendingKeys, assignRxQueue(),
                                        wireSecurity);
            mnh.init(me, cs, con, mci);
            sendingChannel.connect(new InetSocketAddress(conAd.getAddress(),
                                                         conAd.getPort()));
            if (asyncLog.isLoggable(Level.FINER)) {
                asyncLog.finer("MNS: Trying to register a new channel");
            }
            pendingNewChannels.put(sendingChannel, mnh);
            selector.wakeup();
        } catch (ClosedByInterruptException e) {
            if (asyncLog.isLoggable(Level.FINE)) {
                asyncLog.log(Level.FINE, "Connection closed", e);
            }
        } catch (Exception e) {
            if (asyncLog.isLoggable(Level.WARNING)) {
                asyncLog.log(Level.WARNING, "Cannot start connection", e);
            }
        }

    }

    @Override
    public void terminate() {

        if (asyncLog.isLoggable(Level.FINER)) {
            asyncLog.finer("MNS: terminate is called");
        }
        // do what shutdown does - i.e. wrap that thread up
        open = false;
        selector.wakeup();
        connectQueue.shutdown();
        for (int i = 0; i < RX_WORKERS; i++) {
            rxQueue[i].shutdown();
        }
    }

    private RxQueue<RxJob> assignRxQueue() {
        RxQueue<RxJob> retQueue = rxQueue[rxQueueCounter];
        ++rxQueueCounter;
        if (rxQueueCounter == RX_WORKERS) {
            rxQueueCounter = 0;
        }
        return retQueue;
    }

    // this is the selector thread - only woken up when there is activity on a registered socketChannel
    private void runLoop() {

        Set<SelectionKey> keys = null;
        Iterator<SelectionKey> iter = null;
        SelectionKey key = null;
        while (open) {
            if (asyncLog.isLoggable(Level.FINER)) {
                asyncLog.finer(getName()
                               + ": Calling select and presumably, block...");
            }
            try {
                selector.select();
            } catch (Exception e) {
                if (asyncLog.isLoggable(Level.WARNING)) {
                    asyncLog.log(Level.WARNING, "", e);
                }
            }

            // finally registering pending channels
            synchronized (pendingNewChannels) {
                if (!pendingNewChannels.isEmpty()) {
                    if (asyncLog.isLoggable(Level.FINER)) {
                        asyncLog.finer("MNS: Registering pending channels: "
                                       + pendingNewChannels.size());
                    }
                    SocketChannel sc = null;
                    MessageNioHandler mnh = null;
                    for (Enumeration<SocketChannel> en = pendingNewChannels.keys(); en.hasMoreElements();) {
                        sc = en.nextElement();
                        if (sc != null) {
                            mnh = pendingNewChannels.remove(sc);
                            try {
                                synchronized (selector) {
                                    // sc.register(selector, SelectionKey.OP_CONNECT | SelectionKey.OP_READ, mnh);
                                    sc.register(selector,
                                                SelectionKey.OP_CONNECT, mnh);
                                }
                            } catch (Exception e) {
                                if (asyncLog.isLoggable(Level.WARNING)) {
                                    asyncLog.log(Level.WARNING,
                                                 "error registering selector",
                                                 e);
                                }
                            }

                        }
                    }
                }
            }

            // blocking is over - process keys
            synchronized (selector) {
                keys = selector.selectedKeys();
                iter = keys.iterator();
            }

            while (iter.hasNext()) {
                if (asyncLog.isLoggable(Level.FINER)) {
                    asyncLog.finer(getName()
                                   + ": going through the key iterator");
                }
                key = iter.next();

                if (key.isAcceptable()) {
                    if (asyncLog.isLoggable(Level.FINER)) {
                        asyncLog.finer(getName()
                                       + ": isAcceptable() returned true");
                    }
                    try {
                        SocketChannel clientChannel = server.accept();
                        socketOptions.configure(clientChannel.socket());
                        clientChannel.configureBlocking(false);
                        MessageNioHandler conHandler = new MessageNioHandler(
                                                                             selector,
                                                                             clientChannel,
                                                                             deadKeys,
                                                                             writePendingKeys,
                                                                             assignRxQueue(),
                                                                             wireSecurity);
                        conHandler.init(me, connectionSet);
                        // only register for read - write is still enabled by default
                        clientChannel.register(selector, SelectionKey.OP_READ,
                                               conHandler);
                        conHandler.readyForWriting();
                        conHandler.setConnected(true);
                    } catch (Exception e) {
                        if (asyncLog.isLoggable(Level.WARNING)) {
                            asyncLog.log(Level.WARNING,
                                         "Error accepting connection", e);
                        }
                    }

                }

                if (key.isReadable()) {
                    if (asyncLog.isLoggable(Level.FINER)) {
                        asyncLog.finer(getName()
                                       + ": isReadable() returned true");
                    }
                    MessageNioHandler mnh = (MessageNioHandler) key.attachment();
                    ByteBuffer readFullBuffer = mnh.newDataToRead(key);
                    if (readFullBuffer != null) {
                        // create a container for the deserialized job and put on decoupling queue
                        RxJob rxJob = new RxJob(mnh, readFullBuffer);
                        mnh.getRxQueue().add(rxJob);
                    }
                }

                if (key.isWritable()) {
                    if (asyncLog.isLoggable(Level.FINER)) {
                        asyncLog.finer(getName()
                                       + ": isWritable() returned true");
                    }
                    key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
                    MessageNioHandler mnh = (MessageNioHandler) key.attachment();
                    if (mnh.isWritePending()) {
                        if (asyncLog.isLoggable(Level.FINER)) {
                            asyncLog.finer("MNS: PENDING WRITE - SELECTOR WOKE UP ON OP_WRITE");
                        }
                        mnh.writeData();
                    } else {
                        if (asyncLog.isLoggable(Level.FINER)) {
                            asyncLog.finer("MNS: setting channel readyForWriting");
                        }
                        mnh.readyForWriting();
                    }
                }

                if (key.isConnectable()) {
                    if (asyncLog.isLoggable(Level.FINER)) {
                        asyncLog.finer(getName()
                                       + ": isConnectable() returned true");
                    }
                    key.interestOps(key.interestOps()
                                    & ~SelectionKey.OP_CONNECT);
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    MessageNioHandler mnh = (MessageNioHandler) key.attachment();
                    boolean connected = false;
                    if (clientChannel.isConnectionPending()) {
                        try {
                            clientChannel.finishConnect();
                            connected = true;
                        } catch (IOException ioe) {
                            if (asyncLog.isLoggable(Level.WARNING)) {
                                asyncLog.log(Level.WARNING,
                                             String.format("Cannot finish connect for: %s",
                                                           clientChannel), ioe);
                            }
                            try {
                                clientChannel.close();
                            } catch (IOException e) {
                                asyncLog.log(Level.FINEST,
                                             String.format("Cannot close: %s",
                                                           clientChannel), ioe);
                            }
                        }
                    }
                    if (connected) {
                        // merge both calls?
                        if (asyncLog.isLoggable(Level.FINER)) {
                            asyncLog.finer("MNS: setting mnh to right booleans in connectable switch test");
                        }
                        mnh.readyForWriting();
                        mnh.setConnected(true);
                        // do here what was being done in MessageConnectionInitiator before - send to a decoupling queue...
                        connectQueue.add(mnh);
                        try {
                            clientChannel.register(selector,
                                                   SelectionKey.OP_READ, mnh);
                        } catch (Exception ex) {
                            if (asyncLog.isLoggable(Level.WARNING)) {
                                asyncLog.log(Level.WARNING,
                                             String.format("Cannot register read for: %s",
                                                           clientChannel), ex);
                            }
                        }
                    }
                }

                iter.remove();

            }

            // killing of dead Keys
            synchronized (deadKeys) {
                if (!deadKeys.isEmpty()) {
                    if (asyncLog.isLoggable(Level.FINER)) {
                        asyncLog.finer("MNS: Killing of at least one key/channel: "
                                       + deadKeys.size());
                    }
                    SelectionKey deadKey = null;
                    SocketChannel deadChannel = null;
                    int deadKeyNumber = deadKeys.size();
                    for (int i = 0; i < deadKeyNumber; ++i) {
                        deadKey = deadKeys.remove(0);
                        if (asyncLog.isLoggable(Level.FINER)) {
                            asyncLog.finer("MNS: *** Cleanup starting ***" + i);
                        }
                        if (deadKey != null) {
                            deadChannel = (SocketChannel) deadKey.channel();
                            deadKey.cancel();
                            try {
                                if (asyncLog.isLoggable(Level.FINER)) {
                                    asyncLog.finer("MNS: Calling deadChannel.close()");
                                }
                                deadChannel.close();
                            } catch (Exception e) {
                                if (asyncLog.isLoggable(Level.WARNING)) {
                                    asyncLog.log(Level.WARNING, "", e);
                                }
                            }
                        }
                    }
                }
            }

            // registration of write interest for pending writes
            synchronized (writePendingKeys) {
                if (asyncLog.isLoggable(Level.FINER)) {
                    asyncLog.finer("MNS: Examining the content of writePendingKeys");
                }
                if (!writePendingKeys.isEmpty()) {
                    if (asyncLog.isLoggable(Level.FINER)) {
                        asyncLog.finer("MNS: Registering interest for write "
                                       + writePendingKeys.size());
                    }
                    SelectionKey pendingKey = null;
                    int pendingNumber = writePendingKeys.size();
                    if (asyncLog.isLoggable(Level.FINER)) {
                        asyncLog.finer("MNS: pending write NUmber: "
                                       + pendingNumber);
                    }
                    for (int i = 0; i < pendingNumber; ++i) {
                        pendingKey = writePendingKeys.remove(0);
                        pendingKey.interestOps(pendingKey.interestOps()
                                               | SelectionKey.OP_WRITE);
                    }
                }
            }

        } // while open

        // thread is being killed - close down gracefully
        if (asyncLog.isLoggable(Level.FINER)) {
            asyncLog.finer("MNS: server thread is EXITING... Why?");
        }
        try {
            selector.close();
            server.close();
        } catch (IOException ioe) {
            if (asyncLog.isLoggable(Level.WARNING)) {
                asyncLog.log(Level.WARNING, "", ioe);
            }
        }

    }

    @Override
    public void start(Heartbeat initialHeartbeat) {
        start();
    }

}
