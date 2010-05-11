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
package org.smartfrog.services.anubis.components.examples;

import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.AnubisProvider;

public class AnubisPrim {

    private AnubisLocator locator = null;
    private boolean marshallValues = false;
    private AnubisProvider provider = null;
    protected String name = null;

    public AnubisPrim() throws Exception {
        super();
    }

    public AnubisLocator getLocator() {
        return locator;
    }

    public String getName() {
        return name;
    }

    public boolean isMarshallValues() {
        return marshallValues;
    }

    public void setLocator(AnubisLocator locator) {
        this.locator = locator;
    }

    public void setMarshallValues(boolean marshallValues) {
        this.marshallValues = marshallValues;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void start() {

        AnubisProvider.setMarshallValues(marshallValues);
        provider = new AnubisProvider(name);
        provider.setValue("deployed");
        locator.registerProvider(provider);
        provider.setValue("started");
    }

    public void terminate() {
        locator.deregisterProvider(provider);
    }

}
