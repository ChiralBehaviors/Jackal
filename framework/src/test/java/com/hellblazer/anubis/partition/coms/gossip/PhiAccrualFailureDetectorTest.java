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
package com.hellblazer.anubis.partition.coms.gossip;

import junit.framework.TestCase;

/**
 * Basic testing of the failure detector
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class PhiAccrualFailureDetectorTest extends TestCase {

    public void testDetector() throws Exception {

        PhiAccrualFailureDetector detector = new PhiAccrualFailureDetector();

        detector.record(111);
        detector.record(222);
        detector.record(333);
        detector.record(444);
        detector.record(555);

        assertEquals(0.4342, detector.phi(666), 0.01);

        assertEquals(9.566, detector.phi(3000), 0.01);
    }
}
