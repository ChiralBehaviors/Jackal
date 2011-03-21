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
package org.smartfrog.services.anubis.partition.test.stats;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TimedAveCalculator implements AveCalculator {

    public static class Entry {
        public long delay;
        public long time;

        public Entry(long time, long delay) {
            this.time = time;
            this.delay = delay;
        }
    }

    private long        accumulator = 0;
    private long        biggest     = 0;
    private boolean     complete    = false;
    private long        count       = 0;
    private List<Entry> entries     = new LinkedList<Entry>();
    private long        period      = 0;

    public TimedAveCalculator(long period) {
        this.period = period;
    }

    @Override
    public void add(long time, long delay) {
        entries.add(new Entry(time, delay));
        if (delay > biggest) {
            biggest = delay;
        }
        count++;
        accumulator += delay;
        Iterator<Entry> iter = entries.iterator();
        while (iter.hasNext()) {
            Entry earliest = iter.next();
            if (earliest.time >= time - period) {
                break;
            }
            count--;
            accumulator -= earliest.delay;
            iter.remove();
            complete = true;
        }
    }

    @Override
    public long average() {
        return count == 0 ? 0 : accumulator / count;
    }

    @Override
    public long biggest() {
        long result = 0;
        Iterator<Entry> iter = entries.listIterator();
        while (iter.hasNext()) {
            long current = iter.next().delay;
            if (current > result) {
                result = current;
            }
        }
        return result;
    }

    @Override
    public long biggestEver() {
        return biggest;
    }

    @Override
    public boolean isComplete() {
        return complete;
    }

}
