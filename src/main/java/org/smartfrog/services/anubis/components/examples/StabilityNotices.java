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
import org.smartfrog.services.anubis.locator.AnubisStability;

public class StabilityNotices {

    public class Stability extends AnubisStability {
        @Override
        public void stability(boolean isStable, long timeRef) {
            if (isStable) {
                log.info("****** Partition has stablized with time reference "
                         + timeRef);
            } else {
                log.info("****** Partition is UNSTABLE");
            }
        }
    }

    private AnubisLocator locator = null;
    private AnubisStability stability = null;

    public AnubisLocator getLocator() {
        return locator;
    }

    public void setLocator(AnubisLocator locator) {
        this.locator = locator;
    }

    public void start() {
        stability = new Stability();
        locator.registerStability(stability);
    }

    public void terminate() {
        locator.deregisterStability(stability);
    }
}
