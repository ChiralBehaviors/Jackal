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
 */

public class Digest implements Comparable<Digest> {

    InetSocketAddress endpoint;
    int generation;
    int maxVersion;

    Digest(InetSocketAddress ep, int gen, int version) {
        endpoint = ep;
        generation = gen;
        maxVersion = version;
    }

    @Override
    public int compareTo(Digest digest) {
        if (generation != digest.generation) {
            return generation - digest.generation;
        }
        return maxVersion - digest.maxVersion;
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
        if (generation != other.generation) {
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
        result = prime * result + generation;
        result = prime * result + maxVersion;
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(endpoint);
        sb.append(":");
        sb.append(generation);
        sb.append(":");
        sb.append(maxVersion);
        return sb.toString();
    }

    InetSocketAddress getEndpoint() {
        return endpoint;
    }

    int getGeneration() {
        return generation;
    }

    int getMaxVersion() {
        return maxVersion;
    }
}
