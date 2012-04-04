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

import java.rmi.RemoteException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.locator.AnubisListener;
import org.smartfrog.services.anubis.locator.AnubisValue;
import org.smartfrog.services.anubis.locator.names.ProviderInstance;

public class SPListenerAdapterImpl extends AnubisListener {
    private final static Logger log = LoggerFactory.getLogger(SPListenerAdapterImpl.class.getCanonicalName());

    private SPListener          listener;

    public SPListenerAdapterImpl(String name, SPListener listener) {
        super(name);
        this.listener = listener;
    }

    /**
     * Upcalls for to newValue() are never generated - exception if done
     * 
     * @param value
     */
    @Override
    public void newValue(AnubisValue value) {
        Exception thrown = new Exception();
        thrown.fillInStackTrace();
        log.error("newValue() should not be called here", thrown);
    }

    /**
     * newValue() passes through to the sub-process listener
     * 
     * @param i
     */
    @Override
    public synchronized void newValue(ProviderInstance i) {
        try {
            listener.newValue(i);
        } catch (RemoteException ex) {

        }
    }

    /**
     * Upcalls for to removeValue() are never generated - exception if done
     * 
     * @param value
     */
    @Override
    public void removeValue(AnubisValue value) {
        Exception thrown = new Exception();
        thrown.fillInStackTrace();
        log.error("newValue() should not be called here", thrown);
    }

    /**
     * removeValue() passes through to the sub-process listener
     * 
     * @param i
     */
    @Override
    public synchronized void removeValue(ProviderInstance i) {
        try {
            listener.removeValue(i);
        } catch (RemoteException ex) {
        }
    }

    /**
     * removeValue() passes through to the sub-process listener
     * 
     * @param i
     */
    @Override
    public synchronized void removeValue(ProviderInstance i, long time) {
        try {
            listener.removeValue(i, time);
        } catch (RemoteException ex) {
        }
    }
}