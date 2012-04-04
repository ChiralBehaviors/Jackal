/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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
package com.hellblazer.jackal.gossip;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import junit.framework.TestCase;

import com.hellblazer.jackal.gossip.Digest.DigestComparator;

/**
 * Basic testing of the digest state
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class DigestTest extends TestCase {
    public void testBasic() throws Exception {
        InetSocketAddress address = new InetSocketAddress("localhost", 80);
        Digest s = new Digest(address, 667);
        assertEquals(667, s.getTime());
        assertEquals(address, s.getAddress());
        byte[] bytes = new byte[GossipMessages.DIGEST_BYTE_SIZE];
        ByteBuffer msg = ByteBuffer.wrap(bytes);
        s.writeTo(msg);
        msg.flip();
        Digest d = new Digest(msg);
        assertEquals(667, d.getTime());
        assertEquals(address, d.getAddress());
        DigestComparator comparator = new DigestComparator();
        assertEquals(0, comparator.compare(s, d));

        Digest l2 = new Digest(address, 666);
        assertEquals(-1, comparator.compare(l2, d));
        Digest g2 = new Digest(address, 668);
        assertEquals(1, comparator.compare(g2, d));
    }
}
