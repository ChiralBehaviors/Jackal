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
package org.smartfrog.services.anubis.locator.subprocess;

import java.util.HashMap;
import java.util.Map;

import org.smartfrog.services.anubis.locator.subprocess.SPLocatorAdapterImpl.Provider;

public class SPLocatorData {

    private Liveness                                 liveness;
    private Map<String, Provider>                    providers   = new HashMap<String, Provider>();
    private Map<SPListener, SPListenerAdapterImpl>   listeners   = new HashMap<SPListener, SPListenerAdapterImpl>();
    private Map<SPStability, SPStabilityAdapterImpl> stabilities = new HashMap<SPStability, SPStabilityAdapterImpl>();

    SPLocatorData(long timeout) {
        liveness = new Liveness(timeout);
        liveness.ping();
    }

    public Map<SPListener, SPListenerAdapterImpl> getListeners() {
        return listeners;
    }

    public Liveness getLiveness() {
        return liveness;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public Map<SPStability, SPStabilityAdapterImpl> getStabilities() {
        return stabilities;
    }

    void clear() {
        liveness = null;
        providers.clear();
        providers = null;
        listeners.clear();
        listeners = null;
        stabilities.clear();
        stabilities = null;
    }
}
