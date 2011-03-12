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
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
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
    private final long              epoch;
    private final long              viewNumber;

    public Digest(ByteBuffer msg) throws UnknownHostException {
        address = GossipHandler.readInetAddress(msg);
        epoch = msg.getLong();
        viewNumber = msg.getLong();
    }

    public Digest(InetSocketAddress socketAddress, Endpoint ep) {
        address = socketAddress;
        epoch = ep.getEpoch();
        viewNumber = ep.getViewNumber();
    }

    public Digest(InetSocketAddress ep, long diffEpoch, long diffViewNumber) {
        address = ep;
        epoch = diffEpoch;
        viewNumber = diffViewNumber;
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
        if (address == null) {
            if (other.address != null) {
                return false;
            }
        } else if (!address.equals(other.address)) {
            return false;
        }
        if (epoch != other.epoch) {
            return false;
        }
        if (viewNumber != other.viewNumber) {
            return false;
        }
        return true;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public long getEpoch() {
        return epoch;
    }

    public long getViewNumber() {
        return viewNumber;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (address == null ? 0 : address.hashCode());
        result = prime * result + (int) (epoch ^ epoch >>> 32);
        result = prime * result + (int) (viewNumber ^ viewNumber >>> 32);
        return result;
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

    public void writeTo(ByteBuffer buffer) {
        GossipHandler.writeInetAddress(address, buffer);
        buffer.putLong(epoch);
        buffer.putLong(viewNumber);
    }
}
