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


import java.util.Iterator;
import java.util.Set;

public class ActiveTimeQueue extends Thread {

    TimeQueue queue = new TimeQueue();

    private boolean running = false;
    private long    wakeup  = 0;

    public ActiveTimeQueue(String threadName) {
        super(threadName);
    }

    /**
     * The worker thread. This thread is the timer that wakes up to
     * expire items on the queue as they reach their queued time.
     *
     * The getExpiredOrBlock() method synchronizes on the queue and removes
     * items to be expired before releasing the monitor. So doExpirations()
     * operates on a different data structure. This gives two benefits:
     * 1) the add() and remove() methods are guaranteed to progress, because
     *    getExpiredOrBlock() will not block them for long. Therefore calling
     *    the expired() method on the dequeued elements will not deadlock if
     *    those elements happen to have another thread calling add() or remove().
     * 2) An element can call add() or remove() in the implementation of its
     *    expired() method without causing a ConcurrentModificationException
     *    to be thrown on the queue data structure.
     */
    public void run() {
        running = true;
        while(running) {

            Set expiredElements = getExpiredOrBlock();
            if( expiredElements != null )
                doExpirations(expiredElements);

        }
    }

    /**
     * Stop the worker thread and empty the queue.
     */
    public void terminate() {
        synchronized( queue ) {
            running = false;
            queue.clear();
            queue.notifyAll();
        }
    }

    public Set getExpiredOrBlock() {

        /**
         * synchronized on the queue.
         */
        synchronized( queue ) {

            /**
             * Could have been terminated immediately before entering this
             * synchronized method. If so return null (drop out!)
             */
            if( !running )
                return null;

            /**
             * If there are no items block forever (until woken up).
             */
            if( queue.isEmpty() ) {
                try { queue.wait(0); }
                catch (InterruptedException ex) { }
                return null;
            }

            /**
             * get timing information.
             */
            long timeNow  = System.currentTimeMillis();
            Long key      = (Long)queue.firstKey();
            long nextTime = key.longValue();

            /**
             * If no items to expire then sleep until the top one expires or
             * we get woken up.
             */
            if( nextTime > timeNow ) {
                try { queue.wait(nextTime - timeNow); }
                catch (InterruptedException ex) { }
                return null;
            }

            /**
             * If there are items to expire then remove them from the queue
             * and return them.
             */
            return queue.remove(key);
        }
    }


    /**
     * Iterate through the set of expired elements and call their
     * expired() method.
     *
     * @param timeNow
     */
    private void doExpirations(Set expiredElements) {
        Iterator iter = expiredElements.iterator();
        while( iter.hasNext() )
            ((TimeQueueElement)iter.next()).expired();
    }


    /**
     * Add an element to the queue. Notify the worker thread in case this
     * element has been added to the top of the queue and the worker has to
     * wake up earlier.
     *
     * @param element
     * @param time
     * @return boolean
     */
    public boolean add(TimeQueueElement element, long time) {
        synchronized( queue ) {
            if( queue.add(element, time) ) {
                queue.notify();
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Remove an element from the queue - don't notify, the worst that can
     * happen is the worker thread can wake with nothing to do, so why wake it
     * now?
     *
     * @param element
     * @return boolean
     */
    public boolean remove(TimeQueueElement element) {
        synchronized( queue ) {
            return queue.remove(element);
        }
    }
}
