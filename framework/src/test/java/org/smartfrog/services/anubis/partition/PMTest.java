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
package org.smartfrog.services.anubis.partition;

import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.views.View;

public class PMTest implements PartitionNotification {

    static class Tester extends Thread {
        Partition partition = null;
        int       remote    = 0;

        Tester(Partition p, int r) {
            super("Test Driver");
            partition = p;
            remote = r;
        }

        @Override
        public void run() {
            MessageConnection c = partition.connect(remote);
            c.sendObject("Hello World");
            c.sendObject("Goodbye World");
            // c.disconnect();
        }
    }

    boolean   done      = false;
    Partition partition = null;
    String    name      = null;
    int       remote    = -1;

    public PMTest() {
        System.out.println("Created test");
    }

    public String getName() {
        return name;
    }

    public Partition getPartition() {
        return partition;
    }

    public int getRemote() {
        return remote;
    }

    @Override
    public void objectNotification(Object obj, int node, long time) {
        System.out.println(name + "Test: received object " + obj);
    }

    @Override
    public void partitionNotification(View view, int leader) {
        System.out.println(name + "Test: notification " + view + " leader is "
                           + leader);
        if (!done && view.contains(remote)) {
            done = true;
            new Tester(partition, remote).start();
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    public void setRemote(int remote) {
        this.remote = remote;
    }

    public void start() {
        partition.register(this);
        if (remote == -1) {
            done = true;
        }
        System.out.println(name + "Started test");
    }

    public void terminate() {
        System.out.println(name + "Terminating test");
    }

}
