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



public class Liveness {

    private long    lastPing;
    private long    timeout;

    Liveness(long timeout) {
        this.timeout = timeout;
    }

    final synchronized public void setLastPing(long time) {
        lastPing = time;
    }

    final synchronized public boolean isNotTimely(long now) {
        return lastPing+timeout < now;
    }

    final synchronized public void ping() {
        lastPing = System.currentTimeMillis();
    }

    final synchronized public void fail() {
        lastPing = 0;
    }

}
