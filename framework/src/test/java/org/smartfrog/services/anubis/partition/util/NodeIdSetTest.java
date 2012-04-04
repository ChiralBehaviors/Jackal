/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

 */
package org.smartfrog.services.anubis.partition.util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class NodeIdSetTest extends TestCase {

    public void testBasic() {
        int max = Identity.MAX_ID;
        Random random = new Random(666);
        NodeIdSet bs = new NodeIdSet();
        Set<Integer> entered = new HashSet<Integer>();
        bs.add(max);
        entered.add(max);
        bs.add(0);
        entered.add(0);
        for (int i = 0; i < 1000;) {
            int candidate = random.nextInt(max);
            if (entered.add(candidate)) {
                bs.add(candidate);
                i++;
            }
        }
        assertEquals(entered.size(), bs.cardinality());
        assertEquals(HeartbeatMsg.MAX_BIT_SIZE * 8, bs.size());
        for (Iterator<Integer> stream = entered.iterator(); stream.hasNext(); stream.remove()) {
            for (int i : entered) {
                assertTrue(bs.contains(i));
            }
            int i = stream.next();
            assertTrue(bs.contains(i));
            bs.remove(i);
            assertFalse(bs.contains(i));
        }
        assertEquals(0, bs.cardinality());
    }

    public void testClone() {
        NodeIdSet original = new NodeIdSet();
        original.add(1);
        original.add(9);
        original.add(0);

        assertEquals(3, original.cardinality());

        NodeIdSet clone = original.clone();
        assertEquals(3, clone.cardinality());

        assertTrue(clone.contains(0));
        assertTrue(clone.contains(1));
        assertTrue(clone.contains(9));

    }

    public void testMergeDifferentBitSize() {
        NodeIdSet small = new NodeIdSet();
        NodeIdSet large = new NodeIdSet();

        small.add(10);
        small.add(20);

        large.add(Identity.MAX_ID);

        assertTrue(small.size() < large.size());

        large.merge(small);
    }

    public void testOverlapDifferentBitSize() {
        NodeIdSet small = new NodeIdSet();
        NodeIdSet large = new NodeIdSet();

        small.add(10);
        small.add(20);

        large.add(Identity.MAX_ID);

        assertTrue(small.size() < large.size());

        large.overlap(small);
    }
}
