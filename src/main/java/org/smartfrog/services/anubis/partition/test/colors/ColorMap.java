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
package org.smartfrog.services.anubis.partition.test.colors;


import java.util.HashMap;
import java.util.Map;

import java.awt.Color;

import org.smartfrog.services.anubis.partition.util.NodeIdSet;

class ColorMatrix {

    Color[] color = new Color[301];
    int     size  = 301;
    int     step  = 7;
    int     redStep   = 34; // 3 values (100, 134, 168)
    int     greenStep = 11; // 10 values (100, 111, 122, 133, ..., 199)
    int     blueStep  = 11; // 10 values (dito)

    ColorMatrix() {
        int idx = 0;
        for( int red = 100; red < 200; red += redStep) {
            for( int green = 100; green < 200; green += greenStep) {
                for( int blue = 100; blue < 200; blue += blueStep ) {
                    color[idx++] = new Color(red, green, blue);
                }
            }
        }
        color[idx] = new Color(200, 100, 200);
    }
    Color   getColor(Integer idx)  { return color[idx.intValue()]; }
    Integer getIndex(int hash)     { return (hash < 0 ) ? new Integer((-hash) % size) : new Integer(hash % size); }
    Integer nextIndex(Integer idx) { return getIndex(idx.intValue() + step); }
}


public class ColorMap {

    private ColorMatrix color;
    private Map         viewColor = new HashMap();

    public static final Color       defaultColor = Color.white;


    public ColorMap() {
        color = new ColorMatrix();
    }


    public Color allocate(NodeIdSet bitSet) {

        Integer orig   = color.getIndex( bitSet.hashCode() );
        Integer idx    = orig;

        // check for collisions
        while( viewColor.containsKey(idx) ) {

            // if the color has already been allocated to this bit map then
            // return it again
            if( viewColor.get(idx).equals(bitSet) )
                return (Color)color.getColor(idx);

            // if collision then just step on
            idx = color.nextIndex(idx);

            // if we have been all the way around then give up
            if( idx.equals(orig) )
                return defaultColor;
        }

        // allocate the color associated with idx
        viewColor.put(idx, bitSet);
        return (Color)color.getColor(idx);
    }



    public Color deallocate(NodeIdSet bitSet) {

        Integer orig   = color.getIndex( bitSet.hashCode() );
        Integer idx    = orig;

        // check for collisions
        while( viewColor.containsKey(idx) ) {

            // if the correct bitmap, deallocate and quit
            if( viewColor.get(idx).equals(bitSet) ) {
                viewColor.remove(idx);
                return color.getColor(idx);
            }

            // if collision then just step on
            idx = color.nextIndex(idx);

            // if we have been all the way around then give up
            if( idx.equals(orig) )
                return defaultColor;
        }

        // if the color was not allocated return the default color
        return defaultColor;
    }

}
