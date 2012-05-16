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
package org.smartfrog.services.anubis.partition.wire.msg;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import com.hellblazer.jackal.util.ByteBufferPool;

/**
 * @author hhildebrand
 * 
 */
public class ByteBufferOutputStream extends OutputStream {

    private final ByteBufferPool bufferPool;
    private ByteBuffer           buffer;

    /**
     * @param bp
     */
    public ByteBufferOutputStream(ByteBufferPool bp) {
        this(bp, 32);
    }

    /**
     * @param bp
     */
    public ByteBufferOutputStream(ByteBufferPool bp, int initialSize) {
        bufferPool = bp;
        buffer = bufferPool.allocate(initialSize);
    }

    public ByteBuffer toByteBuffer() {
        buffer.limit(buffer.position());
        return buffer;
    }

    /**
     * Increases the capacity if necessary to ensure that it can hold at least
     * the number of elements specified by the minimum capacity argument.
     * 
     * @param minCapacity
     *            the desired minimum capacity
     * @throws OutOfMemoryError
     *             if {@code minCapacity < 0}. This is interpreted as a request
     *             for the unsatisfiably large capacity
     *             {@code (long) Integer.MAX_VALUE + (minCapacity - Integer.MAX_VALUE)}
     *             .
     */
    private void ensureCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buffer.remaining() > 0) {
            try {
                grow(minCapacity);
            } catch (OutOfMemoryError e) {
                System.out.println(String.format("Attempted to grow stream to % bytes",
                                                 minCapacity));
                throw e;
            }
        }
        assert buffer.capacity() >= minCapacity : String.format("Need: %s, required %s more bytes ",
                                                                minCapacity,
                                                                minCapacity
                                                                        - buffer.remaining());
    }

    /**
     * Increases the capacity to ensure that it can hold at least the number of
     * elements specified by the minimum capacity argument.
     * 
     * @param minCapacity
     *            the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        int oldCapacity = buffer.capacity();
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity < 0) {
            if (minCapacity < 0) // overflow
                throw new OutOfMemoryError();
            newCapacity = Integer.MAX_VALUE;
        }
        assert newCapacity >= minCapacity : "Math is hard";
        int position = buffer.position();
        ByteBuffer oldBuffer = buffer;
        buffer = bufferPool.allocate(newCapacity);
        buffer.put(oldBuffer.array(), 0, position);
        bufferPool.free(oldBuffer);
        assert buffer.capacity() >= minCapacity : "Math is hard";
    }

    /**
     * Writes the specified byte to this byte buffer output stream.
     * 
     * @param b
     *            the byte to be written.
     */
    public void write(int b) {
        ensureCapacity(buffer.position() + 1);
        buffer.put((byte) b);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte buffer starting at
     * offset <code>off</code> to this byte buffer output stream.
     * 
     * @param b
     *            the data.
     * @param off
     *            the start offset in the data.
     * @param len
     *            the number of bytes to write.
     */
    public void write(byte b[], int off, int len) {
        if ((off < 0) || (off > b.length) || (len < 0)
            || ((off + len) - b.length > 0)) {
            throw new IndexOutOfBoundsException();
        }
        ensureCapacity(buffer.position() + len);
        buffer.put(b, off, len);
    }

    /**
     * Resets the <code>count</code> field of this byte buffer output stream to
     * zero, so that all currently accumulated output in the output stream is
     * discarded. The output stream can be used again, reusing the already
     * allocated buffer space.
     * 
     * @see java.io.ByteArrayInputStream#count
     */
    public void reset() {
        buffer.rewind();
    }

    /**
     * Returns the current size of the buffer.
     * 
     * @return the value of the <code>count</code> field, which is the number of
     *         valid bytes in this output stream.
     * @see java.io.ByteArrayOutputStream#count
     */
    public int size() {
        return buffer.position();
    }

    /**
     * Closing a <tt>ByteBufferOutputStream</tt> has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an <tt>IOException</tt>.
     * <p>
     * 
     */
    public void close() throws IOException {
    }
}
