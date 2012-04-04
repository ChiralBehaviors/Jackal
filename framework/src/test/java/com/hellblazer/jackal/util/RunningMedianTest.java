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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;

public class RunningMedianTest extends TestCase {
    public void testMedian() {
        Random random = new Random(666);
        RunningMedian median = new RunningMedian(1000);
        List<Double> input = new ArrayList<Double>();

        for (int i = 0; i < 300; i++) {
            double sample = random.nextDouble();
            input.add(sample);
            median.sample(sample);
        }
        Collections.sort(input);
        assertEquals(input.get(input.size() / 2), median.value());

        median = new RunningMedian(1000);
        input = new ArrayList<Double>();
        for (int i = 0; i < 1500; i++) {
            double sample = random.nextDouble();
            input.add(sample);
            median.sample(sample);
        }
        input = input.subList(500, 1500);
        assertEquals(1000, input.size());
        Collections.sort(input);
        assertEquals(input.get(input.size() / 2), median.value());
    }
}
