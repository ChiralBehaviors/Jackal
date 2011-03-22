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
package com.hellblazer.anubis.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A simple ring buffer for storing windows of samples.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Window implements Iterable<Double> {
    private class RingBufferIterator implements Iterator<Double> {
        private int i = 0;

        @Override
        public boolean hasNext() {
            return i < samples.length;
        }

        @Override
        public Double next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return samples[i++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    protected int            count = 0;
    private int              head  = 0;
    protected final double[] samples;
    private int              tail  = 0;

    public Window(int windowSize) {
        samples = new double[windowSize];
    }

    public void addLast(double value) {
        samples[tail] = value;
        tail = (tail + 1) % samples.length;
        count++;
    }

    @Override
    public Iterator<Double> iterator() {
        return new RingBufferIterator();
    }

    public double removeFirst() {
        double item = samples[head];
        count--;
        head = (head + 1) % samples.length;
        return item;
    }

    public int size() {
        return count;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("samples: [");
        for (double sample : this) {
            builder.append(sample).append(", ");
        }
        builder.append(']');
        return builder.toString();
    }
}
