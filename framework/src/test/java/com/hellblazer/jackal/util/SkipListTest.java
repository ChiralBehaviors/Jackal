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

import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

public class SkipListTest extends TestCase {
    public void testCounting() {
        SkipList list = new SkipList();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }

        assertEquals(6, list.countLessThanEqualTo(5.0));
    }

    public void testDuplicates() {
        SkipList list = new SkipList();
        for (int i = 0; i < 10; i++) {
            list.add(i);
        }
        assertEquals(10, list.size());

        assertTrue(list.add(5));

        assertEquals(11, list.size());

        list.remove(Integer.valueOf(5));

        assertEquals(10, list.size());

        assertTrue(list.contains(Integer.valueOf(5)));

        list.remove(Integer.valueOf(5));

        assertEquals(9, list.size());

        assertFalse(list.contains(Integer.valueOf(5)));
    }

    public void testLargeRandom() {
        Random r = new Random(666);
        SkipList list = new SkipList();
        int count = 10000;
        double[] original = new double[count];
        double[] sorted = new double[count];
        for (int i = 0; i < count; i++) {
            double data = r.nextLong();
            sorted[i] = data;
            original[i] = data;
            list.add(data);
        }
        Arrays.sort(sorted);
        assertEquals(count, list.size());
        int i = 0;
        for (double d : sorted) {
            assertEquals(d, list.get(i++));
        }
        for (i = 0; i < count; i++) {
            assertTrue(list.remove(original[i]));
            assertEquals(count - (i + 1), list.size());
        }
    }
}
