package com.hellblazer.jackal.util;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.util.NodeIdSet;

public class NodeIdSetTest extends TestCase {
    public void testFirst() {
        NodeIdSet nv = new NodeIdSet();
        for (int i = 3; i < 100; i += 3) {
            nv.add(i);
        }
        assertEquals(3, nv.first());
    }

    public void testlast() {
        NodeIdSet nv = new NodeIdSet();
        for (int i = 0; i < 100; i += 3) {
            nv.add(i);
        }
        assertEquals(99, nv.last());
    }

    public void testNeighbors() {
        NodeIdSet nv = new NodeIdSet();
        for (int i = 0; i < 100; i += 3) {
            nv.add(i);
        }

        int[] neighbors = nv.neighborsOf(0);
        assertNotNull(neighbors);
        assertEquals(99, neighbors[0]);
        assertEquals(3, neighbors[1]);

        neighbors = nv.neighborsOf(99);
        assertNotNull(neighbors);
        assertEquals(96, neighbors[0]);
        assertEquals(0, neighbors[1]);

        neighbors = nv.neighborsOf(6);
        assertNotNull(neighbors);
        assertEquals(3, neighbors[0]);
        assertEquals(9, neighbors[1]);

    }
}
