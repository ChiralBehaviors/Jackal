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

import org.smartfrog.services.anubis.locator.util.SetMap;
import org.smartfrog.services.anubis.partition.test.mainconsole.NodeData;
import org.smartfrog.services.anubis.partition.views.View;

public class ColorAllocator {

    /**
     * keep a list of nodes that have a color allocated indexed by
     * bitmap (from the view)
     */
    private SetMap nodes = new SetMap();

    /**
     * keep a list of colors that have been allocated indexed
     * by bitmap (from the view)
     */
    private Map allocations = new HashMap();

    private ColorMap colorMap = new ColorMap();

    public ColorAllocator() {
    }

    public synchronized Color allocate(View view, NodeData node) {

        if( nodes.containsKey(view.toBitSet()) ) {
            nodes.put(view.toBitSet(), node);
            return (Color)allocations.get(view.toBitSet());
        }

        nodes.put(view.toBitSet(), node);
        Color c = colorMap.allocate(view.toBitSet());

        if( !c.equals(colorMap.defaultColor) )
            allocations.put(view.toBitSet(), c);

        return c;
    }

    public synchronized void deallocate(View view, NodeData node) {

        nodes.remove(view.toBitSet(), node);
        if( nodes.getSet(view.toBitSet()) == null ) {
            Color c = colorMap.deallocate(view.toBitSet());
            allocations.remove(c);
        }
    }

}
