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

/**
 * Contains information about a specified list of Endpoints and the largest
 * version of the state they have generated as known by the local endpoint.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Digest implements Comparable<Digest> {

    InetSocketAddress epAddress;
    long epoch;
    long maxVersion;

    public Digest(InetSocketAddress ep, long diffEpoch, long diffVersion) {
        epAddress = ep;
        epoch = diffEpoch;
        maxVersion = diffVersion;
    }

    Digest(InetSocketAddress address, Endpoint ep) {
        epAddress = address;
        epoch = ep.getEpoch();
        maxVersion = ep.getHeartbeatVersion();
    }

    @Override
    public int compareTo(Digest digest) {
        if (epoch != digest.epoch) {
            return (int) (epoch - digest.epoch);
        }
        return (int) (maxVersion - digest.maxVersion);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Digest other = (Digest) obj;
        if (epoch != other.epoch) {
            return false;
        }
        if (maxVersion != other.maxVersion) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (epoch ^ epoch >>> 32);
        result = prime * result + (int) (maxVersion ^ maxVersion >>> 32);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(epAddress);
        sb.append(":");
        sb.append(epoch);
        sb.append(":");
        sb.append(maxVersion);
        return sb.toString();
    }

    InetSocketAddress getEpAddress() {
        return epAddress;
    }

    long getEpoch() {
        return epoch;
    }

    long getMaxVersion() {
        return maxVersion;
    }
}
