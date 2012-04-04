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
package org.smartfrog.services.anubis.partition.protocols.heartbeat;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.protocols.leader.Candidate;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

public class HeartbeatProtocolAdapter implements HeartbeatProtocol, Candidate {

    private Candidate             candidate         = null;
    /**
     * Implementation of the HeartbeatProtocol
     */
    private HeartbeatProtocol     heartbeatProtocol = null;

    protected static final Logger log               = LoggerFactory.getLogger(HeartbeatProtocolAdapter.class.getCanonicalName());

    /**
     * Constructor - creates a HeartbeatProtocolAdapter pointing to the given
     * protocol and the candidate via super class
     * 
     * @param hbp
     *            - the protocol
     */
    public HeartbeatProtocolAdapter(HeartbeatProtocol hbp, Candidate can) {
        setProtocol(hbp);
        setCandidate(can);
    }

    @Override
    public int cardinality() {
        return heartbeatProtocol.cardinality();
    }

    @Override
    public void clearReceivedVotes() {
        candidate.clearReceivedVotes();
    }

    @Override
    public boolean containedIn(View v) {
        return heartbeatProtocol.containedIn(v);
    }

    @Override
    public boolean contains(Identity id) {
        return heartbeatProtocol.contains(id);
    }

    @Override
    public boolean contains(int id) {
        return heartbeatProtocol.contains(id);
    }

    @Override
    public boolean contains(View v) {
        return heartbeatProtocol.contains(v);
    }

    @Override
    public int countReceivedVotes() {
        return candidate.countReceivedVotes();
    }

    @Override
    public boolean equalsView(View v) {
        return heartbeatProtocol.equalsView(v);
    }

    public Candidate getCandidate() {
        return candidate;
    }

    /**
     * Candidate interface
     * 
     * @return Identity
     */
    @Override
    public Identity getId() {
        return candidate.getId();
    }

    /**
     * Get the protocol instance
     * 
     * @return - the protocol instance
     */
    public HeartbeatProtocol getProtocol() {
        return heartbeatProtocol;
    }

    /**
     * HeartbeatProtocol interface 3) Timed interface part
     */
    @Override
    public Identity getSender() {
        return heartbeatProtocol.getSender();
    }

    @Override
    public InetSocketAddress getSenderAddress() {
        return heartbeatProtocol.getSenderAddress();
    }

    /**
     * HeartbeatProtocol interface 2) Timed interface part
     */
    @Override
    public long getTime() {
        return heartbeatProtocol.getTime();
    }

    @Override
    public long getTimeStamp() {
        return heartbeatProtocol.getTimeStamp();
    }

    @Override
    public Identity getVote() {
        return candidate.getVote();
    }

    @Override
    public boolean isEmpty() {
        return heartbeatProtocol.isEmpty();
    }

    /**
     * HeartbeatProtocol interface
     */
    @Override
    public boolean isNotTimely(long timenow, long timebound) {
        return heartbeatProtocol.isNotTimely(timenow, timebound);
    }

    @Override
    public boolean isNotTimelyMsgConnection(long timenow, long timebound) {
        return heartbeatProtocol.isNotTimely(timenow, timebound);
    }

    @Override
    public boolean isPreferred() {
        return candidate.isPreferred();
    }

    @Override
    public boolean isQuiesced(long timenow, long quiesce) {
        return heartbeatProtocol.isQuiesced(timenow, quiesce);
    }

    @Override
    public boolean isStable() {
        return heartbeatProtocol.isStable();
    }

    @Override
    public boolean measuresClockSkew() {
        return heartbeatProtocol.measuresClockSkew();
    }

    @Override
    public boolean overlap(View v) {
        return heartbeatProtocol.overlap(v);
    }

    /**
     * HeartbeatProtocol interface 1) HeartbeatReceive interface part Modifies
     * behaviour to deliver a heartbeat to both the heartbeat protocol and the
     * candidate.
     */
    @Override
    public boolean receiveHeartbeat(Heartbeat hb) {

        /**
         * pass the heartbeat to the heartbeat protocol
         */
        boolean accepted = heartbeatProtocol.receiveHeartbeat(hb);
        if (log.isTraceEnabled()) {
            if (accepted) {
                log.trace(String.format("Heart beat accepted from: %s", getId()));
            } else {
                log.trace(String.format("Heart beat rejected from: %s", getId()));
            }
        }

        /**
         * If the heartbeat is accepted ok by the heartbeat protocol then get
         * the piggy-backed vote and pass it to the candidate impl
         */
        if (accepted) {
            setVote(hb.getCandidate());
        }

        return accepted;
    }

    @Override
    public void receiveVote(Candidate c) {
        candidate.receiveVote(c);
    }

    public void setCandidate(Candidate can) {
        candidate = can;
    }

    /**
     * Set the adapter to point to the given protocol
     * 
     * @param hbp
     *            - a protocol instance
     */
    public void setProtocol(HeartbeatProtocol hbp) {
        heartbeatProtocol = hbp;
    }

    @Override
    public void setTime(long t) {
        heartbeatProtocol.setTime(t);
    }

    @Override
    public void setVote(Candidate c) {
        candidate.setVote(c);
    }

    @Override
    public void setVote(Identity v) {
        candidate.setVote(v);
    }

    /**
     * HeartbeatProtocol interface 4) View interface part
     */
    @Override
    public int size() {
        return heartbeatProtocol.size();
    }

    @Override
    public void terminate() {
        heartbeatProtocol.terminate();
        if (log.isTraceEnabled()) {
            log.trace("terminating connection: " + this);
        }
    }

    @Override
    public NodeIdSet toBitSet() {
        return heartbeatProtocol.toBitSet();
    }

    @Override
    public boolean winsAgainst(Candidate c) {
        return candidate.winsAgainst(c);
    }
}
