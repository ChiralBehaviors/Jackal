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
package org.smartfrog.services.anubis.locator.util;


import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Comparator;

public class SortedSetMap extends SetMap {

    /**
     * Construct a SetMap using the TreeMap as an implementation.
     * The TreeMap implements the SortedMap interface.
     *
     * @param comp the comparator to be used by the TreeMap.
     */
    public SortedSetMap(Comparator comp) {
        super(new TreeMap(comp));
    }

    /**
     * get the first key according to the sorted order - leaves the
     * map unchanged.
     *
     * @return the first key or null if the map is empty
     */
    public Object firstKey() { return ((SortedMap)map).firstKey(); }

    /**
     * get the last key according to the sorted order - leaves the map
     * unchanged.
     *
     * @return the last key or null if the map is empty
     */
    public Object lastKey()  { return ((SortedMap)map).lastKey(); }

}
