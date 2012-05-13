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
package org.smartfrog.services.anubis.partition.views;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;

public class BitView implements View, Cloneable, Serializable,
        Iterable<Integer> {

    private static final long serialVersionUID = 1L;

    public static BitView create(Identity id, long t) {
        if (id == null) {
            throw new NullPointerException(
                                           "Attempt to create BitView from null pointer");
        }
        return create(id.id, t);
    }

    public static BitView create(int id, long t) {
        BitView bv = new BitView();
        bv.add(id);
        bv.stablize();
        bv.setTimeStamp(t);
        return bv;
    }

    protected final AtomicBoolean stable    = new AtomicBoolean();
    protected final AtomicLong    timeStamp = new AtomicLong(
                                                             View.undefinedTimeStamp);
    protected final NodeIdSet     view;

    public BitView(NodeIdSet v) {
        view = v;
        assert view != null : "View must be non null";
    }

    public BitView() {
        this(new NodeIdSet());
    }

    public BitView(boolean s, NodeIdSet v, long t) {
        this(v);
        stable.set(s);
        timeStamp.set(t);
    }

    public BitView(View v) {
        this(v.isStable(), v.toBitSet(), v.getTimeStamp());
    }

    public boolean add(Identity i) {
        return add(i.id);
    }

    public boolean add(int i) {
        if (!view.contains(i)) {
            view.add(i);
            destablize();
            return true;
        }
        return false;
    }

    @Override
    public int cardinality() {
        return view.cardinality();
    }

    @Override
    public boolean containedIn(View v) {
        return v.contains(this);
    }

    @Override
    public boolean contains(Identity id) {
        return contains(id.id);
    }

    @Override
    public boolean contains(int i) {
        return view.contains(i);
    }

    @Override
    public boolean contains(View v) {
        if (view.size() < v.size()) {
            return false;
        }
        for (int i = 0; i < view.size(); i++) {
            if (v.contains(i) && !contains(i)) {
                return false;
            }
        }
        return true;
    }

    public BitView copyView(View v) {
        stable.set(v.isStable());
        view.copyFrom(v.toBitSet());
        timeStamp.set(v.getTimeStamp());
        return this;
    }

    public void destablize() {
        stable.set(false);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof View) {
            return equalsView((View) obj);
        }
        return false;
    }

    @Override
    public boolean equalsView(View v) {
        return view.equals(v.toBitSet());
    }

    @Override
    public long getTimeStamp() {
        return timeStamp.get();
    }

    @Override
    public int hashCode() {
        return view.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return view.isEmpty();
    }

    @Override
    public boolean isStable() {
        return stable.get();
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

    public BitView merge(View v) {
        view.merge(v.toBitSet());
        return this;
    }

    @Override
    public boolean overlap(View v) {
        return view.overlap(v.toBitSet());
    }

    public boolean remove(Identity i) {
        return remove(i.id);
    }

    public boolean remove(int i) {
        if (view.contains(i)) {
            view.remove(i);
            destablize();
            return true;
        }
        return false;
    }

    public boolean removeComplement(View v) {
        boolean anyremoved = false;
        for (int i = 0; i < size(); i++) {
            if (!v.contains(i)) {
                remove(i);
                anyremoved = true;
            }
        }
        return anyremoved;
    }

    public void setTimeStamp(long t) {
        timeStamp.set(t);
    }

    @Override
    public int size() {
        return view.size();
    }

    public void stablize() {
        stable.set(true);
    }

    public BitView subtract(View v) {
        view.subtract(v.toBitSet());
        return this;
    }

    @Override
    public NodeIdSet toBitSet() {
        return view;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("<");
        builder.append("|");
        builder.append(isStable() ? "stable: " : "unstable: ");
        for (int i = 0; i < size(); i++) {
            if (contains(i)) {
                builder.append(i).append(" ");
            }
        }
        builder.append(">");
        return builder.toString();
    }
}
