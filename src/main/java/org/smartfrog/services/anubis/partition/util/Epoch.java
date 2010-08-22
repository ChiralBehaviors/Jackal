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
package org.smartfrog.services.anubis.partition.util;

/**
 * The Epoch.
 * we need an epoch that monotonically increases to identify
 * incarnations of a partition manager. Previously we used a value
 * stored on stable storage, but then we wanted to be able to refresh
 * the storage with a blank image, so we chose time instead.
 * Assumes no two partition managers with the same name are started
 * within the clock resolution. Also if started on different machines,
 * then the epochs must still be monotonically increasing.
 */
public class Epoch {

    private long value;

    public Epoch() {
        value = System.currentTimeMillis();
    }

    /**
     * perform a one-time, on-demand value resolution.
     */
    public long longValue() {
        return value;
    }

}
