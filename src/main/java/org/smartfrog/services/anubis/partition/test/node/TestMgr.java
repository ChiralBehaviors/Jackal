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
package org.smartfrog.services.anubis.partition.test.node;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.Status;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.test.msg.IgnoringMsg;
import org.smartfrog.services.anubis.partition.test.msg.PartitionMsg;
import org.smartfrog.services.anubis.partition.test.msg.ThreadsMsg;
import org.smartfrog.services.anubis.partition.test.msg.TimingMsg;
import org.smartfrog.services.anubis.partition.test.stats.StatsManager;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;

public class TestMgr {

    private Set connections = new HashSet();
    private PartitionManager partitionManager = null;
    private TestServer connectionServer = null;
    private ConnectionSet connectionSet = null;
    private StatsManager statistics = new StatsManager();
    private static final long STATSRATE = 5;
    private long statsInterval = STATSRATE * 1000; // adjusts with heartbeat timing
    private long lastStats = 0;
    private boolean testable = true;
    private ConnectionAddress connectionAddress;
    private Identity identity;

    public TestMgr(String host, int port, PartitionManager partitionManager,
                   int id) throws IOException, Exception {
        this.partitionManager = partitionManager;
        String threadName = "Anubis: Partition Manager Test Node (node " + id
                            + ") - connection server";
        connectionServer = new TestServer(this, host, port, threadName);
    }

    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    public void setPartitionManager(PartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    public ConnectionAddress getConnectionAddress() {
        return connectionAddress;
    }

    public void setConnectionAddress(ConnectionAddress connectionAddress) {
        this.connectionAddress = connectionAddress;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public boolean isTestable() {
        return testable;
    }

    public void setTestable(boolean testable) {
        this.testable = testable;
    }

    public ConnectionSet getConnectionSet() {
        return connectionSet;
    }

    public void setConnectionSet(ConnectionSet connectionSet) {
        this.connectionSet = connectionSet;
    }

    public void start() throws IOException {

        if (!testable) {
            return;
        }

        String threadName = "Anubis: Partition Manager Test Node (node "
                            + identity.id + ") - connection server";
        connectionServer = new TestServer(
                                          this,
                                          connectionAddress.ipaddress.getHostName(),
                                          connectionAddress.port, threadName);

        if (!testable) {
            terminate();
            return;
        }
        connectionSet.registerTestManager(this);
        connectionServer.start();
    }

    public void terminate() {
        if (testable) {
            connectionServer.shutdown();
            Iterator iter = connections.iterator();
            while (iter.hasNext()) {
                ((TestConnection) iter.next()).shutdown();
            }
        }
    }

    public void newConnection(SocketChannel channel) {
        String threadName = "Anubis: Partition Manager Test Node (node "
                            + partitionManager.getId() + ") - connection";
        TestConnection connection = new TestConnection(channel, this,
                                                       threadName);
        if (connection.connected()) {
            synchronized (connections) {
                connections.add(connection);
            }
            partitionManager.register(connection);
            updateStatus(connection);
            updateTiming(connection);
            connection.start();
        }
    }

    public void closing(TestConnection connection) {
        partitionManager.deregister(connection);
        synchronized (connections) {
            connections.remove(connection);
        }
    }

    public ConnectionAddress getAddress() {
        return connectionServer.getAddress();
    }

    public void updateStatus(TestConnection tc) {
        Status status = partitionManager.getStatus();
        tc.sendObject(new PartitionMsg(status.view, status.leader));
    }

    public void updateTiming() {
        Iterator iter = connections.iterator();
        while (iter.hasNext())
            updateTiming((TestConnection) iter.next());
    }

    public void updateTiming(TestConnection tc) {
        tc.sendObject(new TimingMsg(connectionSet.getInterval(),
                                    connectionSet.getTimeout()));
    }

    public void setTiming(long interval, long timeout) {
        connectionSet.setTiming(interval, timeout);
        updateTiming();
        statsInterval = STATSRATE * interval;
    }

    public void updateStats(long timenow) {
        if (lastStats < (timenow - statsInterval)) {
            Iterator iter = connections.iterator();
            while (iter.hasNext())
                updateStats((TestConnection) iter.next());
            lastStats = timenow;
        }
    }

    public void updateStats(TestConnection tc) {
        tc.sendObject(statistics.statsMsg());
    }

    public void updateThreads(TestConnection tc) {
        String status = connectionSet.getThreadStatusString();
        tc.sendObject(new ThreadsMsg(status));
    }

    public void schedulingInfo(long time, long delay) {
        statistics.schedulingInfo(time, delay);
        updateStats(time);
    }

    /**
     * set the nodes to ignore
     * @param ignoring
     */
    public void setIgnoring(View ignoring) {
        connectionSet.setIgnoring(ignoring);
        updateIgnoring(ignoring);
    }

    public void updateIgnoring(View ignoring) {
        Iterator iter = connections.iterator();
        while (iter.hasNext())
            updateIgnoring(ignoring, (TestConnection) iter.next());
    }

    public void updateIgnoring(View ignoring, TestConnection tc) {
        tc.sendObject(new IgnoringMsg(ignoring));
    }

}
