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
package org.smartfrog.services.anubis.components.examples;

import java.net.InetAddress;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.partition.Partition;
import org.smartfrog.services.anubis.partition.PartitionNotification;
import org.smartfrog.services.anubis.partition.views.View;

public class PartitionNotices implements PartitionNotification {

    private boolean isStable = false;
    private Partition partition = null;

    public PartitionNotices() throws Exception {
        super();
    }

    public Partition getPartition() {
        return partition;
    }

    @Override
    public void objectNotification(Object obj, int sender, long time) {
        return;
    }

    @Override
    public void partitionNotification(View view, int leader) {
        if (view.isStable() != isStable) {
            isStable = view.isStable();
            if (isStable) {
                InetAddress leaderAddr = partition.getNodeAddress(leader);
                System.out.println("****** Partition has stablized with "
                                   + view.cardinality() + " members");
                System.out.println("****** The leader is node " + leader
                                   + " at " + leaderAddr.toString());
            } else {
                System.out.println("****** Partition is UNSTABLE");
            }
        }
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    @PostConstruct
    public void start() {
        partition.register(this);
    }

    @PreDestroy
    public void terminate() {
        partition.deregister(this);
    }
}
