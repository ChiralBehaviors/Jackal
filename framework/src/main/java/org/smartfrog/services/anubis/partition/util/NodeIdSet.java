/** (C) Copyright 1998-2005 Hewlett-Packard Development Company, LP

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

For more information: www.smartfrog.org

 */
package org.smartfrog.services.anubis.partition.util;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireSizes;

/**
 * The NodeIdSet is the implementation of a set of node ids used as the basis
 * for views and other abstractions that represent node groups. Nodes are either
 * present or not present. There are some methods to manipulate or inspect
 * individual nodes and the entire set of nodes.
 */

public class NodeIdSet implements Serializable, Cloneable, WireSizes,
        Iterable<Integer> {

    private static final int  DEFAULT_SIZE     = 33;
    private static final long serialVersionUID = 1L;

    public static NodeIdSet readWireForm(ByteBuffer bytes, int idx, int len) {
        assert bytes.hasRemaining() : "Empty byte buffer";
        bytes.position(idx);
        byte[] storage = new byte[bytes.getInt()];
        bytes.get(storage);
        return new NodeIdSet(storage);
    }

    /**
     * use a byte array to hold the bits for messaging efficency.
     */
    private byte[] storage;

    /**
     * create a bitset with a default size
     */
    public NodeIdSet() {
        this(DEFAULT_SIZE);
    }

    public NodeIdSet(ByteBuffer buffer) {
        int size = buffer.getInt();
        storage = new byte[size];
        buffer.get(storage);
    }

    /**
     * create a bitset with a size large enough to contain at least i bits
     * 
     * @param i
     *            int
     */
    public NodeIdSet(int i) {
        storage = createByteArray(i, null);
    }

    private NodeIdSet(byte[] storage) {
        this.storage = storage;
    }

    /**
     * add bit at index i
     * 
     * @param i
     *            int
     * @return boolean
     */
    public boolean add(int i) {
        if (i >= size()) {
            // resize
            storage = createByteArray(i + 1, storage);
        }
        int byteNbr = i / 8;
        byte pos = (byte) (i % 8);
        storage[byteNbr] = (byte) (storage[byteNbr] | 1 << pos);
        return true;
    }

    /**
     * number of bits in the set
     * 
     * @return int
     */
    public int cardinality() {
        int bc = 0;
        for (int i = 0; i < storage.length; ++i) {
            for (int j = 0; j < 8; ++j) {
                bc += storage[i] >> j & 1;
            }
        }
        return bc;
    }

    /**
     * Return a new identical copy
     * 
     * @return BitSet
     */
    @Override
    public NodeIdSet clone() {
        NodeIdSet cloneBS;
        try {
            cloneBS = (NodeIdSet) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new IllegalStateException("Unable to clone!", e);
        }
        cloneBS.storage = Arrays.copyOf(storage, storage.length);
        return cloneBS;
    }
    
    public void copyFrom(NodeIdSet original) {
        storage = Arrays.copyOf(original.storage, original.storage.length);
    }

    /**
     * are all the bits in this contained in s?
     * 
     * @param s
     *            BitSet
     * @return boolean
     */
    public boolean containedIn(NodeIdSet s) {
        return compare(s.getBytes(), storage);
    }

    /**
     * is there a bit with index i?
     * 
     * @param i
     *            int
     * @return boolean
     */
    public boolean contains(int i) {
        boolean containOk = false;
        if (i < size()) {
            int byteNbr = i / 8;
            byte pos = (byte) (i % 8);
            byte test = (byte) (storage[byteNbr] & 1 << pos);
            containOk = test != 0;
        }
        return containOk;
    }

    /**
     * Are all the bits in s contained in this?
     * 
     * @param s
     *            BitSet
     * @return boolean
     */
    public boolean contains(NodeIdSet s) {
        return compare(storage, s.getBytes());
    }

    /**
     * equal if obj is a bitset and contains the same bits as this
     * 
     * @param obj
     *            Object
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        boolean equalOk = false;
        if (obj != null && obj instanceof NodeIdSet) {
            NodeIdSet objBS = (NodeIdSet) obj;
            equalOk = this.contains(objBS)
                      && cardinality() == objBS.cardinality();
        }
        return equalOk;
    }

    public int first() {
        for (int i = 0; i < storage.length; ++i) {
            for (int j = 0; j < 8; ++j) {
                if ((storage[i] >> j & 1) > 0) {
                    return i * 8 + j;
                }
            }
        }
        return 0;
    }

    /**
     * flip bit at index i (if present remove it, if absent add it)
     * 
     * @param i
     *            int
     * @return boolean
     */
    public boolean flip(int i) {
        boolean flipOk = false;
        if (i >= size()) {
            // resize
            storage = createByteArray(i + 1, storage);
        }
        //        if (i < this.size()){
        int byteNbr = i / 8;
        byte pos = (byte) (i % 8);
        storage[byteNbr] = (byte) (storage[byteNbr] ^ 1 << pos);
        flipOk = true;
        //        } 
        return flipOk;
    }

    public byte[] getBytes() {
        return storage;
    }

    /**
     * gives the index of the highest set bit
     * 
     * @return int
     */
    public int getMaxBitPos() {
        int retPos = 0;
        one: for (int i = storage.length - 1; i >= 0; --i) {
            if (storage[i] != 0) {
                // found the byte where the msb is
                for (int j = 7; j >= 0; --j) {
                    if ((storage[i] & 1 << j) != 0) {
                        retPos = i * 8 + j;
                        break one;
                    }
                }
            }
        }
        return retPos;
    }

    /**
     * a decent hash function
     * 
     * @return int
     */
    @Override
    public int hashCode() {
        long hash = 23;
        int ctr = 1;
        boolean override = false;
        for (int i = storage.length - 1; i >= 0; --i) {
            if (storage[i] != 0 || override) {
                ++ctr;
                hash ^= storage[i] & 0xff;
                hash = hash * 111 * ctr;
                // or could use (?)
                // hash = hash*37 + storage[i];
                override = true;
            }
        }
        return (int) hash;
    }

    /**
     * contains no bits?
     * 
     * @return boolean
     */
    public boolean isEmpty() {
        return cardinality() == 0;
    }

    @Override
    public Iterator<Integer> iterator() {
        int i = 0;
        for (i = 0; i < size(); i++) {
            if (contains(i)) {
                break;
            }
        }
        final int startPos = i;
        return new Iterator<Integer>() {
            int index = startPos;

            @Override
            public boolean hasNext() {
                return contains(index);
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                int value = index;
                int i = index + 1;
                for (; i < size(); i++) {
                    if (contains(i)) {
                        break;
                    }
                }
                index = i;
                return value;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public int last() {
        for (int i = storage.length - 1; i >= 0; --i) {
            for (int j = 7; j >= 0; --j) {
                if ((storage[i] >> j & 1) > 0) {
                    return i * 8 + j;
                }
            }
        }
        return 0;
    }

    /**
     * Answer the left neighbor of the id, modeling the receiver as a sorted
     * ring
     * 
     * @param id
     * @return the left neighbor id, or -1 if the receiver does not contain the
     *         id or the id is the only member of the set
     */
    public int leftNeighborOf(int id) {
        if (id >= size()) {
            // not a member of the set
            return -1;
        }
        final int byteNbr = id / 8;
        final byte pos = (byte) (id % 8);
        final byte homeByte = storage[byteNbr];
        if ((byte) (homeByte & 1 << pos) == 0) {
            // not a member of the set
            return -1;
        }

        // See if the left neighbor is in the home byte
        for (int i = pos - 1; i >= 0; i--) {
            if ((homeByte & 1 << i) != 0) {
                return byteNbr * 8 + i;
            }
        }
        // scan through the previous bytes
        for (int i = byteNbr - 1; i >= 0; i--) {
            for (int j = 7; j >= 0; j--) {
                if ((storage[i] >> j & 1) > 0) {
                    return i * 8 + j;
                }
            }
        }
        // wrap around
        return last();
    }

    /**
     * modify this bitset by adding all the bits that are in s (logically
     * bitwise: this = this OR s) return true if any modification, otherwise
     * false
     * 
     * @param s
     *            BitSet
     * @return BitSet
     */
    public boolean merge(NodeIdSet s) {
        boolean mergeOk = false;
        int msb = s.getMaxBitPos();
        if (msb > size()) {
            storage = createByteArray(msb, storage);
        }
        byte[] test = s.getBytes();
        byte oldValue = 0;
        int minLength = storage.length < test.length ? storage.length
                                                    : test.length;
        for (int i = 0; i < minLength; ++i) {
            oldValue = storage[i];
            storage[i] |= test[i];
            if (storage[i] != oldValue) {
                mergeOk = true;
            }
        }
        return mergeOk;
    }

    /**
     * Answer the left and right neighbors of the id, modeling the receiver as a
     * sorted ring
     * 
     * @param id
     * @return the left and right neighbor ids, or null if the receiver does not
     *         contain the id
     */
    public int[] neighborsOf(int id) {
        if (id >= size()) {
            // not a member of the set
            return null;
        }
        final int byteNbr = id / 8;
        final byte pos = (byte) (id % 8);
        final byte homeByte = storage[byteNbr];
        if ((byte) (homeByte & 1 << pos) == 0) {
            // not a member of the set
            return null;
        }

        int[] neighbors = new int[] { -1, -1 };

        // See if the right neighbor is in the home byte
        for (int i = pos + 1; i < 8; i++) {
            if ((homeByte & 1 << i) != 0) {
                neighbors[1] = byteNbr * 8 + i;
                break;
            }
        }

        // scan through the remaining bytes
        for (int i = byteNbr + 1; neighbors[1] == -1 && i < storage.length; i++) {
            for (int j = 0; j < 8; j++) {
                if ((storage[i] >> j & 1) > 0) {
                    neighbors[1] = i * 8 + j;
                    break;
                }
            }
        }
        if (neighbors[1] == -1) {
            // wrap around
            neighbors[1] = first();
        }

        // See if the left neighbor is in the home byte
        for (int i = pos - 1; i >= 0; i--) {
            if ((homeByte & 1 << i) != 0) {
                neighbors[0] = byteNbr * 8 + i;
                break;
            }
        }
        // scan through the previous bytes
        for (int i = byteNbr - 1; neighbors[0] == -1 && i >= 0; i--) {
            for (int j = 7; j >= 0; j--) {
                if ((storage[i] >> j & 1) > 0) {
                    neighbors[0] = i * 8 + j;
                    break;
                }
            }
        }
        if (neighbors[0] == -1) {
            // wrap around
            neighbors[0] = last();
        }
        return neighbors;
    }

    /**
     * are there any bits in both this and s?
     * 
     * @param s
     *            BitSet
     * @return boolean
     */
    public boolean overlap(NodeIdSet s) {
        boolean overlapOk = false;
        byte[] test = s.getBytes();
        int minLength = storage.length < test.length ? storage.length
                                                    : test.length;
        for (int i = 0; i < minLength; ++i) {
            if ((storage[i] & test[i]) != 0) {
                overlapOk = true;
                break;
            }
        }
        return overlapOk;
    }

    /**
     * remove bit at index i
     * 
     * @param i
     *            int
     * @return boolean
     */
    public boolean remove(int i) {
        boolean removeOk = false;
        if (i < size()) {
            int byteNbr = i / 8;
            byte pos = (byte) (i % 8);
            storage[byteNbr] = (byte) (storage[byteNbr] & ~(1 << pos));
            removeOk = true;
        }
        return removeOk;
    }

    /**
     * modify this bitset by removing anything that is NOT also in s (logically
     * bitwise: this = this AND s) return true if any modification, otherwise
     * false
     * 
     * @param s
     *            BitSet
     * @return boolean
     */
    public boolean removeComplement(NodeIdSet s) {
        boolean rcOk = false;
        byte[] test = s.getBytes();
        int minLength = storage.length < test.length ? storage.length
                                                    : test.length;
        byte oldValue = 0;
        for (int i = 0; i < minLength; ++i) {
            oldValue = storage[i];
            storage[i] &= test[i];
            if (storage[i] != oldValue) {
                rcOk = true;
            }
        }
        return rcOk;
    }

    /**
     * Answer the right neighbor of the id, modeling the receiver as a sorted
     * ring
     * 
     * @param id
     * @return the right neighbor id, or -1 if the receiver does not contain the
     *         id
     */
    public int rightNeighborOf(int id) {
        if (id >= size()) {
            // not a member of the set
            return -1;
        }
        final int byteNbr = id / 8;
        final byte pos = (byte) (id % 8);
        final byte homeByte = storage[byteNbr];
        if ((byte) (homeByte & 1 << pos) == 0) {
            // not a member of the set
            return -1;
        }

        // See if the right neighbor is in the home byte
        for (int i = pos + 1; i < 8; i++) {
            if ((homeByte & 1 << i) != 0) {
                return byteNbr * 8 + i;
            }
        }

        // scan through the remaining bytes
        for (int i = byteNbr + 1; i < storage.length; i++) {
            for (int j = 0; j < 8; j++) {
                if ((storage[i] >> j & 1) > 0) {
                    return i * 8 + j;
                }
            }
        }
        // wrap around
        return first();
    }

    /**
     * maximum bit index in the current storage array - i.e. storage.length *
     * bits_per_byte.
     * 
     * @return int
     */
    public int size() {
        return storage.length * 8;
    }

    /**
     * modify this bitset by removing anything that is in s (logically bitwise:
     * this = this AND ( NOT s)) return true if any modification, otherwise
     * false
     * 
     * @param s
     *            BitSet
     * @return BitSet
     */
    public boolean subtract(NodeIdSet s) {
        boolean subOk = false;
        byte[] test = s.getBytes();
        int minLength = storage.length < test.length ? storage.length
                                                    : test.length;
        byte oldValue = 0;
        for (int i = 0; i < minLength; ++i) {
            oldValue = storage[i];
            storage[i] &= ~test[i];
            if (storage[i] != oldValue) {
                subOk = true;
            }
        }
        return subOk;
    }

    @Override
    public String toString() {
        StringBuffer stBuf = new StringBuffer();
        stBuf.append("[");
        int maxSz = getMaxBitPos();
        for (int i = 0; i <= maxSz; ++i) {
            if (contains(i)) {
                stBuf.append(" " + i);
            }
        }
        stBuf.append(" ]");
        return stBuf.toString();
    }

    public void writeTo(ByteBuffer buffer) {
        buffer.putInt(storage.length);
        buffer.put(storage);
    }

    public void writeWireForm(ByteBuffer bytes, int idx, int len)
                                                                 throws WireFormException {

        if (storage.length + intSz > len) {
            throw new WireFormException("BitSet to large for allowed length ("
                                        + len + ")");
        }

        bytes.position(idx);
        bytes.putInt(storage.length);
        bytes.put(storage);
    }

    private boolean compare(byte[] base, byte[] test) {
        boolean compareOk = true;
        int minLength = base.length < test.length ? base.length : test.length;
        for (int i = 0; i < minLength; ++i) {
            if (test[i] != (base[i] & test[i])) {
                compareOk = false;
                break;
            }
        }
        if (compareOk && base.length < test.length) {
            int bc = 0;
            for (int i = minLength; i < test.length; ++i) {
                for (int j = 0; j < 8; ++j) {
                    bc += test[i] >> j & 1;
                }
            }
            if (bc != 0) {
                compareOk = false;
            }
        }
        return compareOk;
    }

    private byte[] createByteArray(int index, byte[] seed) {
        byte[] retBa = null;
        int byteNbr = index / 8;
        if (index % 8 != 0) {
            ++byteNbr;
        }
        retBa = new byte[byteNbr];
        if (seed != null && seed.length <= retBa.length) {
            for (int i = 0; i < seed.length; ++i) {
                retBa[i] = seed[i];
            }
        }
        return retBa;
    }

}
