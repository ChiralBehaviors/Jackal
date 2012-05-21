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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

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

    static enum State {
        BODY, CLOSED, ERROR, HEADER, INITIAL;
    }

    protected static final int READ_BUFFER_SIZE = 64 * 1024;
    protected static final int HEADER_BYTE_SIZE = 16;

    protected static String toHex(byte[] data, int length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(length * 4);
        PrintStream stream = new PrintStream(baos);
        HexDump.hexdump(stream, data, 0, length);
        stream.close();
        return baos.toString();
    }

    private final AtomicBoolean               closed     = new AtomicBoolean();
    private volatile ByteBuffer[]             currentWrite;
    private final List<ByteBuffer>            drain      = new ArrayList<ByteBuffer>();
    private volatile ByteBuffer               readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
    private final ReentrantLock               writeLock  = new ReentrantLock();
    protected final ByteBufferPool            bufferPool = new ByteBufferPool(
                                                                              "Abstract Message Handler",
                                                                              100);
    protected volatile SocketChannelHandler   handler;
    protected volatile State                  readState  = State.INITIAL;
    protected final WireSecurity              wireSecurity;
    protected final BlockingDeque<ByteBuffer> writes     = new LinkedBlockingDeque<ByteBuffer>();
    protected volatile State                  writeState = State.INITIAL;

    public AbstractMessageHandler(WireSecurity wireSecurity) {
        this.wireSecurity = wireSecurity;
    }

    @Override
    public void readReady() {
        if (getLog().isTraceEnabled()) {
            getLog().trace(format("Socket read ready [%s]", this));
        }
        while (true) {
            switch (readState) {
                case ERROR:
                case CLOSED:
                    return;
                case INITIAL: {
                    if (!read(readBuffer)) {
                        return;
                    }
                    if (readBuffer.position() < HEADER_BYTE_SIZE) {
                        if (getLog().isTraceEnabled()) {
                            getLog().trace(format("Not enough bytes for a header"));
                        }
                        handler.selectForRead();
                        return;
                    }
                    if (getLog().isTraceEnabled()) {
                        getLog().trace(format("enough bytes for a header"));
                    }
                    readBuffer.flip();
                    readState = State.HEADER;
                    // Fall through to HEADER state intended.
                }
                case HEADER: {
                    int magic = readBuffer.getInt(0);
                    if (magic == MAGIC_NUMBER) {
                        int objectSize = readBuffer.getInt(4);
                        if (readBuffer.remaining() >= objectSize
                                                      + HEADER_BYTE_SIZE) {
                            if (getLog().isTraceEnabled()) {
                                getLog().trace(format("enough bytes for the object"));
                            }
                            long order = readBuffer.getLong(8);
                            readBuffer.position(HEADER_BYTE_SIZE);
                            ByteBuffer msgBuffer = readBuffer.slice();
                            msgBuffer.limit(objectSize);
                            readBuffer.position(objectSize + HEADER_BYTE_SIZE);
                            deliverObject(order, msgBuffer);
                            readBuffer.compact();
                            readBuffer.flip();
                            if (readBuffer.remaining() >= HEADER_BYTE_SIZE) {
                                if (getLog().isTraceEnabled()) {
                                    getLog().trace(format("enough bytes for another header"));
                                }
                                break;
                            }
                            readBuffer.compact();
                        } else {
                            if (objectSize > readBuffer.capacity()) {
                                if (getLog().isTraceEnabled()) {
                                    getLog().trace(format("Growing read buffer to %s",
                                                          objectSize
                                                                  + HEADER_BYTE_SIZE));
                                }
                                ByteBuffer grow = ByteBuffer.allocate(objectSize
                                                                      + HEADER_BYTE_SIZE);
                                grow.put(readBuffer);
                                readBuffer = grow;
                            } else {
                                readBuffer.compact();
                            }
                            if (getLog().isTraceEnabled()) {
                                getLog().trace(format("not enough bytes for the object"));
                            }
                        }
                        readState = State.BODY;
                        handler.selectForRead();
                        return;
                    } else {
                        getLog().error(String.format("invalid magic number %s, required %s",
                                                     magic, MAGIC_NUMBER));
                        readState = State.ERROR;
                        shutdown();
                        return;
                    }
                }
                case BODY: {
                    if (!read(readBuffer)) {
                        return;
                    }
                    if (readBuffer.position() >= readBuffer.getInt(4)
                                                 + HEADER_BYTE_SIZE) {
                        if (getLog().isTraceEnabled()) {
                            getLog().trace(format("now enough bytes for the object"));
                        }
                        readState = State.HEADER;
                        readBuffer.flip();
                        break;
                    }
                    handler.selectForRead();
                    return;
                }
                default: {
                    throw new IllegalStateException("Illegal read state "
                                                    + readState);
                }
            }
        }
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
        if (getLog().isTraceEnabled()) {
            getLog().trace(format("Socket write ready [%s]", this));
        }
        ReentrantLock myLock = writeLock;
        assert !myLock.isHeldByCurrentThread();
        if (!myLock.tryLock()) {
            return;
        }
        try {
            if (getLog().isTraceEnabled()) {
                getLog().trace(format("Socket write ready [%s]", this));
            }
            switch (writeState) {
                case ERROR:
                case CLOSED:
                    return;
                case INITIAL: {
                    writes.drainTo(drain);
                    if (drain.isEmpty()) {
                        return;
                    }
                    ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>(
                                                                              drain.size() * 2);
                    int totalBytes = 0;
                    for (ByteBuffer msg : drain) {
                        ByteBuffer header = bufferPool.allocate(HEADER_BYTE_SIZE);
                        header.putInt(MAGIC_NUMBER);
                        header.putInt(msg.remaining());
                        header.putLong(nextSequence());
                        header.flip();
                        buffers.add(header);
                        buffers.add(msg);
                        totalBytes += header.remaining() + msg.remaining();
                    }
                    if (getLog().isTraceEnabled()) {
                        getLog().trace(format("Writing %s objects, total bytes: %s",
                                              drain.size(), totalBytes));
                    }
                    drain.clear();
                    currentWrite = buffers.toArray(new ByteBuffer[buffers.size()]);
                    writeState = State.BODY;
                    // fallthrough to body intentional
                }
                case BODY: {
                    if (!write(currentWrite)) {
                        return;
                    }
                    if (!currentWrite[currentWrite.length - 1].hasRemaining()) {
                        if (getLog().isTraceEnabled()) {
                            getLog().trace(format("All objects written"));
                        }
                        writeState = State.INITIAL;
                        for (ByteBuffer b : currentWrite) {
                            bufferPool.free(b);
                        }
                    } else {
                        if (getLog().isTraceEnabled()) {
                            getLog().trace(format("still more bytes to write"));
                        }
                        handler.selectForWrite();
                    }
                    return;
                }
                default: {
                    throw new IllegalStateException("Illegal write state: "
                                                    + writeState);
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
            if (getLog().isTraceEnabled()) {
                getLog().trace(format("1st read %s bytes", read));
            }
            if (read < 0) {
                writeState = readState = State.CLOSED;
                shutdown();
                return false;
            } else {
                // Try one more read ;)
                if (buffer.hasRemaining()) {
                    int plusRead = handler.getChannel().read(buffer);
                    if (plusRead < 0) {
                        writeState = readState = State.CLOSED;
                        shutdown();
                        return false;
                    }
                    if (getLog().isTraceEnabled()) {
                        getLog().trace(format("+ read %s bytes", plusRead));
                    }
                }
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

    private boolean write(ByteBuffer[] buffers) {
        try {
            long written = handler.getChannel().write(buffers);
            if (getLog().isTraceEnabled()) {
                getLog().trace(format("%s bytes written", written));
            }
            if (written < 0) {
                close();
                return false;
            } else if (buffers[buffers.length - 1].hasRemaining()) {
                long plusWritten = handler.getChannel().write(buffers);
                if (plusWritten < 0) {
                    close();
                    return false;
                }
                if (getLog().isTraceEnabled()) {
                    getLog().trace(format("%s bytes +written", plusWritten));
                }
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
        if (getLog().isTraceEnabled()) {
            getLog().trace(format("sending buffer"));
        }
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