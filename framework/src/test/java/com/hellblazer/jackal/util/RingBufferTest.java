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
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/**
 * @author hhildebrand
 * 
 */
public class RingBufferTest {
    @Test
    public void testOffer() {
        RingBuffer<String> test = new RingBuffer<String>(1000);
        for (int i = 0; i < 1000; i++) {
            assertEquals("Invalid size", i, test.size());
            assertTrue(test.offer(String.format("Offer: %s", i)));
        }
        assertEquals("Invalid size", 1000, test.size());
        assertFalse((test.offer(String.format("Offer: %s", 1001))));
    }

    @Test
    public void testPoll() {
        RingBuffer<String> test = new RingBuffer<String>(1000);
        for (int i = 0; i < 1000; i++) {
            test.add(String.format("Add: %s", i));
        }
        for (int i = 0; i < 1000; i++) {
            assertEquals("Invalid size", 1000 - i, test.size());
            assertEquals(String.format("Add: %s", i), test.poll());
        }
        assertNull(test.poll());
    }

    @Test
    public void testAccordian() {
        Random r = new Random(0x666);
        RingBuffer<Integer> test = new RingBuffer<Integer>(1000);
        for (int i = 0; i < 500; i++) {
            test.add(i);
        }
        int count = test.size();
        for (int i = 0; i < 10000; i++) {
            if (r.nextBoolean()) {
                assertTrue(test.offer(i));
                count++;
                assertEquals(count, test.size());
            } else {
                assertNotNull(test.poll());
                count--;
                assertEquals(count, test.size());
            }
        }
        assertEquals(count, test.size());
    }

    @Test
    public void testIteration() {
        RingBuffer<String> test = new RingBuffer<String>(1000);
        for (int i = 0; i < 1000; i++) {
            test.add(String.format("Offer: %s", i));
        }
        int i = 0;
        for (String element : test) {
            assertEquals(String.format("Offer: %s", i++), element);
        }
    }

    @Test
    public void testPollOrder() {
        RingBuffer<String> test = new RingBuffer<String>(1000);
        for (int i = 0; i < 1000; i++) {
            test.add(String.format("Offer: %s", i));
        }
        for (int i = 0; i < 1000; i++) {
            String element = test.poll();
            assertEquals(String.format("Offer: %s", i), element);
        }
    }
}
