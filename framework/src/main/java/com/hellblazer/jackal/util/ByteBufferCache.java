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
package com.hellblazer.jackal.util;

import java.nio.ByteBuffer;

/**
 * Provides a byte buffer cache. Not thread safe, intended to be put into thread
 * local.
 * 
 * @author hhildebrand
 * 
 */
public class ByteBufferCache {
    public static final ThreadLocal<ByteBufferCache> BUFFER_CACHE = new ThreadLocal<ByteBufferCache>() {
                                                                      @Override
                                                                      protected ByteBufferCache initialValue() {
                                                                          return new ByteBufferCache();
                                                                      }
                                                                  };
    private final ByteBuffer[]                       buffers;
    private int                                      count        = 0;
    private int                                      start        = 0;

    public ByteBufferCache() {
        buffers = new ByteBuffer[8];
    }

    public ByteBuffer get(int capacity) {
        if (count == 0) {
            return ByteBuffer.allocate(capacity);
        }
        ByteBuffer buffer = buffers[start];
        if (buffer.capacity() < capacity) {
            buffer = null;
            int i = start;
            while ((i = (i + 1) % 8) != start) {
                ByteBuffer candidate = buffers[i];
                if (candidate == null)
                    break;
                if (candidate.capacity() >= capacity) {
                    buffer = candidate;
                    break;
                }
            }
            if (buffer == null) {
                if (!isEmpty()) {
                    removeFirst();
                }
                return ByteBuffer.allocate(capacity);
            }
            buffers[i] = buffers[start];
        }

        buffers[start] = null;
        start = (start + 1) % 8;
        count -= 1;

        buffer.rewind();
        buffer.limit(capacity);
        return buffer;
    }

    private boolean isEmpty() {
        return (count == 0);
    }

    public void recycle(ByteBuffer buffer) {
        buffer.clear();
        if (count < 8) {
            int i = (start + count) % 8;
            buffers[i] = buffer;
            count += 1;
        }
    }

    private ByteBuffer removeFirst() {
        assert (count > 0);
        ByteBuffer buffer = buffers[start];
        buffers[start] = null;
        start = (start + 1) % 8;
        count -= 1;
        return buffer;
    }
}
