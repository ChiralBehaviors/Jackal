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


import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.protocols.leader.Candidate;
import org.smartfrog.services.anubis.partition.util.NodeIdSet;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

public class HeartbeatProtocolAdapter
        implements HeartbeatProtocol, Candidate {

    /**
     * Implementation of the HeartbeatProtocol
     */
    private HeartbeatProtocol         heartbeatProtocol = null;
    private Candidate                 candidate         = null;

    /**
     * Constructor - creates a HeartbeatProtocolAdapter pointing to
     * the given protocol and the candidate via super class
     * @param hbp - the protocol
     */
    public HeartbeatProtocolAdapter(HeartbeatProtocol hbp, Candidate can) {
        setProtocol(hbp);
        setCandidate(can);
    }

    /**
     * Set the adapter to point to the given protocol
     * @param hbp - a protocol instance
     */
    public void setProtocol(HeartbeatProtocol hbp) { heartbeatProtocol = hbp; }
    public void setCandidate(Candidate can)        { candidate = can; }

    /**
     * Get the protocol instance
     * @return - the protocol instance
     */
    public HeartbeatProtocol getProtocol()  { return heartbeatProtocol; }
    public Candidate         getCandidate() { return candidate; }

    /**
     * HeartbeatProtocol interface
     */
    public boolean isNotTimely(long timenow, long timebound) {
        return heartbeatProtocol.isNotTimely(timenow, timebound);
    }
    public boolean isQuiesced(long timenow, long quiesce) {
        return heartbeatProtocol.isQuiesced(timenow, quiesce);
    }
    public boolean measuresClockSkew() {
        return heartbeatProtocol.measuresClockSkew();
    }
    public void terminate() {
        heartbeatProtocol.terminate();
    }


    /**
     * HeartbeatProtocol interface
     * 1) HeartbeatReceive interface part
     * Modifies behaviour to deliver a heartbeat to both the heartbeat
     * protocol and the candidate.
     */
    public boolean receiveHeartbeat(Heartbeat hb) {

        /**
         * pass the heartbeat to the heartbeat protocol
         */
        boolean accepted = heartbeatProtocol.receiveHeartbeat(hb);

        /**
         * If the heartbeat is accepted ok by the heartbeat protocol
         * then get the piggy-backed vote and pass it to the candidate impl
         */
        if( accepted )
            setVote(hb.getCandidate());

        return accepted;
    }

    /**
     * HeartbeatProtocol interface
     * 2) Timed interface part
     */
    public long getTime()           { return heartbeatProtocol.getTime(); }
    public void setTime(long t)     { heartbeatProtocol.setTime(t); }
    /**
     * HeartbeatProtocol interface
     * 3) Timed interface part
     */
    public Identity          getSender()        { return heartbeatProtocol.getSender(); }
    public ConnectionAddress getSenderAddress() { return heartbeatProtocol.getSenderAddress(); }
    /**
     * HeartbeatProtocol interface
     * 4) View interface part
     */
    public int               size()                { return heartbeatProtocol.size(); }
    public int               cardinality()         { return heartbeatProtocol.cardinality(); }
    public boolean           isEmpty()             { return heartbeatProtocol.isEmpty(); }
    public boolean           isStable()            { return heartbeatProtocol.isStable(); }
    public long              getTimeStamp()        { return heartbeatProtocol.getTimeStamp(); }
    public boolean           contains(int id)      { return heartbeatProtocol.contains(id); }
    public boolean           contains(Identity id) { return heartbeatProtocol.contains(id); }
    public boolean           contains(View v)      { return heartbeatProtocol.contains(v); }
    public boolean           containedIn(View v)   { return heartbeatProtocol.containedIn(v); }
    public boolean           overlap(View v)       { return heartbeatProtocol.overlap(v); }
    public boolean           equalsView(View v)    { return heartbeatProtocol.equalsView(v); }
    public NodeIdSet            toBitSet()            { return heartbeatProtocol.toBitSet(); }

    /**
     * Candidate interface
     * @return  Identity
     */
    public Identity          getId()               { return candidate.getId(); }
    public boolean           isPreferred()           { return candidate.isPreferred(); }
    public Identity          getVote()                { return candidate.getVote(); }
    public void              setVote(Identity v)      { candidate.setVote(v); }
    public void              setVote(Candidate c)     { candidate.setVote(c); }
    public void              clearReceivedVotes()     { candidate.clearReceivedVotes(); }
    public void              receiveVote(Candidate c) { candidate.receiveVote(c); }
    public int               countReceivedVotes()     { return candidate.countReceivedVotes(); }
    public boolean           winsAgainst(Candidate c) { return candidate.winsAgainst(c); }
}
