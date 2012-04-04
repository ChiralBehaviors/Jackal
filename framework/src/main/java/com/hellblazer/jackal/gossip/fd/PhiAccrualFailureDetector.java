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
package com.hellblazer.jackal.gossip.fd;

import java.util.concurrent.locks.ReentrantLock;

import com.hellblazer.jackal.gossip.FailureDetector;
import com.hellblazer.jackal.util.RunningAverage;
import com.hellblazer.jackal.util.RunningMedian;
import com.hellblazer.jackal.util.SampledWindow;

/**
 * Instead of providing information of a boolean nature (trust vs. suspect),
 * this failure detector outputs a suspicion level on a continuous scale. The
 * protocol samples the arrival time of heartbeats and maintains a sliding
 * window of the most recent samples. This window is used to estimate the
 * arrival time of the next heartbeat, similarly to conventional adaptive
 * failure detectors. The distribution of past samples is used as an
 * approximation for the probabilistic distribution of future heartbeat
 * messages. With this information, it is possible to compute a value phi with a
 * scale that changes dynamically to match recent network conditions.
 * 
 * Based on the paper, "The phi Accrual Failure Detector", by Naohiro
 * Hayashibara, Xavier Defago, Rami Yared, and Takuya Katayama
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class PhiAccrualFailureDetector implements FailureDetector {
    private double              last;
    private final double        minInterval;
    private final ReentrantLock stateLock = new ReentrantLock();
    private final double        threshold;
    private SampledWindow       window;

    public PhiAccrualFailureDetector(double convictThreshold,
                                     boolean useMedian, int windowSize,
                                     long expectedSampleInterval,
                                     int initialSamples, double minimumInterval) {
        threshold = convictThreshold;
        minInterval = minimumInterval;
        if (useMedian) {
            window = new RunningMedian(windowSize);
        } else {
            window = new RunningAverage(windowSize);
        }
        long now = System.currentTimeMillis();
        last = now - initialSamples * expectedSampleInterval;
        for (int i = 0; i < initialSamples; i++) {
            record((long) (last + expectedSampleInterval), 0L);
        }
        assert last == now;
    }

    /* (non-Javadoc)
     * @see com.hellblazer.jackal.gossip.FailureDetector#record(long)
     */
    @Override
    public void record(long now, long delay) {
        final ReentrantLock myLock = stateLock;
        try {
            myLock.lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }
        try {
            double interArrivalTime = now - last;
            if (interArrivalTime < minInterval) {
                return;
            }
            window.sample(interArrivalTime);
            last = now;
        } finally {
            myLock.unlock();
        }
    }

    /**
     * 
     * Given the programmed conviction threshold sigma, and assuming that we
     * decide to suspect when phi >= sigma, when sigma = 1 then the likeliness
     * that we will make a mistake (i.e., the decision will be contradicted in
     * the future by the reception of a late heartbeat) is about 10%. The
     * likeliness is about 1% with sigma = 2, 0.1% with sigma = 3, and so on.
     * <p>
     * Although the original paper suggests that the distribution is
     * approximated by the Gaussian distribution the Cassandra group has
     * reported that the Exponential Distribution to be a better approximation,
     * because of the nature of the gossip channel and its impact on latency
     * 
     * @see com.hellblazer.jackal.gossip.FailureDetector#shouldConvict(long)
     */
    @Override
    public boolean shouldConvict(long now) {
        final ReentrantLock myLock = stateLock;
        try {
            myLock.lockInterruptibly();
        } catch (InterruptedException e) {
            return false;
        }
        try {
            if (window.size() == 0) {
                return false;
            }
            double delta = now - last;
            double phi = -1
                         * Math.log10(Math.pow(Math.E,
                                               -1 * delta / window.value()));
            boolean shouldConvict = phi > threshold;
            /*
            if (shouldConvict) {
                System.out.println("delta: " + delta);
            }
            */
            return shouldConvict;
        } finally {
            myLock.unlock();
        }
    }
}
