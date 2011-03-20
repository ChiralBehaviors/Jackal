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
package com.hellblazer.anubis.util;

/**
 * Provide the median value for a windowed set of samples.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class RunningMedian implements SampledWindow {
    private int            count  = 0;
    private int            head   = 0;
    private final double[] samples;
    private final SkipList sorted = new SkipList();
    private int            tail   = 0;

    public RunningMedian(int windowSize) {
        samples = new double[windowSize];
    }

    @Override
    public boolean hasSamples() {
        return count != 0;
    }

    @Override
    public void sample(double sample) {

        sorted.add(sample);
        if (count == samples.length) {
            sorted.remove(removeFirst());
        }

        addLast(sample);
    }

    @Override
    public double value() {
        if (count == 0) {
            throw new IllegalStateException(
                                            "Must have at least one sample to calculate the median");
        }
        return sorted.get(sorted.size() / 2);
    }

    private void addLast(double value) {
        samples[tail] = value;
        tail = (tail + 1) % samples.length;
        count++;
    }

    private double removeFirst() {
        double item = samples[head];
        count--;
        head = (head + 1) % samples.length;
        return item;
    }
}
