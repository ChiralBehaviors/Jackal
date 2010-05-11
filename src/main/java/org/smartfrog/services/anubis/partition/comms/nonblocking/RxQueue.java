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


import java.util.Vector;

public class RxQueue {

    private Vector thisQueue = null;
    private boolean open;

    /**
     * Queue where received serialized objects are put on.
     * Worker threads empty that queue and deliver the object
     * to anubis
     */
    public RxQueue(){
	    thisQueue = new Vector(10);
        open = true;
    }
    
    /**
     * shutdown the queue
     */
    public synchronized void shutdown() {
        open = false;
        thisQueue.clear();
        notifyAll();
    }
    
    /**
     * indicates if the queue is open
     * 
     * @return true iff the queue is open
     */
    public synchronized boolean isOpen() {
        return open;
    }

    /**
     * method to add a new rx serialized object to the queue
     * this method notifies sleeping worker threads there is a job
     * to deliver
     *
     * @param objectToAdd rx object
     */
    public synchronized void add(Object objectToAdd){
        if( open ) {
            thisQueue.add(objectToAdd);
            notifyAll();
        }
    }

    /**
     * get teh next item in the queue; blocking if the queue is empty.
     * @return the next item or null if the queue is closed.
     */
    public synchronized Object next(){
        while( open) {
            if( thisQueue.isEmpty() ) {
                try { 
                    wait(); 
                } catch (InterruptedException e) { 
                    // do nothing
                }
            } else {
                return thisQueue.remove(0);
            }
        }
        return null;
    }

    /**
     * method used to check if queue is empty
     *
     * @return boolean value, true if queue is empty, false otherwise
     */
    public synchronized boolean isEmpty(){
        return thisQueue.isEmpty();
    }

}
