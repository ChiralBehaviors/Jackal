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
package com.hellblazer.jackal.gossip;

/**
 * 
 * The failure detector contract
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public interface FailureDetector {

    /**
     * Record the arrival time of a heartbeat.
     * 
     * @param now
     *            - the timestamp of the heartbeat
     * @param delay
     *            - the local delay perceived in receiving the heartbeat
     */
    public abstract void record(long now, long delay);

    /**
     * Answer true if the suspicion level of the detector has exceeded the
     * conviction threshold.
     * 
     * @param now
     *            - the the time to calculate conviction
     * @return - true if the conviction threshold has been exceeded.
     */
    public abstract boolean shouldConvict(long now);

}