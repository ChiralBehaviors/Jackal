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
package com.hellblazer.anubis.partition.coms.gossip;

import java.util.concurrent.locks.ReentrantLock;

import com.hellblazer.anubis.util.RunningAverage;
import com.hellblazer.anubis.util.RunningMedian;
import com.hellblazer.anubis.util.SampledWindow;

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
public class PhiAccrualFailureDetector implements AccrualFailureDetector {
    private static final boolean DEFAULT_USE_MEDIAN  = true;
    private static final int     DEFAULT_WINDOW_SIZE = 1000;
    private static final double  MIN_DELTA           = 10.0D;

    private double               last                = System.currentTimeMillis();
    private final ReentrantLock  stateLock           = new ReentrantLock();
    private SampledWindow        window;

    public PhiAccrualFailureDetector(long expectedSampleInterval) {
        this(DEFAULT_USE_MEDIAN, DEFAULT_WINDOW_SIZE, expectedSampleInterval,
             DEFAULT_WINDOW_SIZE / 2);
    }

    public PhiAccrualFailureDetector(long expectedSampleInterval,
                                     int initialSamples) {
        this(DEFAULT_USE_MEDIAN, DEFAULT_WINDOW_SIZE, expectedSampleInterval,
             initialSamples);
    }

    public PhiAccrualFailureDetector(boolean useMedian, int windowSize,
                                     long expectedSampleInterval,
                                     int initialSamples) {
        if (useMedian) {
            window = new RunningMedian(windowSize);
        } else {
            window = new RunningAverage(windowSize);
        }
        for (int i = 0; i < initialSamples; i++) {
            record((long) (last + expectedSampleInterval));
        }
    }

    /* (non-Javadoc)
     * @see com.hellblazer.anubis.partition.coms.gossip.AccrualFailureDetector#p(long)
     */
    @Override
    public double p(long now) {
        final ReentrantLock myLock = stateLock;
        try {
            myLock.lockInterruptibly();
        } catch (InterruptedException e) {
            return 0.0D;
        }
        try {
            if (window.size() == 0) {
                return 0.0D;
            }
            double phi = -1 * Math.log10(exponentialPhi(now));
            return phi;
        } finally {
            myLock.unlock();
        }
    }

    private double exponentialPhi(long now) {
        return Math.pow(Math.E, -1 * (now - last) / window.value());
    }

    /* (non-Javadoc)
     * @see com.hellblazer.anubis.partition.coms.gossip.AccrualFailureDetector#record(long)
     */
    @Override
    public void record(long now) {
        final ReentrantLock myLock = stateLock;
        try {
            myLock.lockInterruptibly();
        } catch (InterruptedException e) {
            return;
        }
        try {
            double interArrivalTime = now - last;
            if (interArrivalTime < MIN_DELTA) {
                return;
            }
            window.sample(interArrivalTime);
            last = now;
        } finally {
            myLock.unlock();
        }
    }
}
