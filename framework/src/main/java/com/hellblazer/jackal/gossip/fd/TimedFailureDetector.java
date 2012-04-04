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

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class TimedFailureDetector implements FailureDetector {
    private volatile long last = -1;
    private final long    maxInterval;

    public TimedFailureDetector(long maxInterval) {
        this.maxInterval = maxInterval;
    }

    @Override
    public void record(long now, long delay) {
        last = now;
    }

    @Override
    public boolean shouldConvict(long now) {
        if (last < 0) {
            return false;
        }
        return now - last > maxInterval;
    }

}
