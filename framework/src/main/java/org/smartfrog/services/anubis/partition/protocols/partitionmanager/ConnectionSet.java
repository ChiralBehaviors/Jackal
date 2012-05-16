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
package org.smartfrog.services.anubis.partition.protocols.partitionmanager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.comms.Connection;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServer;
import org.smartfrog.services.anubis.partition.comms.IOConnectionServerFactory;
import org.smartfrog.services.anubis.partition.comms.MessageConnection;
import org.smartfrog.services.anubis.partition.comms.SelfConnection;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsIntf;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatConnection;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocol;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
import org.smartfrog.services.anubis.partition.protocols.leader.Candidate;
import org.smartfrog.services.anubis.partition.protocols.leader.LeaderMgr;
import org.smartfrog.services.anubis.partition.protocols.leader.LeaderProtocolFactory;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.views.ViewListener;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

import com.hellblazer.jackal.partition.test.node.ControllerAgent;

/**
 * Anubis Detection Service.
 * 
 * ConnectionSet is the heart of the protocol. This object maintains the local
 * view of the world as a set of connections that it recognises as valid with
 * other nodes. This connection set is the base information that the partition
 * manager uses to determine a valid partition.
 * 
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Hewlett-Packard Ltd.
 * </p>
 * 
 * @author Paul Murray
 * @version 1.0
 */
public class ConnectionSet implements ViewListener, ConnectionManager {
    private static final Logger             log                 = LoggerFactory.getLogger(ConnectionSet.class.getCanonicalName());

    private final AtomicBoolean             changeInViews       = new AtomicBoolean(
                                                                                    false);
    private final Map<Identity, Connection> connections         = new HashMap<Identity, Connection>();
    private final IOConnectionServer        connectionServer;
    private final BitView                   connectionView      = new BitView();
    private volatile ControllerAgent        agent;
    private final Heartbeat                 heartbeat;
    private final HeartbeatCommsIntf        heartbeatComms;
    private volatile long                   heartbeatInterval   = 0;
    private final HeartbeatProtocolFactory  heartbeatProtocolFactory;
    private final Identity                  identity;
    private volatile View                   ignoring            = new BitView();
    private final IntervalExec              intervalExec;
    private final LeaderMgr                 leaderMgr;
    private final LeaderProtocolFactory     leaderProtocolFactory;
    private final Set<Connection>           msgConDelayedDelete = new HashSet<Connection>();
    private final Set<MessageConnection>    msgConnections      = new HashSet<MessageConnection>();
    private final NodeIdSet                 msgLinks            = new NodeIdSet();
    private final PartitionProtocol         partitionProtocol;
    private volatile long                   quiesce             = 0;
    private final AtomicBoolean             sendingHeartbeats   = new AtomicBoolean(
                                                                                    false);
    private volatile long                   stability           = 0;
    private final AtomicBoolean             stablizing          = new AtomicBoolean(
                                                                                    false);
    private volatile boolean                terminated          = false;
    private volatile long                   timeout             = 0;
    private volatile long                   viewNumber          = 0;

    public ConnectionSet(InetSocketAddress connectionAddress,
                         Identity identity,
                         HeartbeatCommsFactory heartbeatCommsFactory,
                         IOConnectionServerFactory factory,
                         LeaderProtocolFactory leaderProtocolFactory,
                         HeartbeatProtocolFactory heartbeatProtocolFactory,
                         PartitionProtocol partitionProtocol, long interval,
                         long timeout, boolean isPreferredLeaderNode)
                                                                     throws IOException {
        this.identity = identity;
        this.leaderProtocolFactory = leaderProtocolFactory;
        this.heartbeatProtocolFactory = heartbeatProtocolFactory;
        this.partitionProtocol = partitionProtocol;
        this.partitionProtocol.setConnectionSet(this);
        heartbeatComms = heartbeatCommsFactory.create(this);
        setTiming(interval, timeout);
        connectionServer = factory.create(connectionAddress, identity, this);

        SelfConnection self = new SelfConnection(identity, connectionView,
                                                 connectionServer.getAddress(),
                                                 isPreferredLeaderNode);

        // Heartbeat and leader protocols 
        leaderMgr = leaderProtocolFactory.createMgr(connections, self);

        stability = heartbeatInterval + timeout;
        quiesce = heartbeatInterval + stability;

        // Protocol timing driver thread 
        intervalExec = new IntervalExec(identity, this, heartbeatInterval);

        // Heartbeat message initialisation 
        heartbeat = heartbeatProtocolFactory.createMsg(identity,
                                                       connectionServer.getAddress());
        heartbeat.setMsgLinks(msgLinks);
        heartbeat.setView(connectionView);
        heartbeat.setViewNumber(viewNumber);
        heartbeat.setIsPreferred(isPreferredLeaderNode);
        heartbeat.setCandidate(leaderMgr.getLeader());

        /**
         * Start connected to self
         */
        connections.put(identity, self);
        connectionView.add(identity);

        log.info(String.format("Connection set: %s started on: %s", identity,
                               connectionServer.getAddress()));
    }

    /**
     * dummy for now - check for completion of stability period.
     * 
     * @param timenow
     */
    public void checkStability(long timenow) {
        if (!stablizing.get()) {
            if (log.isTraceEnabled()) {
                log.trace("Unstable @ " + timenow);
            }
            return;
        }

        /**
         * If the stability period has expired then set the partition to stable
         * and clear the stability period from interval executive
         */
        if (timenow >= connectionView.getTimeStamp()) {
            if (log.isTraceEnabled()) {
                log.trace("Stablizing view @ " + timenow);
            }
            connectionView.stablize();
            stablizing.set(false);
            intervalExec.clearStability();
            partitionProtocol.stableView();
            dropBrokenConnections();
        }

        /**
         * drive the partition manager's notifictions.
         */
        partitionProtocol.notifyChanges();
    }

    /**
     * Create a message connection to the node with the given id.
     * 
     * @param id
     *            - the target node
     * @return - null if the target node is not valid (i.e. not in the
     *         connection set), the MessageConnection corresponding to the given
     *         target if it is valid.
     */
    public synchronized MessageConnection connect(int id) {

        /**
         * If there is not a valid connection in the set corresponding to the
         * required id then return null to indicate failure.
         */
        if (!connectionView.contains(id)) {
            log.error(String.format("No valid connection for id: %s on: %s, view: %s ",
                                    id, identity.id, connectionView));
            return null;
        }

        /**
         * Cannot connect to self - return null and print informative stack
         * trace to output stream. Ultimately we should either handle messaging
         * to self or throw an exception. It is not reasonable to give the same
         * error condition for both unknown id and self by returning null in
         * both cases.
         */
        if (identity.id == id) {
            log.error("ConnectionSet.connect() called with self as parameter - cannot connect to self");
            return null;
        }

        /**
         * Set the msgLinks id to indicate that this end requires a connection.
         * this is a idempotent operation so there is no harm doing it up front.
         * This should be done before constructing a connection so that the
         * first heartbeat the other end sees will include the indication that
         * this end wants a connection.
         */
        msgLinks.add(id);

        /**
         * Find the connection that corresponds to the id we want.
         */
        Identity node = new Identity(identity.magic, id, 0);
        Connection con = connections.get(node);

        /**
         * If we already have a message connection then just return it
         */
        if (con instanceof MessageConnection) {
            ((MessageConnection) con).connect();
            if (log.isTraceEnabled()) {
                log.trace("connection already established for: " + id + " on: "
                          + identity.id);
            }
            return (MessageConnection) con;
        }

        /**
         * At this point we have established that we do not have a message
         * connection, but there is a heartbeat connection. Convert the
         * heartbeat connection to a message connection.
         */
        if (log.isTraceEnabled()) {
            log.trace("Converting heartbeat connection for: " + id + " on: "
                      + identity.id);
        }
        HeartbeatConnection hbcon = (HeartbeatConnection) con;
        MessageConnection mcon = new MessageConnection(identity, this,
                                                       hbcon.getProtocol(),
                                                       hbcon.getCandidate());
        if (isIgnoring(hbcon.getSender())) {
            mcon.setIgnoring(true);
        }
        connections.put(hbcon.getSender(), mcon);
        msgConnections.add(mcon);

        /**
         * If this end is initiating then start and initiator. If the other end
         * is initiating then blast an extra heartbeat to speed up the other end
         * getting the indication to initiate. In either case this is an
         * asynchronous operation to complete the message connection just
         * created.
         */
        if (thisEndInitiatesConnectionsTo(hbcon.getSender())) {
            if (log.isTraceEnabled()) {
                log.trace("Initiating connection to: " + id + " on: "
                          + identity.id);
            }
            getConnectionServer().initiateConnection(identity,
                                                     mcon,
                                                     prepareHeartbeat(heartbeat.getTime()));
        } else {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Waiting for callback from: %s on: %s",
                                        id, identity));
            }
            heartbeatComms.requestConnect(prepareHeartbeat(heartbeat.getTime()),
                                          node);
        }

        /**
         * return the message connection immediately - don't wait for the
         * asynchronous build of the implementation.
         */
        return mcon;
    }

    @Override
    public void connectTo(Identity peer) {
        connect(peer.id);
    }

    public synchronized boolean contains(Identity sender) {
        return getView().contains(sender);
    }

    /**
     * Replace a message connection with a heartbeat connection
     * 
     * @param mcon
     *            - the message connection to be replaced
     */
    public synchronized void convertToHeartbeatConnection(MessageConnection mcon) {
        Connection con = new HeartbeatConnection(identity, this,
                                                 mcon.getProtocol(),
                                                 mcon.getCandidate());
        msgConnections.remove(mcon);
        connections.put(con.getSender(), con);
    }

    /**
     * If this end initiates connections to the given node then replace the
     * heartbeat connection with a message connection.
     * 
     * @param con
     *            - the heartbeat connection to be replaced
     */
    public synchronized void convertToMessageConnection(HeartbeatConnection con) {

        /**
         * ignore the request for a message connection if we are not connected
         * (could be in connections and not in connection set due to timed-out
         * connection that has not quiesced yet.)
         */
        if (!connectionView.contains(con.getSender())) {
            if (log.isTraceEnabled()) {
                log.trace("Cannot convert connection to " + con.getSender()
                          + " as it is not in the view");
            }
            return;
        }

        if (thisEndInitiatesConnectionsTo(con.getSender())) {
            MessageConnection mcon = new MessageConnection(identity, this,
                                                           con.getProtocol(),
                                                           con.getCandidate());
            connections.put(mcon.getSender(), mcon);
            msgConnections.add(mcon);

            if (isIgnoring(mcon.getSender())) {
                if (log.isTraceEnabled()) {
                    log.trace("Ignoring connection to " + con.getSender());
                }
                mcon.setIgnoring(true);
            }

            getConnectionServer().initiateConnection(identity,
                                                     mcon,
                                                     prepareHeartbeat(heartbeat.getTime()));
        }
    }

    /**
     * To disconnect a message connection we set the msgLinks to indicate that
     * this end does not need it any longer.
     * 
     * Need to add code to do the disconnect - when both ends are clear.
     * 
     * @param id
     */
    public synchronized void disconnect(Identity id) {
        msgLinks.remove(id.id);
        Connection connection = connections.remove(id);
        if (connection != null) {
            msgConnections.remove(connection);
            connection.terminate();
        }
    }

    /**
     * Force an election - the partition manager determines when a new election
     * should take place. The election is relative to the partition membership,
     * but connectionSet is responsible for it because the candidate information
     * is held by the connections.
     * 
     * @param v
     *            - election relative to view v
     * @return - the winner's id
     */
    public synchronized Identity electLeader(View v) {
        return leaderMgr.electLeader(v);
    }

    /**
     * gets the connection in the specified possition of the connection set.
     * 
     * @param id
     * @return connection
     */
    public synchronized Connection getConnection(Identity id) {
        return connections.get(id);
    }

    /**
     * get the connection server
     */
    public IOConnectionServer getConnectionServer() {
        return connectionServer;
    }

    /**
     * Returns the most recently set up heartbeat
     * 
     * @return - the heartbeat
     */
    public Heartbeat getHeartbeat() {
        return prepareHeartbeat(heartbeat.getTime());
    }

    public long getInterval() {
        return heartbeatInterval;
    }

    /**
     * getNodeAddress() obtains the InetAddress associated with the other end of
     * a given connection.
     * 
     * @param id
     *            - the id of the node
     * @return - the InetAddress associated with id. If there is not a valid for
     *         id then return null.
     */
    public synchronized InetAddress getNodeAddress(int id) {

        /**
         * If there is not a valid connection in the set corresponding to the
         * required id then return null to indicate failure.
         */
        if (!connectionView.contains(id)) {
            return null;
        }

        /**
         * Find the connection that corresponds to the id we want.
         */
        Identity node = new Identity(identity.magic, id, 0);
        Connection con = connections.get(node);

        return con.getSenderAddress().getAddress();
    }

    /**
     * returns an string representing the status of all threads
     */
    public String getThreadStatusString() {
        StringBuilder builder = new StringBuilder();
        builder.append(intervalExec.getThreadStatusString()).append("\n");
        builder.append(heartbeatComms.getStatusString()).append("\n");
        builder.append(connectionServer.getThreadStatusString()).append("\n");
        return builder.toString();
    }

    public long getTimeout() {
        return timeout / heartbeatInterval;
    }

    public synchronized View getView() {
        return connectionView;
    }

    public synchronized boolean isIgnoring(Identity id) {
        return agent != null && ignoring.contains(id);
    }

    /**
     * Connections use this method to indicate that their view has changed. If
     * the new view does not include this node then remove the remote node will
     * be removed from the partition.
     * 
     * Whenever a view changes we must destablize. It is possible that the only
     * change is the view number, but this indicates that the remote view
     * changed and changed back - this is a real change!!
     * 
     * @param id
     *            - the connection's identity (not used)
     * @param v
     *            - the view
     */
    @Override
    public synchronized void newView(Identity id, View v) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("New view: %s on: %s from: %s", v,
                                    identity, id));
        }
        changeInViews.set(true);
        stablizing.set(false);
        connectionView.destablize();
        intervalExec.clearStability();
        /**
         * should we set the time stamp to default here?
         */

        /**
         * If this node is not in the new view then the remote node can not be
         * in the partition. Immediately remove it and destabalise.
         */
        if (!v.contains(identity)) {
            partitionProtocol.remove(id);
        }

    }

    /**
     * Connections use this method to indicate that their view timeStamp has
     * change (although the view may be the same). The view time stamp is
     * interpreted as the time at which the remote node recognized that its
     * views (connection set and remote views) became consistent.
     * 
     * This information is only interesting if the local view is stablizing.
     * 
     * @param id
     *            - the connection's identity (not used)
     * @param v
     *            - the view (includes the timestamp)
     */
    @Override
    public synchronized void newViewTime(Identity id, View v) {
        if (changeInViews.get() || !stablizing.get()) {
            if (log.isTraceEnabled()) {
                log.trace(String.format("New view time: %s on: %s from: %s, changeInViews: %s, stabilzing: %s",
                                        v, identity, id, changeInViews.get(),
                                        stablizing));
            }
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace(String.format("New view time: %s on: %s from: %s", v,
                                    identity, id));
        }

        if (isBetterTimeStamp(v.getTimeStamp())) {
            connectionView.setTimeStamp(v.getTimeStamp());
            intervalExec.setStability(connectionView.getTimeStamp());
        }
    }

    /**
     * 
     * @param hb
     *            Heartbeat
     * @return boolean
     */
    @Override
    public synchronized boolean receiveHeartbeat(Heartbeat hb) {
        Connection con = getConnection(hb.getSender());
        if (con == null) {
            Candidate can = leaderProtocolFactory.createCandidate(hb);
            HeartbeatProtocol hbp = heartbeatProtocolFactory.createProtocol(hb,
                                                                            this,
                                                                            prepareHeartbeat(heartbeat.getTime()));
            con = new HeartbeatConnection(identity, this, hbp, can);
            if (log.isTraceEnabled()) {
                log.trace(String.format("Adding heartbeat connection: %s at: %s ",
                                        hb.getSender(), identity));
            }
            addConnection(con);
            // should this return true?
            return true;
        }
        if (log.isTraceEnabled()) {
            log.trace(String.format("Heart beat for existing connection: %s at: %s ",
                                    hb.getSender(), identity));
        }
        if (con.isSelf()) {
            if (!hb.getSenderAddress().equals(heartbeat.getSenderAddress())) {
                log.error(String.format("Detected another member with same node identity as this node: %s.  Terminating this member",
                                        identity));
                terminate();
                return false;
            }
        }
        return con.receiveHeartbeat(hb);
    }

    public synchronized void receiveObject(Object obj, Identity id, long time) {
        if (terminated) {
            return;
        }
        if (connectionView.contains(id)) {
            partitionProtocol.receiveObject(obj, id, time);
        } else {
            if (log.isTraceEnabled()) {
                log.trace(String.format("Ignoring received object: %s from: %s on: %s as it is not in our view",
                                        obj, id, identity));
            }
        }
    }

    public void registerController(ControllerAgent controller) {
        heartbeat.setController(controller.getAddress());
        intervalExec.registerController(controller);
        agent = controller;
    }

    /**
     * Used to indicate that a connection has terminated. This results in the
     * connection being removed from the local connection set.
     * 
     * @param con
     *            - the terminated connection
     */
    public synchronized void removeConnection(Connection con) {

        /**
         * In the special case where this call is nested inside a call to
         * sendHeartbeats() we delay removing the connection to avoid a
         * ConcurrentModificationException in sendHeartbeats(). This alternative
         * has a lower overhead than cloning msgConnections every time.
         */
        if (sendingHeartbeats.get()) {
            msgConDelayedDelete.add(con);
            return;
        }

        if (log.isTraceEnabled()) {
            log.trace(String.format("Removing connection: %s from: %s",
                                    con.getSender(), identity));
        }

        /**
         * remove from the connectionSet view. If a message connection remove it
         * from the list of message connections. Record that there has been a
         * change in the views and create a new view number. Tell the partition
         * manager immediately.
         */
        connectionView.remove(con.getSender());
        if (con instanceof MessageConnection) {
            msgConnections.remove(con);
        }
        changeInViews.set(true);
        intervalExec.clearStability();
        viewNumber++;
        partitionProtocol.remove(con.getSender());
    }

    /**
     * Send a heartbeat: heartbeats are sent on the multicast comms and each
     * messaging connection.
     * 
     * @param timenow
     */
    public synchronized void sendHeartbeat(long timenow) {

        /**
         * This flag is used to delay connection removal in removeConnection().
         */
        sendingHeartbeats.set(true);

        /**
         * send the heartbeat using multicast for heartbeat connections
         */
        heartbeatComms.sendHeartbeat(prepareHeartbeat(timenow));

        /**
         * send the heartbeat on message connections.
         */
        for (MessageConnection mcon : msgConnections) {
            mcon.sendMsg(heartbeat);
        }

        /**
         * Do delayed removeConnection() calls
         */
        sendingHeartbeats.set(false);
        for (Connection con : msgConDelayedDelete) {
            removeConnection(con);
        }
        msgConDelayedDelete.clear();

        if (agent != null) {
            agent.updateHeartbeat(heartbeat);
        }
    }

    public synchronized void setIgnoring(View ignoring) {
        if (agent == null) {
            return;
        }
        if (ignoring == null) {
            ignoring = new BitView();
        }
        this.ignoring = ignoring;

        heartbeatComms.setIgnoring(ignoring);

        for (MessageConnection mcon : msgConnections) {
            if (isIgnoring(mcon.getSender())) {
                mcon.setIgnoring(true);
            } else {
                mcon.setIgnoring(false);
            }
        }
    }

    public void setTiming(long interval, long timeout) {
        heartbeatInterval = interval;
        this.timeout = interval * timeout;
        stability = interval * (timeout + 1);
        quiesce = interval * (timeout + 2);
        if (intervalExec != null) {
            intervalExec.setInterval(interval);
        }
    }

    @PostConstruct
    public void start() throws Exception {
        if (log.isTraceEnabled()) {
            log.trace(identity + " connection address is "
                      + connectionServer.getAddress().toString());
        }

        connectionView.setTimeStamp(identity.epoch);
        partitionProtocol.start();
        intervalExec.start();
        connectionServer.start(heartbeat);
        heartbeatComms.start(heartbeat);
    }

    @PreDestroy
    public void terminate() {
        terminated = true;
        intervalExec.terminate();
        connectionServer.terminate();
        for (Connection con : connections.values()) {
            con.terminate();
        }
        heartbeatComms.terminate();
    }

    /**
     * indicates which end initiates a connection to a given node
     * 
     * @param target
     *            - the other end of the intended connection
     * @return - true if this end initiates the connection, false otherwise
     */
    public boolean thisEndInitiatesConnectionsTo(Identity target) {
        return thisEndInitiatesConnectionsTo(target.id);
    }

    @Override
    public String toString() {
        StringBuffer str = new StringBuffer();
        str.append("\n").append(identity).append(" ConnectionSet view: ").append(super.toString());
        str.append("\n==================================================");
        for (Connection con : connections.values()) {
            str.append("\n ").append(con.getSender()).append(" : ").append(con);
        }
        return str.toString();
    }

    /**
     * Instruct the connection set to start using the newly created message
     * connection. This happens when the other end initiates a connection.
     * 
     * @param mcon
     *            - the new connection
     * @return - false if the connection is not in the connection set, true if
     *         the connection was added.
     */
    public synchronized boolean useNewMessageConnection(MessageConnection mcon) {
        if (!connectionView.contains(mcon.getSender())) {
            return false;
        }
        connections.put(mcon.getSender(), mcon);
        msgConnections.add(mcon);
        return true;
    }

    /**
     * Check for any changes that the partition manager should be informed of.
     * This method is called each time around the heartbeat interval and drives
     * reports to the user.
     * 
     * @param timenow
     *            - the current time
     */
    public synchronized void viewChangeCheck(long timenow) {

        /**
         * If the view is stable then nothing has changed and there is nothing
         * to report
         */
        if (connectionView.isStable()) {
            return;
        }

        /**
         * If the view has changed then inform the partition manager. If the
         * view is consistent then initiate the stablization period.
         */
        if (changeInViews.compareAndSet(true, false)) {
            partitionProtocol.changedView();

            /**
             * the consistent(timenow) method also sets the view timestamp to
             * the predicted stability time when consistent (returns true) or to
             * undefined when inconsistent (returns false).
             */
            if (consistent(timenow)) {
                stablizing.set(true);
                intervalExec.setStability(connectionView.getTimeStamp());
            }
        }

        /**
         * drive the partition manager's notifictions.
         */
        partitionProtocol.notifyChanges();
    }

    /**
     * Returns an indication of
     * 
     * @param id
     * @return boolean true if message link contains id
     */
    public boolean wantsMsgLinkTo(Identity id) {
        return msgLinks.contains(id.id);
    }

    /**
     * Add a new connection to the set. This will have been created by the
     * transport.
     * 
     * @param con
     */
    private void addConnection(Connection con) {
        connections.put(con.getSender(), con);
        connectionView.add(con.getSender());
        changeInViews.set(true);
        intervalExec.clearStability();
        viewNumber++;
    }

    /**
     * The set of views is consistent if all views agree with the local view
     * (which is the connection set). This method sets the local view timestamp
     * as a side-effect
     * 
     * @return
     */
    private boolean consistent(long timenow) {
        connectionView.setTimeStamp(timenow + stability);
        for (Connection con : connections.values()) {
            /**
             * if an active connection and it is not self then check it is
             * consistent with self and check its stability time
             */
            if (connectionView.contains(con.getSender())
                && !con.getSender().equalId(identity)) {
                /**
                 * if not equal the not consistent - set the time to undefined
                 * and return false
                 */
                if (!con.equalsView(connectionView)) {
                    connectionView.setTimeStamp(View.undefinedTimeStamp);
                    return false;
                }
                /**
                 * The following is an optimisation that can only be applied if
                 * the heartbeat protocol checks for synchronised clocks.
                 */
                if (con.measuresClockSkew()
                    && isBetterTimeStamp(con.getTimeStamp())) {
                    connectionView.setTimeStamp(con.getTimeStamp());
                }
            }
        }
        return true;
    }

    /**
     * Drop connections to nodes that are not in the view - they will be broken.
     */
    private void dropBrokenConnections() {
        for (MessageConnection connection : msgConnections) {
            if (!connectionView.contains(connection.getId())) {
                connection.disconnect();
            }
        }
    }

    /**
     * returns true if the time stamp given as a parameter is better than the
     * one currently held by this view (connectionSet).
     * 
     * The timeStamp parameter is better then the current time stamp if: 1)
     * timeStamp is defined and the current time stamp is not 2) both are
     * defined but timeStamp is less than the current time stamp.
     * 
     * Note: a time stamp is undefined if its value is View.undefinedTimeStamp
     * 
     * @param timeStamp
     * @return
     */
    private boolean isBetterTimeStamp(long timeStamp) {
        return timeStamp != View.undefinedTimeStamp
               && (connectionView.getTimeStamp() == View.undefinedTimeStamp || timeStamp < connectionView.getTimeStamp());
    }

    private Heartbeat prepareHeartbeat(long timenow) {
        /**
         * prepare the heartbeat with the latest information
         */
        heartbeat.setMsgLinks(msgLinks);
        heartbeat.setTime(timenow);
        heartbeat.setView(connectionView);
        heartbeat.setViewNumber(viewNumber);
        heartbeat.setCandidate(leaderMgr.getLeader());
        return heartbeat;
    }

    /**
     * indicates which end initiates a connection to a given node
     * 
     * @param target
     *            - the other end of the intended connection
     * @return - true if this end initiates the connection, false otherwise
     */
    private boolean thisEndInitiatesConnectionsTo(int target) {
        return identity.id < target;
    }

    /**
     * Force the partition to destabilize
     */
    protected synchronized void destabilize() {
        if (connectionView.isStable()) {
            changeInViews.set(true);
            stablizing.set(false);
            connectionView.destablize();
            intervalExec.clearStability();
        }
    }

    /**
     * scans through the connections and checks to see if any have expired or
     * are ready to be cleaned up. If expired they are terminated (entering a
     * quiescence period), if to be cleaned up they are removed.
     */
    synchronized void checkTimeouts(long timenow) {

        Iterator<Entry<Identity, Connection>> iter = connections.entrySet().iterator();
        while (iter.hasNext()) {
            Connection con = iter.next().getValue();

            /**
             * Only bother if the connection has missed its deadline
             */
            if (con.isNotTimely(timenow, timeout)) {
                if (log.isTraceEnabled()) {
                    log.trace(String.format("Terminating untimely connection: %s",
                                            con));
                }

                /**
                 * If the connection is in the connection set then terminate it.
                 */
                if (connectionView.contains(con.getSender())) {
                    con.terminate();
                    removeConnection(con);
                }

                /**
                 * check for clean up - don't clean up until the quiescence
                 * period has expired. This is how new connections are prevented
                 * during quiescence.
                 */
                if (con.isQuiesced(timenow, quiesce)) {
                    iter.remove();
                    if (log.isTraceEnabled()) {
                        log.trace(String.format("Removed connection %s", con));
                    }
                }
            }
        }
    }

    /**
     * @return
     */
    public Identity getId() {
        return identity;
    }
}
