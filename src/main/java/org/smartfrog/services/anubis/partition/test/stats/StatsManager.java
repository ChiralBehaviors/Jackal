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



import java.util.Map;
import java.util.HashMap;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.test.msg.StatsMsg;

public class StatsManager {

    private Scheduling scheduling  = new Scheduling();
    private Map        heartbeat   = new HashMap();

    public StatsManager() {}

    public void schedulingInfo(long time, long delay) {
        scheduling.add(time, delay);
    }

    public void heartbeatInfo(Identity connection, long time, long delay) {

        Heartbeats heartbeatStats = (Heartbeats)heartbeat.get(connection);
        if( heartbeatStats == null ) {
            heartbeatStats = new Heartbeats();
            heartbeat.put(connection, heartbeatStats);
        }

        heartbeatStats.add(time, delay);
    }

    public StatsMsg statsMsg() {
        return new StatsMsg(scheduling.oneMinAve(),
                            scheduling.tenMinAve(),
                            scheduling.oneHourAve(),
                            scheduling.biggest());
    }
}
