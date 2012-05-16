/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.jackal.partition.test.node;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.Status;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.test.msg.IgnoringMsg;
import org.smartfrog.services.anubis.partition.test.msg.PartitionMsg;
import org.smartfrog.services.anubis.partition.test.msg.ThreadsMsg;
import org.smartfrog.services.anubis.partition.test.msg.TimingMsg;
import org.smartfrog.services.anubis.partition.test.stats.StatsManager;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;

import com.hellblazer.pinkie.CommunicationsHandler;
import com.hellblazer.pinkie.CommunicationsHandlerFactory;
import com.hellblazer.pinkie.ServerSocketChannelHandler;
import com.hellblazer.pinkie.SocketOptions;

/**
 * 
 * @author hhildebrand
 * 
 */
public class ControllerAgent {
    private class TestConnectionFactory implements CommunicationsHandlerFactory {
        @Override
        public CommunicationsHandler createCommunicationsHandler(SocketChannel channel) {
            ControllerConnection connection = new ControllerConnection(
                                                                       ControllerAgent.this,
                                                                       wireSecurity);
            connections.add(connection);
            partitionManager.register(connection);
            return connection;
        }
    }

    private static final long                STATISTICS_RATE    = 5;

    private final Set<ControllerConnection>  connections        = new CopyOnWriteArraySet<ControllerConnection>();
    private ConnectionSet                    connectionSet      = null;
    private volatile long                    lastStatistics     = 0;
    private final PartitionManager           partitionManager;
    private final StatsManager               statistics         = new StatsManager();
    private volatile long                    statisticsInterval = STATISTICS_RATE * 1000;
    private final ServerSocketChannelHandler handler;
    private final WireSecurity               wireSecurity;

    public ControllerAgent(InetSocketAddress endpoint,
                           PartitionManager partitionManager, int id,
                           ConnectionSet connectionSet,
                           SocketOptions socketOptions,
                           WireSecurity wireSecurity, ExecutorService commsExec)
                                                                                throws IOException,
                                                                                Exception {
        this.partitionManager = partitionManager;
        handler = new ServerSocketChannelHandler(
                                                 String.format("Partition Manager Test Node %s - connection server",
                                                               id),
                                                 socketOptions, endpoint,
                                                 commsExec,
                                                 new TestConnectionFactory());
        this.connectionSet = connectionSet;
        this.wireSecurity = wireSecurity;
    }

    public void closing(ControllerConnection connection) {
        partitionManager.deregister(connection);
        connections.remove(connection);
    }

    public InetSocketAddress getAddress() {
        return handler.getLocalAddress();
    }

    public void schedulingInfo(long time, long delay) {
        statistics.schedulingInfo(time, delay);
        updateStats(time);
    }

    /**
     * set the nodes to ignore
     * 
     * @param ignoring
     */
    public void setIgnoring(View ignoring) {
        connectionSet.setIgnoring(ignoring);
        updateIgnoring(ignoring);
    }

    public void setTiming(long interval, long timeout) {
        connectionSet.setTiming(interval, timeout);
        updateTiming();
        statisticsInterval = STATISTICS_RATE * interval;
    }

    @PostConstruct
    public void start() throws IOException {
        handler.start();
        connectionSet.registerController(this);
    }

    @PreDestroy
    public void terminate() {
        handler.terminate();
    }

    public void updateHeartbeat(Heartbeat hb) {
        HeartbeatMsg heartbeatMsg = HeartbeatMsg.toHeartbeatMsg(hb);
        for (ControllerConnection tc : connections) {
            tc.send(heartbeatMsg);
        }
    }

    public void updateIgnoring(View ignoring) {
        Iterator<ControllerConnection> iter = connections.iterator();
        while (iter.hasNext()) {
            updateIgnoring(ignoring, iter.next());
        }
    }

    public void updateIgnoring(View ignoring, ControllerConnection tc) {
        tc.sendObject(new IgnoringMsg(ignoring));
    }

    public void updateStats(ControllerConnection connection) {
        connection.sendObject(statistics.statsMsg());
    }

    public void updateStats(long timenow) {
        if (lastStatistics < timenow - statisticsInterval) {
            Iterator<ControllerConnection> iter = connections.iterator();
            while (iter.hasNext()) {
                updateStats(iter.next());
            }
            lastStatistics = timenow;
        }
    }

    public void updateStatus(ControllerConnection connection) {
        Status status = partitionManager.getStatus();
        connection.sendObject(new PartitionMsg(status.view, status.leader));
    }

    public void updateThreads(ControllerConnection connection) {
        String status = connectionSet.getThreadStatusString();
        connection.sendObject(new ThreadsMsg(status));
    }

    public void updateTiming() {
        Iterator<ControllerConnection> iter = connections.iterator();
        while (iter.hasNext()) {
            updateTiming(iter.next());
        }
    }

    public void updateTiming(ControllerConnection connection) {
        connection.sendObject(new TimingMsg(connectionSet.getInterval(),
                                            connectionSet.getTimeout()));
    }

    /**
     * @return
     */
    public Object getId() {
        // TODO Auto-generated method stub
        return connectionSet.getId();
    }

}
