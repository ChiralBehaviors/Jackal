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

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.hellblazer.anubis.util.HexDump;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
abstract public class AbstractCommunicationsHandler implements
        CommunicationsHandler {
    private static enum State {
        CLOSE, ERROR, HEADER, INITIAL, MESSAGE;
    }

    public static final int     HEADER_SIZE  = 8;
    public static final int     MAGIC_NUMBER = 24051967;
    private static final Logger log          = Logger.getLogger(AbstractCommunicationsHandler.class.getCanonicalName());

    private static String toHex(byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length * 4);
        PrintStream stream = new PrintStream(baos);
        HexDump.hexdump(stream, data, 0, data.length);
        stream.close();
        return baos.toString();
    }

    protected final ServerChannelHandler handler;
    private final SocketChannel          channel;
    private ByteBuffer                   headerIn   = ByteBuffer.wrap(new byte[HEADER_SIZE]);
    private ByteBuffer                   headerOut  = ByteBuffer.wrap(new byte[HEADER_SIZE]);
    private ByteBuffer                   msgIn;
    private ByteBuffer                   msgOut;
    private volatile boolean             open       = true;
    private State                        readState  = State.INITIAL;
    private final Semaphore              writeGate;
    private State                        writeState = State.INITIAL;
    private final ReentrantLock          readLock   = new ReentrantLock();
    private final ReentrantLock          writeLock  = new ReentrantLock();

    public AbstractCommunicationsHandler(ServerChannelHandler handler,
                                         SocketChannel channel) {
        this.handler = handler;
        this.channel = channel;
        writeGate = new Semaphore(1, true);
    }

    @Override
    public void close() {
        if (log.isLoggable(Level.FINE)) {
            Exception e = new Exception("Socket close trace");
            log.log(Level.FINE,
                    String.format("Closing connection to %s", channel), e);
        }
        readState = writeState = State.CLOSE;
        open = false;
        try {
            channel.close();
        } catch (IOException e) {
        }
        handler.closeHandler(this);
    }

    public boolean connected() {
        return open;
    }

    @Override
    public SocketChannel getChannel() {
        return channel;
    }

    @Override
    public synchronized void handleAccept() {
        readState = State.INITIAL;
        writeState = State.INITIAL;
        selectForRead();
    }

    @Override
    public synchronized void handleRead() {
        final ReentrantLock myReadLock = readLock;
        try {
            myReadLock.lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }
        try {
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
        } finally {
            myReadLock.unlock();
        }
    }

    @Override
    public void handleWrite() {
        final ReentrantLock myWriteLock = writeLock;
        try {
            myWriteLock.lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }
        try {
            switch (writeState) {
                case INITIAL: {
                    throw new IllegalStateException(
                                                    "Should never be initial state");
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
        } finally {
            myWriteLock.unlock();
        }
    }

    public void shutdown() {
        open = false;
        closing();
        try {
            close();
        } catch (Exception ex) {
        }
    }

    abstract protected void closing();

    abstract protected void deliver(byte[] msg);

    protected void selectForRead() {
        handler.selectForRead(this);
    }

    protected void selectForWrite() {
        handler.selectForWrite(this);
    }

    protected void send(byte[] bytes) {
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
        if (log.isLoggable(Level.FINEST)) {
            log.finest(format("Sending message: \n%s", toHex(bytes)));
        }
        writeHeader();
    }

    private void readHeader() {
        // Clear the buffer and read bytes 
        int numBytesRead;
        try {
            numBytesRead = channel.read(headerIn);
        } catch (ClosedChannelException e) {
            writeState = State.CLOSE;
            readState = State.CLOSE;
            shutdown();
            return;
        } catch (IOException e) {
            readState = State.ERROR;
            writeState = State.CLOSE;
            if (isNotClosed(e) && log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING, "Errror reading header", e);
            }
            shutdown();
            return;
        }

        if (numBytesRead == -1) {
            readState = State.CLOSE;
            shutdown();
        } else if (!headerIn.hasRemaining()) {
            headerIn.flip();
            if (headerIn.getInt(0) != MAGIC_NUMBER) {
                readState = State.ERROR;
                if (log.isLoggable(Level.WARNING)) {
                    log.log(Level.WARNING, "incorrect magic number in header");
                }
                shutdown();
                return;
            }
            int length = headerIn.getInt(4);
            byte[] msg = new byte[length];
            msgIn = ByteBuffer.wrap(msg);
            readState = State.MESSAGE;
            readMessage();
        } else {
            selectForRead();
        }
    }

    private void readMessage() {
        int numBytesRead;
        try {
            numBytesRead = channel.read(msgIn);
        } catch (ClosedChannelException e) {
            writeState = State.CLOSE;
            readState = State.CLOSE;
            shutdown();
            return;
        } catch (IOException e) {
            readState = State.ERROR;
            writeState = State.CLOSE;
            if (isNotClosed(e) && log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING, "Error reading message body", e);
            }
            shutdown();
            return;
        }
        if (numBytesRead == -1) {
            readState = State.CLOSE;
            shutdown();
        } else if (msgIn.hasRemaining()) {
            selectForRead();
        } else {
            byte[] msg = msgIn.array();
            readState = State.INITIAL;
            msgIn = null;
            if (log.isLoggable(Level.FINEST)) {
                log.finest(format("delivering message: \n%s", toHex(msg)));
            }
            deliver(msg);
            selectForRead();
        }
    }

    private void writeHeader() {
        int bytesWritten;
        try {
            bytesWritten = channel.write(headerOut);
        } catch (ClosedChannelException e) {
            writeState = State.CLOSE;
            shutdown();
            return;
        } catch (IOException e) {
            writeState = State.ERROR;
            if (isNotClosed(e) && log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING, "Unable to send message header", e);
            }
            shutdown();
            return;
        }
        if (bytesWritten == -1) {
            writeState = State.CLOSE;
            shutdown();
        } else if (headerOut.hasRemaining()) {
            writeState = State.HEADER;
            selectForWrite();
        } else {
            writeState = State.MESSAGE;
            writeMessage();
        }
    }

    private void writeMessage() {
        int bytesWritten;
        try {
            bytesWritten = channel.write(msgOut);
        } catch (ClosedChannelException e) {
            writeState = State.CLOSE;
            shutdown();
            return;
        } catch (IOException e) {
            writeState = State.ERROR;
            if (isNotClosed(e) && log.isLoggable(Level.WARNING)) {
                log.log(Level.WARNING, "Unable to send message body", e);
            }
            shutdown();
            return;
        }
        if (bytesWritten == -1) {
            writeState = State.CLOSE;
            shutdown();
        } else if (headerOut.hasRemaining()) {
            selectForWrite();
        } else {
            writeState = State.INITIAL;
            msgOut = null;
            writeGate.release();
        }
    }

    protected boolean isNotClosed(IOException e) {
        String message = e.getMessage();
        return !"broken pipe".equalsIgnoreCase(message)
               && !"connection reset by peer".equalsIgnoreCase(message);
    }

    @Override
    public String toString() {
        Socket socket = channel.socket();
        return "Handler for " + "[local=" + socket.getLocalSocketAddress()
               + ", remote=" + socket.getRemoteSocketAddress() + "]";
    }
}