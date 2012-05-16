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

import org.junit.Test;
import static junit.framework.Assert.*;

/**
 * @author hhildebrand
 * 
 */
public class ByteBufferOutputStreamTest {
    @Test
    public void testGrow() {
        ByteBufferPool pool = new ByteBufferPool("test", 100);
        ByteBufferOutputStream test = new ByteBufferOutputStream(pool);
        for (int i = 0; i < 1024 * 1024; i++) {
            test.write(5);
        }
        ByteBuffer produced = test.toByteBuffer();
        assertEquals("Invalid buffer limit", 1024 * 1024, produced.limit());
        assertEquals("Invalid capacity", 1048576, produced.capacity());
        assertEquals("Invalid bytes allocated", 2097120,
                     pool.getBytesAllocated());
        assertEquals("Invalid buffer allocations", 16, pool.getCreated());
    }
}
