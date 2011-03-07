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
public class PhiAccrualFailureDetector {
    private static final int WINDOW_SIZE = 1000;
    private int              count       = 0;
    private int              head        = 0;
    private double           last        = -1D;
    private final double[]   samples     = new double[WINDOW_SIZE];
    private double           sum         = 0D;
    private int              tail        = 0;

    /**
     * Answer the suspicion level of the detector.
     * <p>
     * Given some threshold sigma, and assuming that we decide to suspect p when
     * phi >= sigma, when sigma = 1 then the likeliness that we will make a
     * mistake (i.e., the decision will be contradicted in the future by the
     * reception of a late heartbeat) is about 10%. The likeliness is about 1%
     * with sigma = 2, 0.1% with sigma = 3, and so on.
     * <p>
     * Although the original paper suggests that the distribution is
     * approximated by the Gaussian distribution the Cassandra group has
     * reported that the Exponential Distribution to be a better approximation,
     * because of the nature of the gossip channel and its impact on latency
     * 
     * @param now
     *            - the the time to calculate phi
     * @return - the suspicion level of the detector
     */
    public synchronized double phi(long now) {
        if (count == 0) {
            return 0d;
        }
        return -1
               * Math.log10(Math.pow(Math.E, -1 * (now - last) / (sum / count)));
    }

    /**
     * Record the inter arrival time of a heartbeat.
     */
    public synchronized void record(long now) {
        if (last > 0D) {
            double interArrivalTime = now - last;
            sum += interArrivalTime;
            if (count == WINDOW_SIZE) {
                sum -= removeFirst();
            }

            addLast(interArrivalTime);
        }
        last = now;
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
