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
package org.smartfrog.services.anubis.locator.names;

/**
 * <p>Title: </p>
 * <p>Description: The ListenerProxy is a proxy representation of registered
 *                 AnubisListener objects. A single ListenerProxy instance
 *                 represents all instances of AnubisListeners with the same
 *                 name registered at the same node. A local register will
 *                 register interest in a named provider (i.e. indicate that it
 *                 has one or more AnubisListeners that are interested in the
 *                 named provider) by registering a ListenerProxy object. </p>
 *
 *
 */
import java.io.Serializable;



public class ListenerProxy extends NameData implements Serializable {

    private             long uniqueRegId;
    public static final long undefinedRegId = -1;

    public ListenerProxy(String name, Integer node, long uniqueRegId) {
        super(name, node);
        this.uniqueRegId = uniqueRegId;
    }


    /**
     * returns true if this class has a uniqueRegId that preceeds that of the
     * paramter. Returns false otherwise. Note that the equals() method for
     * this class does not refer to the uniqueRegId attribute - two listener
     * proxies with different ids are considered the same.
     *
     * @param lp ListenerProxy
     * @return boolean
     */
    public boolean uridPreceeds(ListenerProxy lp) {
        return (lp != null) && (this.uniqueRegId < lp.uniqueRegId);
    }

    /**
     * returns true if this class has a uniqueRegId that equals that of the
     * paramter. Returns false otherwise. Note that the equals() method for
     * this class does not refer to the uniqueRegId attribute - two listener
     * proxies with different ids are considered the same.
     *
     * @param lp ListenerProxy
     * @return boolean
     */
    public boolean uridEquals(ListenerProxy lp) {
        return (lp != null) && (this.uniqueRegId == lp.uniqueRegId);
    }

    public String toString() {
        return "ListenerProxy [" + name +
                ", node=" + node + ", urid=" + uniqueRegId + "]";
    }
}
