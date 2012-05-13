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
package org.smartfrog.services.anubis.partition.comms;

import java.net.InetSocketAddress;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocol;
import org.smartfrog.services.anubis.partition.protocols.leader.Candidate;
import org.smartfrog.services.anubis.partition.protocols.leader.CandidateImpl;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

public class SelfConnection extends BitView implements Connection,
        HeartbeatProtocol, Candidate {

    private static final long       serialVersionUID = 1L;
    private final InetSocketAddress address;
    private transient Candidate     candidate;
    private final Identity          me;

    public SelfConnection(Identity id, View v, InetSocketAddress addr,
                          boolean preferred) {
        me = id;
        address = addr;
        view.copyFrom(v.toBitSet());
        stable.set(v.isStable());
        candidate = new CandidateImpl(me, me, preferred);
    }

    @Override
    public void clearReceivedVotes() {
        candidate.clearReceivedVotes();
    }

    @Override
    public int countReceivedVotes() {
        return candidate.countReceivedVotes();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SelfConnection) {
            return me.equals(((SelfConnection) obj).me);
        }
        return false;
    }

    /**
     * Candidate interface - redirect to CandidateImpl
     * 
     * @return Indentity
     */
    @Override
    public Identity getId() {
        return getSender();
    }

    /**
     * Connection interface - includes Sender interface
     */
    @Override
    public Identity getSender() {
        return me;
    }

    @Override
    public InetSocketAddress getSenderAddress() {
        return address;
    }

    /**
     * Timed interface
     */
    @Override
    public long getTime() {
        return System.currentTimeMillis();
    }

    @Override
    public Identity getVote() {
        return candidate.getVote();
    }

    @Override
    public int hashCode() {
        return me.hashCode();
    }

    /**
     * HeartbeatProtocol interface
     */
    @Override
    public boolean isNotTimely(long timenow, long timeout) {
        return false;
    }

    @Override
    public boolean isNotTimelyMsgConnection(long timenow, long timebound) {
        return isNotTimely(timenow, timebound);
    }

    @Override
    public boolean isPreferred() {
        return candidate.isPreferred();
    }

    @Override
    public boolean isQuiesced(long timenow, long quiesce) {
        return false;
    }

    @Override
    public boolean isSelf() {
        return true;
    }

    /**
     * Self connection is clearly not skewed
     */
    @Override
    public boolean measuresClockSkew() {
        return true;
    }

    /**
     * HeartbeatProtocol interface
     * 
     * @param h
     */
    @Override
    public boolean receiveHeartbeat(Heartbeat h) {
        return true;
    }

    @Override
    public void receiveVote(Candidate c) {
        candidate.receiveVote(c);
    }

    @Override
    public void setTime(long t) {
        return;
    }

    @Override
    public void setVote(Candidate c) {
        candidate.setVote(c);
    }

    @Override
    public void setVote(Identity v) {
        candidate.setVote(v);
    }

    @Override
    public void terminate() {
        return;
    }

    @Override
    public boolean winsAgainst(Candidate c) {
        return candidate.winsAgainst(c);
    }
}
