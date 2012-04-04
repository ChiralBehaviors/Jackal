package com.hellblazer.jackal.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.views.BitView;

public class BitViewTest extends TestCase {
    public void testIterator() {
        BitView bv = new BitView();
        for (int i = 0; i < 10; i++) {
            bv.add(i);
        }

        Set<Integer> bag = new HashSet<Integer>();
        for (int i : bv) {
            bag.add(i);
        }
        assertEquals(10, bag.size());

        for (int i = 0; i < 10; i++) {
            assertTrue(bag.contains(i));
        }

        Iterator<Integer> iterator = bv.iterator();
        for (int i = 0; i < 10; i++) {
            iterator.next();
        }
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail("Expected no such element!");
        } catch (NoSuchElementException e) {
            // expected
        }
    }
}
