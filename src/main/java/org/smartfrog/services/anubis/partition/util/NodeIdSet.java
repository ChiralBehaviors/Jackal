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

import org.smartfrog.services.anubis.partition.wire.WireFormException;
import org.smartfrog.services.anubis.partition.wire.WireSizes;

/**
 * The NodeIdSet is the implementation of a set of node ids used as the basis
 * for views and other abstractions that represent node groups. Nodes are either
 * present or not present. There are some methods to manipulate or inspect
 * individual nodes and the entire set of nodes.
 */

public class NodeIdSet implements Serializable, Cloneable, WireSizes {

    private static final int DEFAULT_SIZE = 33;

    /**
     * use a byte array to hold the bits for messaging efficency.
     */
    private byte[] storage;

    /**
     * create a bitset with a default size
     */
    public NodeIdSet(){
        this(DEFAULT_SIZE);
    }

    /**
     * create a bitset with a size large enough to contain at least
     * i bits
     * @param i int
     */
    public NodeIdSet(int i) {
        storage = this.createByteArray(i, null);
    }


    private NodeIdSet(byte[] storage) {
        this.storage = storage;
    }


    public static NodeIdSet readWireForm(ByteBuffer bytes, int idx, int len) throws
        WireFormException {

        bytes.position(idx);
        byte[] storage = new byte[bytes.getInt()];
        bytes.get(storage);
        return new NodeIdSet(storage);
    }


    public void writeWireForm(ByteBuffer bytes, int idx, int len) throws
        WireFormException {

        if (storage.length + intSz > len)
            throw new WireFormException("BitSet to large for allowed length (" +
                                        len + ")");

        bytes.position(idx);
        bytes.putInt(storage.length);
        bytes.put(storage);
    }

    /**
     * maximum bit index in the current storage array - i.e.
     * storage.length * bits_per_byte.
     * @return int
     */
    public int size(){
        return storage.length*8;
    }

    /**
     * number of bits in the set
     * @return int
     */
    public int cardinality(){
        int bc = 0;
        for (int i=0; i<storage.length; ++i)
            for (int j=0; j<8; ++j)
                bc += ((storage[i] >> j)&1);
        return bc;
    }

    /**
     * contains no bits?
     * @return boolean
     */
    public boolean isEmpty(){
        return (this.cardinality() == 0);
    }

    /**
     * add bit at index i
     * @param i int
     * @return boolean
     */
    public boolean add(int i){
        boolean addOk = false;
        if (i >= this.size()){
            // resize
            storage = this.createByteArray(i+1, storage);
        }
        int byteNbr = i / 8;
        byte pos = (byte)(i % 8);
        storage[byteNbr] = (byte)(storage[byteNbr] | (1 << pos));
        addOk = true;
        return addOk;
    }

    /**
     * remove bit at index i
     * @param i int
     * @return boolean
     */
    public boolean remove(int i)
    {
        boolean removeOk = false;
        if (i < this.size()){
            int byteNbr = i / 8;
            byte pos = (byte)(i % 8);
            storage[byteNbr] = (byte)(storage[byteNbr] & ~(1 << pos));
            removeOk = true;
        }
        return removeOk;
    }

    /**
     * flip bit at index i (if present remove it, if absent add it)
     * @param i int
     * @return boolean
     */
    public boolean flip(int i){
        boolean flipOk = false;
        if (i >= this.size()){
            // resize
            storage = this.createByteArray(i+1, storage);
        }
//        if (i < this.size()){
            int byteNbr = i / 8;
            byte pos = (byte)(i % 8);
            storage[byteNbr] = (byte)(storage[byteNbr] ^ (1 << pos));
            flipOk = true;
//        } 
        return flipOk;
    }

    /**
     * is there a bit with index i?
     * @param i int
     * @return boolean
     */
    public boolean contains(int i){
        boolean containOk = false;
        if (i < this.size()){
            int byteNbr = i / 8;
            byte pos = (byte)(i % 8);
            byte test = (byte)(storage[byteNbr] & (1 << pos));
            containOk = (test != 0);
        }
        return containOk;
    }

    /**
     * Are all the bits in s contained in this?
     * @param s BitSet
     * @return boolean
     */
    public boolean contains(NodeIdSet s){
        return compare(storage, s.getBytes());
    }

    /**
     * are all the bits in this contained in s?
     * @param s BitSet
     * @return boolean
     */
    public boolean containedIn(NodeIdSet s){
        return compare(s.getBytes(), storage);
    }


    /**
     * are there any bits in both this and s?
     * @param s BitSet
     * @return boolean
     */
    public boolean overlap(NodeIdSet s){
        boolean overlapOk = false;
        byte[] test = s.getBytes();
        int minLength = ((storage.length < test.length) ? storage.length : test.length);
        for (int i=0; i<minLength; ++i){
            if ((storage[i]&test[i]) != 0){
                overlapOk = true;
                break;
            }
        }
        return overlapOk;
    }

    /**
     * modify this bitset by removing anything that is NOT also in s
     * (logically bitwise: this = this AND s)
     * return true if any modification, otherwise false
     * @param s BitSet
     * @return boolean
     */
    public boolean removeComplement(NodeIdSet s){
        boolean rcOk = false;
        byte[] test = s.getBytes();
        int minLength = ((storage.length < test.length) ? storage.length : test.length);
        byte oldValue = 0;
        for (int i=0; i<minLength; ++i){
            oldValue = storage[i];
            storage[i] &= test[i];
            if (storage[i] != oldValue)
                rcOk = true;
        }
        return rcOk;
    }

    /**
     * modify this bitset by removing anything that is in s
     * (logically bitwise: this = this AND ( NOT s))
     * return true if any modification, otherwise false
     * @param s BitSet
     * @return BitSet
     */
    public boolean subtract(NodeIdSet s){
        boolean subOk = false;
        byte[] test = s.getBytes();
        int minLength = ((storage.length < test.length) ? storage.length : test.length);
        byte oldValue = 0;
        for (int i=0; i<minLength; ++i){
            oldValue = storage[i];
            storage[i] &= ~test[i];
            if (storage[i] != oldValue)
                subOk = true;
        }
        return subOk;
    }

    /**
     * modify this bitset by adding all the bits that are in s
     * (logically bitwise: this = this OR s)
     * return true if any modification, otherwise false
     * @param s BitSet
     * @return BitSet
     */
    public boolean merge(NodeIdSet s){
        boolean mergeOk = false;
        int msb = s.getMaxBitPos();
        if (msb > this.size())
            storage = createByteArray(msb, storage);
        byte[] test = s.getBytes();
        byte oldValue = 0;
        for (int i=0; i<storage.length; ++i){
            oldValue = storage[i];
            storage[i] |= test[i];
            if (storage[i] != oldValue)
                mergeOk = true;
        }
        return mergeOk;
    }

    /**
     * equal if obj is a bitset and contains the same bits as this
     * @param obj Object
     * @return boolean
     */
    public boolean equals(Object obj){
        boolean equalOk = false;
        if ((obj != null) && (obj instanceof NodeIdSet)){
            NodeIdSet objBS = (NodeIdSet)obj;
            equalOk = (this.contains(objBS) && (this.cardinality() == objBS.cardinality()));
        }
        return equalOk;
    }


    /**
     * Return a new identical copy
     * @return BitSet
     */
    public Object clone(){
        NodeIdSet cloneBS = new NodeIdSet(this.size());
        byte[] cloneStorage = cloneBS.getBytes();
        for(int i=0; i<storage.length; ++i)
            cloneStorage[i] = storage[i];
        return cloneBS;
    }

    /**
     * a decent hash function
     * @return int
     */
    public int hashCode(){
        long hash = 23;
        int ctr = 1;
        boolean override = false;
        for (int i=(storage.length-1); i>=0; --i){
            if ((storage[i] != 0) || (override)){
                ++ctr;
                hash ^= (storage[i]&0xff);
                hash = hash*111*ctr;
                // or could use (?)
                // hash = hash*37 + storage[i];
                override = true;
            }
        }
        return (int)hash;
    }


    public String toString(){
        StringBuffer stBuf = new StringBuffer();
        stBuf.append("[");
        int maxSz = getMaxBitPos();
        for(int i=0; i <= maxSz; ++i) {
            if( contains(i) )
                stBuf.append(" " + i);
        }
        stBuf.append(" ]");
        return stBuf.toString();
    }

    public byte[] getBytes(){
        return storage;
    }

    /**
     * gives the index of the highest set bit
     * @return int
     */
    public int getMaxBitPos(){
        int retPos = 0;
        one:
            for (int i=(storage.length-1);i>=0;--i){
                if (storage[i] != 0){
                    // found the byte where the msb is
                    for (int j=7; j>=0; --j){
                        if ((storage[i]&(1<<j)) != 0){
                            retPos = i*8+j;
                            break one;
                        }
                    }
                }
            }
        return retPos;
    }

    private boolean compare(byte[] base, byte[] test){
        boolean compareOk = true;
        int minLength = ((base.length < test.length) ? base.length : test.length);
        for (int i=0; i<minLength; ++i){
            if (test[i] != (base[i]&test[i])){
                compareOk = false;
                break;
            }
        }
        if ( (compareOk) && (base.length < test.length) ){
            int bc = 0;
            for (int i= minLength; i < test.length; ++i){
                for (int j=0; j<8; ++j)
                    bc += ((test[i] >> j)&1);
            }
            if (bc != 0) compareOk = false;
        }
        return compareOk;
    }

    private byte[] createByteArray(int index, byte[] seed){
        byte[] retBa = null;
        int byteNbr = index/8;
        if (index%8 != 0)  ++byteNbr;
        retBa = new byte[byteNbr];
        if ((seed != null) && (seed.length <= retBa.length)){
            for(int i=0; i< seed.length; ++i)
                retBa[i] = seed[i];
        }
        return retBa;
    }

}
