/** (C) Copyright 2010 Hal Hildebrand, all rights reserved.

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
 */
package org.smartfrog.services.anubis.partition.diagnostics;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.Status;
import org.smartfrog.services.anubis.partition.diagnostics.msg.IgnoringMsg;
import org.smartfrog.services.anubis.partition.diagnostics.msg.PartitionMsg;
import org.smartfrog.services.anubis.partition.diagnostics.msg.ThreadsMsg;
import org.smartfrog.services.anubis.partition.diagnostics.msg.TimingMsg;
import org.smartfrog.services.anubis.partition.diagnostics.stats.StatsManager;
import org.smartfrog.services.anubis.partition.protocols.partitionmanager.ConnectionSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class Diagnostics {

    private static final long STATSRATE = 5;
    private static final Logger log = Logger.getLogger(Diagnostics.class.getCanonicalName());

    private ConnectionAddress connectionAddress;
    private DiagnosticsServer connectionServer = null;
    private ConnectionSet connectionSet = null;
    private Identity identity;
    private long lastStats = 0;
    private PartitionManager partitionManager = null;
    private StatsManager statistics = new StatsManager();
    private long statsInterval = STATSRATE * 1000; // adjusts with heartbeat
                                                   // timing
    private boolean active = true;

    public Diagnostics(PartitionManager partitionManager, int id)
                                                                 throws IOException,
                                                                 Exception {
        this.partitionManager = partitionManager;
    }

    public void closing(DiagnosticsMessageHandler handler) {
        partitionManager.deregister(handler);
    }

    public ConnectionAddress getAddress() {
        return new ConnectionAddress(connectionServer.getLocalAddress());
    }

    public ConnectionAddress getConnectionAddress() {
        return connectionAddress;
    }

    public DiagnosticsServer getConnectionServer() {
        return connectionServer;
    }

    public ConnectionSet getConnectionSet() {
        return connectionSet;
    }

    public Identity getIdentity() {
        return identity;
    }

    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    public boolean isTestable() {
        return active;
    }

    public void newHandler(DiagnosticsMessageHandler connection) {
        if (connection.connected()) {
            partitionManager.register(connection);
            updateStatus(connection);
            updateTiming(connection);
        }
    }

    public void schedulingInfo(long time, long delay) {
        statistics.schedulingInfo(time, delay);
        updateStats(time);
    }

    public void setConnectionAddress(ConnectionAddress connectionAddress) {
        this.connectionAddress = connectionAddress;
    }

    public void setConnectionServer(DiagnosticsServer cs) {
        connectionServer = cs;
        connectionServer.setDiagnostics(this);
    }

    public void setConnectionSet(ConnectionSet connectionSet) {
        this.connectionSet = connectionSet;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    /**
     * set the nodes to ignore
     * 
     * @param ignoring
     */
    public void setIgnoring(View ignoring) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(String.format("now ignoring: %s", ignoring));
        }
        connectionSet.setIgnoring(ignoring);
        updateIgnoring(ignoring);
    }

    public void setPartitionManager(PartitionManager partitionManager) {
        this.partitionManager = partitionManager;
    }

    public void setTiming(long interval, long timeout) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest(String.format("setting interval: %s, timeout: %s",
                                     interval, timeout));
        }
        connectionSet.setTiming(interval, timeout);
        updateTiming();
        statsInterval = STATSRATE * interval;
    }

    public void setUseDiagnostics(boolean useDiagnostics) {
        active = useDiagnostics;
    }

    @PostConstruct
    public void start() throws IOException {

        if (!active) {
            return;
        }

        if (!active) {
            terminate();
            return;
        }
        connectionServer.connect();
        connectionSet.registerDiagnostics(this);
        connectionServer.start();
    }

    @PreDestroy
    public void terminate() {
        if (active) {
            connectionServer.terminate();
            for (DiagnosticsMessageHandler handler : connectionServer.getOpenHandlers()) {
                handler.shutdown();
            }
        }
    }

    public void updateIgnoring(View ignoring) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("updating console with my ignoring view");
        }
        for (DiagnosticsMessageHandler handler : connectionServer.getOpenHandlers()) {
            updateIgnoring(ignoring, handler);
        }
    }

    public void updateIgnoring(View ignoring, DiagnosticsMessageHandler handler) {
        handler.sendObject(new IgnoringMsg(ignoring));
    }

    public void updateStats(DiagnosticsMessageHandler handler) {
        handler.sendObject(statistics.statsMsg());
    }

    public void updateStats(long timenow) {
        if (lastStats < timenow - statsInterval) {
            if (log.isLoggable(Level.FINEST)) {
                log.finest("updating console with my stats");
            }
            for (DiagnosticsMessageHandler handler : connectionServer.getOpenHandlers()) {
                updateStats(handler);
            }
            lastStats = timenow;
        }
    }

    public void updateStatus(DiagnosticsMessageHandler handler) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("updating console with my partition status");
        }
        Status status = partitionManager.getStatus();
        handler.sendObject(new PartitionMsg(status.view, status.leader));
    }

    public void updateThreads(DiagnosticsMessageHandler handler) {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("updating console with my thead statuses");
        }
        String status = connectionSet.getThreadStatusString();
        handler.sendObject(new ThreadsMsg(status));
    }

    public void updateTiming() {
        if (log.isLoggable(Level.FINEST)) {
            log.finest("updating console with my timing settings");
        }
        for (DiagnosticsMessageHandler handler : connectionServer.getOpenHandlers()) {
            updateTiming(handler);
        }
    }

    public void updateTiming(DiagnosticsMessageHandler handler) {
        handler.sendObject(new TimingMsg(connectionSet.getInterval(),
                                         connectionSet.getTimeout()));
    }
}
