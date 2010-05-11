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
package org.smartfrog.services.anubis.locator.util;



import java.util.LinkedList;

public class BlockingQueue {

    private LinkedList queue = new LinkedList();
    private boolean    active = true;

    /**
     * Constructor
     */
    public BlockingQueue() { }

    /**
     * Add an object to the queue - objects are added to the end.
     * When the object ahs been added any threads waiting on the queue
     * will be notified. Hence the get() method that blocks when the queue
     * is empty will be notified and able to get the added element.
     *
     * @param obj
     */
    public synchronized void put(Object obj) {
        queue.addLast(obj);
        notifyAll();
    }

    /**
     * get an object from the queue. Objects are retrieved from the front
     * of the queue. If the queue is empty this method will block until
     * an object is added or the queue it terminated.
     *
     * @return an object from the queue or null if the queue is terminated
     */
    public synchronized Object get() {
        Object result = null;
        while( active && (result == null) ) {
            if( queue.isEmpty() ) {
                try { wait(); }
                catch (InterruptedException ex) { }
            } else {
                result = queue.removeFirst();
            }
        }
        return result;
    }

    /**
     * empty the queue
     */
    public synchronized void clear() {
        queue.clear();
    }

    /**
     * If the queue is active, clear the queue, unblock any waiting threads and
     * indicate that the queue is not active.
     * If the queue is not active do nothing.
     */
    public synchronized void deactivate() {
        if( active ) {
            clear();
            active = false;
            notifyAll();
        }
    }

    /**
     * If the queue is not active then clear the queue and set it to active.
     * If the queue is already active do nothing.
     */
    public synchronized void activate() {
        if( !active ) {
            clear();
            active = true;
        }
    }
}
