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
package com.hellblazer.jackal.util;

/**
 * A simple ring buffer for storing windows of samples of multiple variables.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class MultiWindow {

    protected int              count = 0;
    private int                head  = 0;
    protected final double[][] samples;
    private int                tail  = 0;

    public MultiWindow(int windowSize, int numVariables) {
        samples = new double[windowSize][numVariables];
    }

    public void addLast(double... value) {
        samples[tail] = value;
        tail = (tail + 1) % samples.length;
        count++;
    }

    public double[] removeFirst() {
        double[] items = samples[head];
        count--;
        head = (head + 1) % samples.length;
        return items;
    }

    public int size() {
        return count;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("[ ");
        for (int i = 0; i < count; i++) {
            buf.append(samples[(i + head) % samples.length]);
            buf.append(", ");
        }
        buf.append("]");
        return buf.toString();
    }
}
