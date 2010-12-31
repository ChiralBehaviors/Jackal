package com.hellblazer.anubis.basiccomms.nio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.comms.Connection;
import org.smartfrog.services.anubis.partition.comms.IOConnection;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatConnection;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.WireMsg;
import org.smartfrog.services.anubis.partition.wire.WireSizes;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.msg.TimedMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurityException;

public class MessageHandler implements WireSizes, IOConnection {
    static enum State {
        INITIAL, HEADER, MESSAGE, ERROR, CLOSE;
    }

    private static Logger log = Logger.getLogger(MessageHandler.class.toString());
    private boolean announceTerm = true;
    private final ConnectionSet connectionSet;
    private boolean ignoring = false;
    private final Identity me;
    private MessageConnection messageConnection;
    private long receiveCount = INITIAL_MSG_ORDER;
    private long sendCount = INITIAL_MSG_ORDER;
    private final WireSecurity wireSecurity;
    private volatile boolean open = true;
    private final SocketChannel channel;
    private ByteBuffer headerIn = ByteBuffer.wrap(new byte[HEADER_SIZE]);
    private ByteBuffer headerOut = ByteBuffer.wrap(new byte[HEADER_SIZE]);
    private ByteBuffer msgIn;
    private ByteBuffer msgOut;
    private final ServerChannelHandler handler;
    private volatile State readState = State.INITIAL;
    private volatile State writeState = State.INITIAL;
    private String toString;
    private final Semaphore writeGate = new Semaphore(1);

    public MessageHandler(Identity id, ConnectionSet cs, WireSecurity sec,
                          SocketChannel chan, ServerChannelHandler h) {
        me = id;
        connectionSet = cs;
        wireSecurity = sec;
        channel = chan;
        handler = h;
    }

    public MessageHandler(Identity id, ConnectionSet cs, WireSecurity sec,
                          SocketChannel chan, ServerChannelHandler h,
                          MessageConnection mc) {
        me = id;
        connectionSet = cs;
        messageConnection = mc;
        wireSecurity = sec;
        toString = "Anubis: Message Handler (node " + me.id + ", remote node "
                   + messageConnection.getSender().id + ")";
        channel = chan;
        handler = h;
    }

    @Override
    public boolean connected() {
        return open;
    }

    @Override
    public void send(TimedMsg tm) {
        send(tm, false);
    }

    @Override
    public void setIgnoring(boolean ignoring) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(this + "set ignoring: " + ignoring);
        }
        this.ignoring = ignoring;
    }

    @Override
    public void silent() {
        announceTerm = false;
    }

    @Override
    public void terminate() {
        announceTerm = false;
        shutdown();
    }

    @Override
    public String toString() {
        return toString;
    }

    protected void close() {
        readState = writeState = State.CLOSE;
        if (announceTerm && messageConnection != null) {
            messageConnection.closing();
        }
        open = false;
        try {
            channel.close();
        } catch (IOException e) {
        }
        handler.closeHandler(this);
    }

    protected SocketChannel getChannel() {
        return channel;
    }

    protected void handleAccept() {
        readState = State.INITIAL;
        writeState = State.INITIAL;
        handler.selectForRead(this);
    }

    protected void handleRead() {
        switch (readState) {
            case INITIAL: {
                headerIn.clear();
                readState = State.HEADER;
                readHeader();
                break;
            }
            case HEADER: {
                readHeader();
                break;
            }
            case ERROR: {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("In error, ignoring read ready");
                }
                break; // Don't read while in error
            }
            case MESSAGE: {
                readMessage();
                break;
            }
            case CLOSE: {
                break; // ignore
            }
            default:
                throw new IllegalStateException("Invalid read state");
        }
    }

    protected void handleWrite() {
        switch (writeState) {
            case INITIAL: {
                throw new IllegalStateException("Should never be initial state");
            }
            case HEADER: {
                writeHeader();
                break;
            }
            case ERROR: {
                if (log.isLoggable(Level.FINEST)) {
                    log.finest("In error, ignoring write ready");
                }
                break; // Don't write while in error
            }
            case MESSAGE: {
                writeMessage();
                break;
            }
            case CLOSE: {
                break; // ignore
            }
            default:
                throw new IllegalStateException("Invalid write state");
        }
    }

    protected void send(TimedMsg timedMessage, boolean initial) {
        try {
            timedMessage.setOrder(sendCount);
            if (!initial) {
                sendCount++;
            }
            send(wireSecurity.toWireForm(timedMessage));
        } catch (Exception ex) {
            log.log(Level.SEVERE, me + " failed to marshall timed message: "
                                  + timedMessage
                                  + " - shutting down connection", ex);
            shutdown();
        }
    }

    private void closing() {
        if (announceTerm && messageConnection != null) {
            messageConnection.closing();
        }
    }

    private void deliver(byte[] bytes) {
        if (ignoring) {
            return;
        }

        WireMsg msg = null;
        try {
            msg = wireSecurity.fromWireForm(bytes);
        } catch (WireSecurityException ex) {
            log.severe(me
                       + "connection transport encountered security violation unmarshalling message - ignoring "); // + this.getSender() );
            return;
        } catch (Exception ex) {
            log.severe(me
                       + "connection transport unable to unmarshall message "); // + this.getSender() );
            shutdown();
            return;
        }

        if (!(msg instanceof TimedMsg)) {
            log.severe(me + "connection transport received non timed message "); // + this.getSender() );
            shutdown();
            return;
        }

        TimedMsg tm = (TimedMsg) msg;

        if (tm.getOrder() != receiveCount) {
            log.severe(me
                       + "connection transport has delivered a message out of order.  Expected: "
                       + receiveCount + " actual: " + tm.getOrder()
                       + " - shutting down");
            shutdown();
            return;
        }

        /**
         * handle the message. We do not increment the order for the initial
         * heartbeat message opening a new connection.
         */
        if (messageConnection == null) {
            initialMsg(tm);
        } else {
            receiveCount++;
            messageConnection.deliver(tm);
        }
    }

    private void initialMsg(TimedMsg tm) {

        Object obj = tm;
        TimedMsg bytes = tm;

        /**
         * must be a heartbeat message
         */
        if (!(obj instanceof HeartbeatMsg)) {
            log.log(Level.SEVERE,
                    me
                            + " did not receive a heartbeat message as first message - shutdown",
                    new Exception());
            shutdown();
            return;
        }

        HeartbeatMsg hbmsg = (HeartbeatMsg) obj;

        /**
         * There must be a valid connection (heartbeat connection)
         */
        if (!connectionSet.getView().contains(hbmsg.getSender())) {
            log.severe(me + " did not have incoming connection from "
                       + hbmsg.getSender().toString()
                       + " in the connection set");
            shutdown();
            return;
        }

        Connection con = connectionSet.getConnection(hbmsg.getSender());

        /**
         * If it is a message connection then attempt to assign this impl to
         * that connection. If successful then record the message connection so
         * all further messages go directly to it. If not successful then
         * shutdown the this implementation object and abort.
         */
        if (con instanceof MessageConnection) {
            if (((MessageConnection) con).assignImpl(this)) {
                messageConnection = (MessageConnection) con;
                toString = "Anubis: Message Handler (node " + me.id
                           + ", remote node " + con.getSender().id + ")";
                messageConnection.deliver(bytes);
            } else {
                log.severe(me + " failed to assign incoming connection from "
                           + con.getSender().toString());
                shutdown();
            }
            return;
        }

        /**
         * By now we should be left with a heartbeat connection - sanity check
         */
        if (!(con instanceof HeartbeatConnection)) {
            log.severe(me
                       + " ?!? incoming connection from "
                       + con.getSender().toString()
                       + " is in connection set, but not heartbeat or message type");
            shutdown();
            return;
        }
        HeartbeatConnection hbcon = (HeartbeatConnection) con;

        /**
         * If the connection is a heartbeat connection then the other end must
         * be setting up the connection without this end having requested it.
         * That means the other end must want it, so check the msgLink field for
         * this end is set - this is a sanity check.
         * 
         * *********************************************************************
         * 
         * The case can happen, so the above comment is incorrect. If the user
         * does a connect and then disconnect without sending a message, then
         * the other end could initiate a connection neither end needs in
         * response to the initial connect. Do not count this as an error, but
         * do log its occurance.
         */
        if (!hbmsg.getMsgLinks().contains(me.id)) {
            log.severe(me
                       + " VALID CASE - FOR INFORMATION ONLY:=> incoming connection from "
                       + con.getSender().toString()
                       + " when neither end wants the connection");
            // next two lines removed to allow this case
            // shutdown();
            // return;
        }

        /**
         * Now we are left with a valid heartbeat connection and the other end
         * is initiating a message connection, so create this end.
         * 
         * Note that the connection set only finds out about the newly created
         * message connection when it is informed by the call to
         * connectionSet.useNewMessageConnection(), so it can not terminate the
         * connection before the call to messageConnection.assignImpl(). Also,
         * we created the message connection, so we know it does not yet have an
         * impl. Hence we can assume it will succeed in assigning the impl.
         */
        messageConnection = new MessageConnection(me, connectionSet,
                                                  hbcon.getProtocol(),
                                                  hbcon.getCandidate());
        messageConnection.assignImpl(this);
        messageConnection.deliver(bytes);

        /**
         * if the call to connectionSet.useNewMessageConnection() then a
         * connection has been created since we checked for it above with
         * connectionSet.getConnection(). The other end will not make two
         * connection attempts at the same time, but if this thread is delayed
         * during the last 20 lines of code for long enough for the following to
         * happen: 1. other end time out connection + 2. quiesence period + 3.
         * this end rediscover other end in multicast heartbeats + 4. other end
         * initiates new connection attempt + 5. new connection attempt gets
         * accepted (new thread created for it) + 6. read first heartbeat and
         * get through this code in the new thread. Then it could beat this
         * thread to it. If all this happens (and based on the premise
         * "if it can happen it will happen") then this thread should rightly
         * comit suicide in disgust!!!!
         */
        if (!connectionSet.useNewMessageConnection(messageConnection)) {
            log.severe(me + "Concurrent creation of message connections from "
                       + messageConnection.getSender());
            shutdown();
            return;
        }
        toString = "Anubis: Message Handler (node " + me.id + ", remote node "
                   + messageConnection.getSender().id + ")";
    }

    private void logClose(String reason, Throwable throwable) {
        if (ignoring) {
            return;
        }
        if (messageConnection == null) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE,
                        me
                                + " shutdown unassigned message connection transport:"
                                + reason, throwable);
            }
        } else {
            messageConnection.logClose(reason, throwable);
        }
    }

    private void readHeader() {
        // Clear the buffer and read bytes 
        int numBytesRead;
        try {
            numBytesRead = channel.read(headerIn);
        } catch (IOException e) {
            logClose("Errror reading header", e);
            shutdown();
            return;
        }

        if (numBytesRead == -1) {
            readState = State.CLOSE;
            close();
        } else if (!headerIn.hasRemaining()) {
            headerIn.flip();
            if (headerIn.getInt(0) != MAGIC_NUMBER) {
                readState = State.ERROR;
                logClose("incorrect magic number in header", null);
                shutdown();
                return;
            }
            int length = headerIn.getInt(4);
            byte[] msg = new byte[length];
            msgIn = ByteBuffer.wrap(msg);
            readState = State.MESSAGE;
            readMessage();
        } else {
            handler.selectForRead(this);
        }
    }

    private void readMessage() {
        int numBytesRead;
        try {
            numBytesRead = channel.read(msgIn);
        } catch (IOException e) {
            readState = State.ERROR;
            logClose("Error reading message body", e);
            terminate();
            return;
        }
        if (numBytesRead == -1) {
            readState = State.CLOSE;
            close();
        } else if (msgIn.hasRemaining()) {
            handler.selectForRead(this);
        } else {
            byte[] msg = msgIn.array();
            msgIn = null;
            readState = State.INITIAL;
            deliver(msg);
            handler.selectForRead(this);
        }
    }

    private void send(byte[] bytes) {
        try {
            writeGate.acquire();
        } catch (InterruptedException e) {
            return;
        }
        writeState = State.INITIAL;
        headerOut.clear();
        headerOut.putInt(0, MAGIC_NUMBER);
        headerOut.putInt(4, bytes.length);
        msgOut = ByteBuffer.wrap(bytes);
        writeHeader();
    }

    private void shutdown() {
        open = false;
        closing();
        try {
            close();
        } catch (Exception ex) {
        }

    }

    private void writeHeader() {
        int bytesWritten;
        try {
            bytesWritten = channel.write(headerOut);
        } catch (IOException e) {
            writeState = State.ERROR;
            logClose("Unable to send message header", e);
            shutdown();
            return;
        }
        if (bytesWritten == -1) {
            writeState = State.CLOSE;
            close();
            return;
        } else if (headerOut.hasRemaining()) {
            writeState = State.HEADER;
            handler.selectForWrite(this);
        } else {
            writeState = State.MESSAGE;
            writeMessage();
        }
    }

    private void writeMessage() {
        int bytesWritten;
        try {
            bytesWritten = channel.write(msgOut);
        } catch (IOException e) {
            writeState = State.ERROR;
            logClose("Unable to send message body", e);
            shutdown();
            return;
        }
        if (bytesWritten == -1) {
            writeState = State.CLOSE;
            close();
        } else if (headerOut.hasRemaining()) {
            handler.selectForWrite(this);
        } else {
            writeState = State.INITIAL;
            msgOut = null;
            writeGate.release();
        }
    }
}
