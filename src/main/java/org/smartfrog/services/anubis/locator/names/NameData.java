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


import java.io.Serializable;

public class NameData implements Serializable {

    public String        name;
    public Integer       node;

    public NameData(String name, Integer node) {
        this.name     = name;
        this.node     = node;
    }

    /**
     * Two name data are equivalent if they have the same name and the
     * same node. This is because there may be multiple proxies for a
     * particular name, but only one per node.
     *
     * @param obj
     * @return boolean
     */
    public boolean equals(Object obj) {
        if( obj instanceof NameData )
            return ( name.equals( ((NameData)obj).name) &&
                     node.equals( ((NameData)obj).node) );
        else
            return false;
    }

    public int hashCode() {
        return name.hashCode() + node.hashCode();
    }
}
