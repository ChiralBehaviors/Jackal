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
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread safe pool for byte buffers
 * 
 * @author hhildebrand
 * 
 */
public class ByteBufferPool {

    private final ReentrantLock          lock      = new ReentrantLock();
    private int                          created   = 0;
    private int                          discarded = 0;
    private final String                 name;
    private final RingBuffer<ByteBuffer> pool;
    private int                          pooled    = 0;
    private int                          reused    = 0;

    public ByteBufferPool(String name, int limit) {
        this.name = name;
        pool = new RingBuffer<ByteBuffer>(limit);
    }

    public ByteBuffer allocate(int capacity) {
        final ReentrantLock myLock = lock;
        myLock.lock();
        try {
            if (pool.isEmpty()) {
                created++;
                return ByteBuffer.allocate(capacity);
            }
            int remaining = pool.size();
            while (remaining != 0) {
                ByteBuffer allocated = pool.poll();
                if (allocated.capacity() >= capacity) {
                    reused++;
                    allocated.rewind();
                    allocated.limit(capacity);
                    return allocated;
                }
                pool.add(allocated);
                remaining--;
            }
            created++;
            return ByteBuffer.allocate(capacity);
        } finally {
            myLock.unlock();
        }
    }

    public void free(ByteBuffer free) {
        final ReentrantLock myLock = lock;
        myLock.lock();
        try {
            if (!pool.offer(free)) {
                discarded++;
            } else {
                free.clear();
                pooled++;
            }
        } finally {
            myLock.unlock();
        }
    }

    /**
     * @return the created
     */
    public int getCreated() {
        return created;
    }

    /**
     * @return the discarded
     */
    public int getDiscarded() {
        return discarded;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    public int getPooled() {
        return pooled;
    }

    public int size() {
        return pool.size();
    }

    @Override
    public String toString() {
        return String.format("Pool[%s] size: %s reused: %s created: %s pooled: %s discarded: %s",
                             name, size(), reused, created, pooled, discarded);
    }
}
