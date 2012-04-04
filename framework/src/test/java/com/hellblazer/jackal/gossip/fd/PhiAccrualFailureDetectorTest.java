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
package com.hellblazer.jackal.gossip.fd;

import junit.framework.TestCase;

import com.hellblazer.jackal.gossip.FailureDetector;

/**
 * Basic testing of the failure detector
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class PhiAccrualFailureDetectorTest extends TestCase {

    public void testDetector() throws Exception {

        FailureDetector detector = new PhiAccrualFailureDetector(11, false,
                                                                 1000, 500, 0,
                                                                 1.0);
        long inc = 500;

        long now = System.currentTimeMillis();

        detector.record(now, 0L);
        now += inc;

        detector.record(now, 0L);
        now += inc;

        detector.record(now, 0L);
        now += inc;

        detector.record(now, 0L);
        now += inc;

        detector.record(now, 0L);

        now += inc;

        assertFalse(detector.shouldConvict(now));

        assertTrue(detector.shouldConvict(now + 30000));
    }
}
