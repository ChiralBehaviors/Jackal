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



import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * SetMap is a commonly used data structure that is a map in which all
 * entries are sets. It implements a map that associates a set of entries
 * with each key instead of the usual map that associates a single entry
 * with each key. SetMap does not extend collection - probably should for
 * the purists!!
 *
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class SetMap {

    protected Map   map = null;

    /**
     * Default constructor creates a HashMap implementation
     */
    public SetMap() { map = new HashMap(); }

    /**
     * Constructor - allows the user to define the implementation
     *               of the map. For example, it could be subclassed to
     *               create a sorted set map (sorted on the keys).
     * @param map
     */
    public SetMap(Map map) { this.map = map; }

    /**
     * removes all entries from the SetMap.
     */
    public void    clear() { map.clear(); }

    /**
     * indicates if the map is empty
     *
     * @return  boolean
     */
    public boolean isEmpty() { return map.isEmpty(); }

    /**
     * test to see if a key is contained in the map.
     *
     * @param key - the key
     * @return - true if the key is in the map, false if not.
     */
    public boolean containsKey(Object key) { return map.containsKey(key); }

    /**
     * returns a set view of the keys in the map.
     *
     * @return a set view of the keys
     */
    public Set     keySet() { return map.keySet(); }

    /**
     * returns a set view of the entries in the map. That is a set of
     * objects of the type Map.Entry.
     *
     * @return Set
     */
    public Set     entrySet() { return map.entrySet(); }

    /**
     * returns the set of entries associated with a given key.
     *
     * @param key - the key
     * @return - the set of entries associated with the key, or null if
     *           the key is not contained in the map.
     */
    public Set     getSet(Object key) { return (Set)map.get(key); }

    /**
     * returns the number of entries associated with a given key.
     * @param key - the key
     * @return - the number of entries in the set associated with key, or 0 if
     *           the map does not contain the key.
     */
    public int     getSetSize(Object key) { return (containsKey(key) ? getSet(key).size() : 0); }

    /**
     * add an association between a key and an entry to the map. Note that
     * if the key is already in the map the entry will be added to the set
     * of entries associated with that key. If the key is not in the map, it
     * will be added and the entry will be the only entry in the set associated
     * with it.
     *
     * @param key - the key
     * @param entry - the entry
     * @return - true if the key was already in the map, false if it was not.
     */
    public boolean put(Object key, Object entry) {
        if( map.containsKey(key) ) {
            Set s = (Set)map.get(key);
            s.add(entry);
            return true;
        } else {
            Set s = new HashSet();
            s.add(entry);
            map.put(key, s);
            return false;
        }
    }

    /**
     * Removes a specific association from the data structure. If there are
     * no more entries associated with the key the key is also removed.
     *
     * @param key - the key
     * @param entry - the entry
     * @return - true if the entry was removed from the map, false if it was
     *           not in the map.
     */
    public boolean remove(Object key, Object entry) {
        if( map.containsKey(key) ) {
            Set s = (Set)map.get(key);
            if( s.remove(entry) ) {
                if( s.isEmpty() )
                    map.remove(key);
                return true;
            }
        }
        return false;
    }

    /**
     * removes the key and the entire set of entries associated with it from
     * the map. The set of entries is returned.
     *
     * @param key - the key
     * @return - the set of entries that was previously associated with the
     *           key. null is returned if the key was not in the map
     */
    public Set remove(Object key) {
        return (Set)map.remove(key);
    }
}
