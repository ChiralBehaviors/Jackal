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
package org.smartfrog.services.anubis.locator;

/**
 * <p>Title: </p>
 * <p>Description: The main API to the Anubis Locator. The locator recognises
 *                 "provider" and "listener" objects defined by the user. A
 *                 provider object has a name and either exists in the system
 *                 or does not - in this case "exists" means it has registered
 *                 with the locator. A listener object receives notifications
 *                 to indicate the current status of a provider, where the
 *                 status is "present" or "absent".
 * </p>
 * <p>             The user defines providers and listeners by extending the
 *                 AnubisProvider and AnubisListener base classes and registering
 *                 them using this API.
 */
import org.smartfrog.services.anubis.locator.util.ActiveTimeQueue;

public interface AnubisLocator {
    /**
     * register a provider. This is a non-blocking operation. A provider
     * should have a unique name in the system. Uniqueness is not currently
     * tested by the locator. This could be changed to a blocking operation
     * that does test uniqueness and passes or fails. It has not been decided
     * how to deal with clashes when partitions merge.
     */
    public void    registerProvider(AnubisProvider provider);
    /**
     * deregister a provider - non-blocking.
     */
    public void    deregisterProvider(AnubisProvider provider);
    /**
     * register a listener - this is a non-blocking method. It does not
     * return a value, instead a notification will be provided to indicate
     * the presence or absence of a provider. There is no guarantee that the
     * registration call will return before the notification is given as the
     * notification is asynchronous. If the partition is stable the
     * notification will arrive with the usual timing guarantees, if not there
     * are no guarantees.
     */
    public void registerListener(AnubisListener listener);
    /**
     * deregister a listener - non-blocking.
     */
    public void deregisterListener(AnubisListener listener);
    /**
     * registerStability registers an interface for stability notifications.
     * This interface is called to inform the user when the local partition
     * becomes stable or unstable.
     */
    public void registerStability(AnubisStability stability);
    /**
     * deregisterStability deregisters a stability notification iterface.
     */
    public void deregisterStability(AnubisStability stability);
    /**
     * Indicates that the provider has a new value
     * @param provider
     */
    public void newProviderValue(AnubisProvider provider);
    /**
     * Providers access to a timer queue - this is a utility for users that
     * are building on the AnubisLocator interface that will order their
     * timers with those used by Anubis as well as provide a general timer
     * facility.
     *
     * @return ActiveTimeQueue
     */
    public ActiveTimeQueue getTimeQueue();
    /**
     * Returns the upper bound on communication delay.
     * @return long
     */
    public long getmaxDelay();
}
