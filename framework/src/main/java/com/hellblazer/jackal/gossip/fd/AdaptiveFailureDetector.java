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

import com.hellblazer.jackal.gossip.FailureDetector;
import com.hellblazer.jackal.util.MultiWindow;
import com.hellblazer.jackal.util.SkipList;

/**
 * An adaptive accural failure detector based on the paper:
 * "A New Adaptive Accrual Failure Detector for Dependable Distributed Systems"
 * by Benjamin Satzger, Andreas Pietzowski, Wolfgang Trumler, Theo Ungerer
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class AdaptiveFailureDetector extends MultiWindow implements
        FailureDetector {

    private double         last        = -1.0;
    private final double   minInterval;
    private final double   scale;
    private final SkipList sorted      = new SkipList();
    private final double   threshold;
    private double         sumOfDelays = 0.0;

    public AdaptiveFailureDetector(double convictionThreshold, int windowSize,
                                   double scale, long expectedSampleInterval,
                                   int initialSamples, double minimumInterval) {
        super(windowSize, 2);
        threshold = convictionThreshold;
        minInterval = minimumInterval;
        this.scale = scale;

        long now = System.currentTimeMillis();
        last = now - initialSamples * expectedSampleInterval;
        for (int i = 0; i < initialSamples; i++) {
            record((long) (last + expectedSampleInterval), 0L);
        }
        assert last == now;
    }

    @Override
    public synchronized void record(long timeStamp, long delay) {
        if (last >= 0.0) {
            double sample = timeStamp - last;
            if (sample < minInterval) {
                return;
            }
            sorted.add(sample);
            sumOfDelays += delay;
            if (count == samples.length) {
                double[] removed = removeFirst();
                sorted.remove(removed[0]);
                sumOfDelays -= removed[1];
            }
            addLast(sample, delay);
        }
        last = timeStamp + sumOfDelays / count;
    }

    @Override
    public synchronized boolean shouldConvict(long now) {
        double delta = (now - last) * scale;
        double countLessThanEqualTo = sorted.countLessThanEqualTo(delta);
        boolean convict = countLessThanEqualTo / count >= threshold;
        return convict;
    }
}
