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

import static junit.framework.Assert.assertEquals;

import java.nio.ByteBuffer;

import org.junit.Test;

/**
 * @author hhildebrand
 * 
 */
public class ByteBufferPoolTest {

    @Test
    public void testFree() {
        ByteBufferPool test = new ByteBufferPool("test", 100);
        for (int i = 0; i < 100; i++) {
            test.free(ByteBuffer.allocate(i));
        }
        assertEquals(0, test.getCreated());
        assertEquals(100, test.getPooled());

        test.free(ByteBuffer.allocate(100));

        assertEquals(100, test.getPooled());
        assertEquals(100, test.size());
        assertEquals(1, test.getDiscarded());

        for (int i = 0; i < 100; i++) {
            assertEquals(i, test.allocate(i).capacity());
        }

        assertEquals(0, test.size());
        assertEquals(0, test.getCreated());

        assertEquals(100, test.allocate(100).capacity());

        assertEquals(1, test.getCreated());
        assertEquals(0, test.size());
    }

    @Test
    public void testFreeMatch() {
        ByteBufferPool test = new ByteBufferPool("test", 100);
        for (int i = 0; i < 100; i++) {
            test.free(ByteBuffer.allocate(i));
        }
        assertEquals(0, test.getCreated());
        assertEquals(100, test.getPooled());

        test.free(ByteBuffer.allocate(100));

        assertEquals(100, test.getPooled());
        assertEquals(100, test.size());
        assertEquals(1, test.getDiscarded());

        for (int i = 99; i >= 0; i--) {
            assertEquals(i, test.allocate(i).capacity());
        }

        assertEquals(0, test.size());
        assertEquals(0, test.getCreated());

        assertEquals(100, test.allocate(100).capacity());

        assertEquals(1, test.getCreated());
        assertEquals(0, test.size());
    }
}
