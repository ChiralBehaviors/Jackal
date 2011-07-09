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
package org.smartfrog.services.anubis.partition.comms.nonblocking;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class RxQueue<E> {

    private final AtomicBoolean    open;
    private final BlockingQueue<E> thisQueue;

    /**
     * Queue where received serialized objects are put on. Worker threads empty
     * that queue and deliver the object to anubis
     */
    public RxQueue() {
        thisQueue = new LinkedBlockingQueue<E>();
        open = new AtomicBoolean(true);
    }

    /**
     * method to add a new rx serialized object to the queue this method
     * notifies sleeping worker threads there is a job to deliver
     * 
     * @param objectToAdd
     *            rx object
     */
    public void add(E objectToAdd) {
        if (open.get()) {
            thisQueue.add(objectToAdd);
        }
    }

    /**
     * method used to check if queue is empty
     * 
     * @return boolean value, true if queue is empty, false otherwise
     */
    public boolean isEmpty() {
        return thisQueue.isEmpty();
    }

    /**
     * indicates if the queue is open
     * 
     * @return true iff the queue is open
     */
    public boolean isOpen() {
        return open.get();
    }

    /**
     * get teh next item in the queue; blocking if the queue is empty.
     * 
     * @return the next item or null if the queue is closed.
     */
    public E next() {
        if (open.get()) {
            try {
                return thisQueue.take();
            } catch (InterruptedException e) {
                throw new IllegalStateException("Interrupted", e);
            }
        }
        return null;
    }

    /**
     * shutdown the queue
     */
    public void shutdown() {
        open.set(false);
        thisQueue.clear();
    }

}
