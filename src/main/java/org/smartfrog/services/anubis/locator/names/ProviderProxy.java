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
 * <p>Description: The ProviderProxy is a proxy representation of registered
 *                 AnubisProvider objects. Providers are supposed to be unique
 *                 within a system (i.e. each has a unique name) so a
 *                 ProviderProxy represents exactly one provider. A local
 *                 register will indicate that it has an AnubisProvider
 *                 registered with it by registering a ProviderProxy object</p>
 *
 *                 <p>Note that a ProviderProxy inherits the NameData equals()
 *                 method - so ProxyProviders are equivalent if they have the
 *                 same name. So if two AnubisProviders register with the same
 *                 name at two different local registers, their ProviderProxies
 *                 will be judged as equal.
 *
 */
import java.io.Serializable;

public class ProviderProxy extends NameData implements Serializable {

    public ProviderProxy(String name, Integer node) {
        super(name, node);
    }

    public String toString() {
            return "ProviderProxy [" + name +
                    ", on node=" + node + "]";
    }
}
