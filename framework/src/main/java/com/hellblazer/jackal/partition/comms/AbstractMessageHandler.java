/** 
 * (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.jackal.partition.comms;

import static java.lang.String.format;
import static org.smartfrog.services.anubis.partition.wire.WireSizes.MAGIC_NUMBER;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.jackal.partition.comms.MessageHandler.State;
import com.hellblazer.jackal.util.ByteBufferPool;
import com.hellblazer.jackal.util.HexDump;
import com.hellblazer.pinkie.CommunicationsHandler;
import com.hellblazer.pinkie.SocketChannelHandler;

/**
 * 
 * @author hhildebrand
 * 
 */
public abstract class AbstractMessageHandler implements CommunicationsHandler {
    protected static String toHex(byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length * 4);
        PrintStream stream = new PrintStream(baos);
        HexDump.hexdump(stream, data, 0, data.length);
        stream.close();
        return baos.toString();
    }

    private final AtomicBoolean               closed     = new AtomicBoolean();
    private volatile ByteBuffer               currentWrite;
    private ByteBuffer                        readBuffer;
    private final ByteBuffer                  rxHeader   = ByteBuffer.allocate(16);
    private final ReentrantLock               writeLock  = new ReentrantLock();
    private final ByteBuffer                  wxHeader   = ByteBuffer.allocate(16);
    protected final ByteBufferPool            bufferPool = new ByteBufferPool(
                                                                              "Abstract Message Handler",
                                                                              100);
    protected volatile SocketChannelHandler   handler;
    protected volatile State                  readState  = State.HEADER;
    protected final WireSecurity              wireSecurity;
    protected final BlockingQueue<ByteBuffer> writes     = new ArrayBlockingQueue<ByteBuffer>(
                                                                                              4);
    protected volatile State                  writeState = State.INITIAL;

    public AbstractMessageHandler(WireSecurity wireSecurity) {
        this.wireSecurity = wireSecurity;
    }

    @Override
    public void readReady() {
        if (getLog().isTraceEnabled()) {
            getLog().trace(format("Socket read ready [%s]", this));
        }
        switch (readState) {
            case ERROR:
                return;
            case CLOSED:
                return;
            case HEADER: {
                if (!read(rxHeader)) {
                    return;
                }
                if (rxHeader.hasRemaining()) {
                    break;
                }
                rxHeader.flip();
                int readMagic = rxHeader.getInt();
                if (getLog().isTraceEnabled()) {
                    getLog().trace("Read magic number: " + readMagic);
                }
                if (readMagic == MAGIC_NUMBER) {
                    if (getLog().isTraceEnabled()) {
                        getLog().trace("RxHeader magic-number fits");
                    }
                    // get the object size and create a new buffer for it
                    int objectSize = rxHeader.getInt();
                    if (getLog().isTraceEnabled()) {
                        getLog().trace("read objectSize: " + objectSize);
                    }
                    readBuffer = bufferPool.allocate(objectSize);
                    readState = State.BODY;
                } else {
                    getLog().error(String.format("invalid magic number %s, required %s"),
                                   readMagic, MAGIC_NUMBER);
                    readState = State.ERROR;
                    shutdown();
                    return;
                }
                // Fall through to BODY state intended.
            }
            case BODY: {
                if (!read(readBuffer)) {
                    return;
                }
                if (!readBuffer.hasRemaining()) {
                    long order = rxHeader.getLong();
                    rxHeader.clear();
                    readBuffer.flip();
                    deliverObject(order, readBuffer);
                    bufferPool.free(readBuffer);
                    readBuffer = null;
                    readState = State.HEADER;
                }
                break;
            }
            default: {
                throw new IllegalStateException("Illegal read state "
                                                + readState);
            }
        }

        handler.selectForRead();
    }

    public void shutdown() {
        closed.set(true);
        writes.clear();
        writeState = readState = State.CLOSED;
        handler.close();
        getLog().info(bufferPool.toString());
    }

    @Override
    public void writeReady() {
        ReentrantLock myLock = writeLock;
        assert !myLock.isHeldByCurrentThread();
        if (!myLock.tryLock()) {
            return;
        }
        try {
            if (getLog().isTraceEnabled()) {
                getLog().trace(format("Socket write ready [%s]", this));
            }
            while (true) {
                switch (writeState) {
                    case ERROR:
                        return;
                    case CLOSED:
                        return;
                    case INITIAL: {
                        currentWrite = writes.poll();
                        if (currentWrite == null) {
                            return;
                        }
                        wxHeader.clear();
                        wxHeader.putInt(0, MAGIC_NUMBER);
                        wxHeader.putInt(4, currentWrite.remaining());
                        wxHeader.putLong(8, nextSequence());
                        writeState = State.HEADER;
                    }
                    case HEADER: {
                        if (!write(wxHeader)) {
                            return;
                        }
                        if (wxHeader.hasRemaining()) {
                            handler.selectForWrite();
                            return;
                        }
                        writeState = State.BODY;
                        // fallthrough to body intentional
                    }
                    case BODY: {
                        if (!write(currentWrite)) {
                            return;
                        }
                        if (!currentWrite.hasRemaining()) {
                            writeState = State.INITIAL;
                            if (writes.isEmpty()) {
                                return;
                            }
                        } else {
                            bufferPool.free(currentWrite);
                            handler.selectForWrite();
                            return;
                        }
                        break;
                    }
                    default: {
                        throw new IllegalStateException("Illegal write state: "
                                                        + writeState);
                    }
                }
            }
        } finally {
            myLock.unlock();
        }
    }

    private boolean isClose(IOException ioe) {
        return "Broken pipe".equals(ioe.getMessage())
               || "Connection reset by peer".equals(ioe.getMessage());
    }

    private boolean read(ByteBuffer buffer) {
        try {
            int read = handler.getChannel().read(buffer);
            if (read < 0) {
                writeState = readState = State.CLOSED;
                shutdown();
                return false;
            }
        } catch (IOException ioe) {
            if (getLog().isTraceEnabled()) {
                getLog().trace(format("Failed to read socket channel [%s]",
                                      this), ioe);
            }
            error();
            return false;
        } catch (NotYetConnectedException nycex) {
            if (getLog().isWarnEnabled()) {
                getLog().warn("Attempt to read a socket channel before it is connected",
                              nycex);
            }
            error();
            return false;
        }
        return true;
    }

    private boolean write(ByteBuffer buffer) {
        try {
            if (handler.getChannel().write(buffer) < 0) {
                close();
                return false;
            }
        } catch (ClosedChannelException e) {
            if (getLog().isTraceEnabled()) {
                getLog().trace(format("shutting down handler due to other side closing [%s]",
                                      this), e);
            }
            error();
            return false;
        } catch (IOException ioe) {
            if (getLog().isWarnEnabled() && !isClose(ioe)) {
                getLog().warn("shutting down handler", ioe);
            }
            error();
            return false;
        }
        return true;
    }

    protected void close() {
        closed.set(true);
        writes.clear();
        writeState = readState = State.CLOSED;
        handler.close();
    }

    abstract protected void deliverObject(long order, ByteBuffer readBuffer);

    protected void error() {
        writeState = readState = State.ERROR;
        shutdown();
    }

    abstract protected Logger getLog();

    /**
     * @return
     */
    protected long nextSequence() {
        return 0L; // default
    }

    protected void sendObject(ByteBuffer buffer) {
        if (closed.get()) {
            if (getLog().isInfoEnabled()) {
                getLog().trace(format("handler is closed, ignoring send on [%s]",
                                      this));
            }
            return;
        }
        if (getLog().isTraceEnabled()) {
            getLog().trace(format("sendObject being called [%s]", this));
        }
        try {
            writes.put(buffer);
        } catch (InterruptedException e) {
            return;
        }
        handler.selectForWrite();
    }

}