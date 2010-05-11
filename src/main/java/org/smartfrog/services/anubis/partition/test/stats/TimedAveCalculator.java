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



import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

public class TimedAveCalculator implements AveCalculator {

    public class Entry {
        public long time;
        public long delay;
        public Entry(long time, long delay) {
            this.time = time;
            this.delay = delay;
        }
    }

    private List    entries     = new LinkedList();
    private long    count       = 0;
    private long    accumulator = 0;
    private long    period      = 0;
    private long    biggest     = 0;
    private boolean complete    = false;

    public TimedAveCalculator(long period) {
        this.period = period;
    }

    public void add(long time, long delay) {
        entries.add(new Entry(time, delay));
        if( delay > biggest ) biggest = delay;
        count++;
        accumulator+=delay;
        Iterator iter = entries.iterator();
        while(iter.hasNext()) {
            Entry earliest = (Entry)iter.next();
            if( earliest.time >= time-period ) break;
            count--;
            accumulator-=earliest.delay;
            iter.remove();
            complete = true;
        }
    }


    public long average() {
        return (count==0 ? 0 : accumulator/count);
    }

    public boolean isComplete() { return complete; }

    public long biggestEver() { return biggest; }

    public long biggest() {
        long result = 0;
        Iterator iter = entries.listIterator();
        while( iter.hasNext() ) {
            long current = ((Entry)iter.next()).delay;
            if( current > result ) result = current;
        }
        return result;
    }


}
