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



import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocol;
import org.smartfrog.services.anubis.partition.protocols.leader.Candidate;
import org.smartfrog.services.anubis.partition.protocols.leader.CandidateImpl;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;


public class SelfConnection extends BitView implements Connection, HeartbeatProtocol, Candidate {

    private Identity          me        = null;
    private Candidate         candidate = null;
    private ConnectionAddress address   = null;

    public SelfConnection(Identity id, View v, ConnectionAddress addr, boolean preferred) {
            me        = id;
            address   = addr;
            view      = v.toBitSet();
            stable    = v.isStable();
            candidate = new CandidateImpl(me, me, preferred);
    }

    /**
     * HeartbeatProtocol interface
     */
    public boolean isNotTimely(long timenow, long timeout)  { return false; }
    public boolean isQuiesced(long timenow, long quiesce) { return false; }
    /**
     * Self connection is clearly not skewed
     */
    public boolean measuresClockSkew() { return true; }

    /**
     * Timed interface
     */
    public long              getTime()                 { return System.currentTimeMillis();}
    public void              setTime(long t)           { return; }

    /**
     * Connection interface - includes Sender interface
     */
    public Identity          getSender()               { return me; }
    public ConnectionAddress getSenderAddress()        { return address; }
    public void              terminate()               { return; }

    /**
     * HeartbeatProtocol interface
     * @param h
     */
    public boolean           receiveHeartbeat(Heartbeat h) { return true; }

    /**
     * Candidate interface - redirect to CandidateImpl
     * @return Indentity
     */
    public Identity  getId()               { return getSender(); }
    public boolean   isPreferred()           { return candidate.isPreferred(); }
    public Identity  getVote()                { return candidate.getVote(); }
    public void      setVote(Identity v)      { candidate.setVote(v); }
    public void      setVote(Candidate c)     { candidate.setVote(c); }
    public void      clearReceivedVotes()     { candidate.clearReceivedVotes(); }
    public void      receiveVote(Candidate c) { candidate.receiveVote(c); }
    public int       countReceivedVotes()     { return candidate.countReceivedVotes(); }
    public boolean   winsAgainst(Candidate c) { return candidate.winsAgainst(c); }
}
