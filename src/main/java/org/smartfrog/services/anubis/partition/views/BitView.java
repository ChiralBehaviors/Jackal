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

import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.util.Identity;

public class BitView implements View, Cloneable, Serializable {

    protected boolean stable    = true;
    protected NodeIdSet  view      = null;
    protected long    timeStamp = View.undefinedTimeStamp;

    public BitView() {
        view = new NodeIdSet();
    }

    public BitView(boolean s, NodeIdSet v, long t) {
        if( v == null )
            throw new NullPointerException("Attempt to construct BitView from null pointer");
        stable    = s;
        view      = v;
        timeStamp = t;
    }
    
    public BitView(View v) {
        if( v == null )
            throw new NullPointerException("Attempt to construct BitView from null pointer");
        stable    = v.isStable();
        view      = v.toBitSet();
        timeStamp = v.getTimeStamp();
    }
    
    public static BitView create(int id, long t) {
        BitView bv = new BitView();
        bv.add(id);
        bv.stablize();
        bv.setTimeStamp(t);
        return bv;
    }
    
    public static BitView create(Identity id, long t) {
        if( id == null )
            throw new NullPointerException("Attempt to create BitView from null pointer");
        return create(id.id, t);
    }

    public boolean isStable()            { return stable; }
    public long    getTimeStamp()        { return timeStamp; }
    public void    setTimeStamp(long t)  { timeStamp = t; }
    public int     size()                { return view.size(); }
    public int     cardinality()         { return view.cardinality(); }
    public boolean isEmpty()             { return view.isEmpty(); }
    public void    stablize()            { stable = true; }
    public void    destablize()          { stable = false; }
    public BitView copyView(View v) {
        stable    = v.isStable();
        view      = (NodeIdSet)v.toBitSet().clone();
        timeStamp = v.getTimeStamp();
        return this;
    }
    public boolean add(int i) {
        if( !view.contains(i) ) {
            view.add(i);
            destablize();
            return true;
        }
        return false;
    }
    public boolean add(Identity i)       { return add(i.id); }
    public boolean remove(int i) {
        if( view.contains(i) ) {
            view.remove(i);
            destablize();
            return true;
        }
        return false;
    }
    public boolean remove(Identity i)     { return remove(i.id); }
    public NodeIdSet  toBitSet()            { return view; }
    public boolean contains(int i)       { return view.contains(i); }
    public boolean contains(Identity id) { return contains(id.id); }
    public boolean containedIn(View v)   { return v.contains(this); }

    public boolean contains(View v) {
        if( view.size() < v.size() )
            return false;
        for(int i = 0; i < view.size(); i++)
            if( v.contains(i) && !contains(i) )
                return false;
        return true;
    }

    public boolean overlap(View v) {
        return view.overlap(v.toBitSet());
    }

    public boolean equalsView(View v) {
        return view.equals(v.toBitSet());
    }

    public boolean equals(Object obj) {
        if( obj instanceof View )
            return equalsView((View)obj);
        else
            return false;
    }

    public boolean removeComplement(View v) {
        boolean anyremoved = false;
        for(int i = 0; i < size(); i++)
            if( !v.contains(i) ) {
                remove(i);
                anyremoved = true;
            }
        return anyremoved;
    }

    public BitView subtract(View v) {
        view.subtract(v.toBitSet());
        return this;
    }

    public BitView merge(View v) {
        view.merge(v.toBitSet());
        return this;
    }

    public String toString() {
        String str = "<";
        str += timeStamp + "|";
        str += (isStable() ? "stable: " : "unstable: ");
        for(int i = 0; i < size(); i++)
            if( contains(i) )
                str += i + " ";
        str += ">";
        return str;
    }
}
