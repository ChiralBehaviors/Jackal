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

import java.net.InetSocketAddress;
import java.util.Comparator;

/**
 * Contains information about a specified list of Endpoints and the largest
 * version of the state they have generated as known by the local endpoint.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Digest {

    public static class DigestComparator implements Comparator<Digest> {
        @Override
        public int compare(Digest digest1, Digest digest2) {
            if (digest1.epoch != digest2.epoch) {
                return (int) (digest1.epoch - digest2.epoch);
            }
            return (int) (digest1.viewNumber - digest2.viewNumber);
        }
    }

    private final InetSocketAddress address;
    private final long epoch;
    private final long viewNumber;

    public Digest(InetSocketAddress ep, long diffEpoch, long diffViewNumber) {
        address = ep;
        epoch = diffEpoch;
        viewNumber = diffViewNumber;
    }

    Digest(InetSocketAddress socketAddress, Endpoint ep) {
        address = socketAddress;
        epoch = ep.getEpoch();
        viewNumber = ep.getViewNumber();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(address);
        sb.append(":");
        sb.append(epoch);
        sb.append(":");
        sb.append(viewNumber);
        return sb.toString();
    }

    InetSocketAddress getEpAddress() {
        return address;
    }

    long getEpoch() {
        return epoch;
    }

    long getViewNumber() {
        return viewNumber;
    }
}
