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

import com.hellblazer.anubis.util.SkipList;
import com.hellblazer.anubis.util.Window;

/**
 * An adaptive accural failure detector based on the paper:
 * "A New Adaptive Accrual Failure Detector for Dependable Distributed Systems"
 * by Benjamin Satzger, Andreas Pietzowski, Wolfgang Trumler, Theo Ungerer
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class AdaptiveFailureDetector extends Window implements
        AccrualFailureDetector {
    public final static double DEFAULT_SCALE       = 0.9;
    public final static int    DEFAULT_WINDOW_SIZE = 1000;

    private double             last                = -1.0;
    private final double       scale;
    private final SkipList     sorted              = new SkipList();

    public AdaptiveFailureDetector() {
        this(DEFAULT_WINDOW_SIZE, DEFAULT_SCALE);
    }

    public AdaptiveFailureDetector(int windowSize, double scale) {
        super(windowSize);
        this.scale = scale;
    }

    @Override
    public double p(long now) {
        double delta = (now - last) * scale;
        double countLessThanEqualTo = sorted.countLessThanEqualTo(delta);
        return countLessThanEqualTo / (double) count;
    }

    @Override
    public void record(long now) {
        if (last >= 0.0) {
            double sample = now - last;
            sorted.add(sample);
            if (count == samples.length) {
                sorted.remove(removeFirst());
            }
            addLast(sample);
        }
        last = now;
    }
}
