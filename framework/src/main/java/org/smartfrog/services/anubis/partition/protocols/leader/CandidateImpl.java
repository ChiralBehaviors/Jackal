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
package org.smartfrog.services.anubis.partition.protocols.leader;

import org.smartfrog.services.anubis.partition.util.Identity;

public class CandidateImpl implements Candidate {

    private int      count     = 0;
    /**
     * Candidate information
     */
    private Identity me        = null;
    private boolean  preferred = false;
    private Identity vote      = null;

    /**
     * Constructor - set to vote for given candidate
     * 
     * @param id
     *            - own id
     * @param v
     *            - voting for candidate v
     * @param preferred
     *            - is this a preferred node
     */
    public CandidateImpl(Identity id, Identity v, boolean preferred) {
        me = id;
        vote = v;
        this.preferred = preferred;
    }

    @Override
    public void clearReceivedVotes() {
        count = -1;
    }

    @Override
    public int countReceivedVotes() {
        return count;
    }

    /**
     * Candidate interface
     * 
     * @return Identity
     */
    @Override
    public Identity getId() {
        return me;
    }

    @Override
    public Identity getVote() {
        return vote;
    }

    @Override
    public boolean isPreferred() {
        return preferred;
    }

    @Override
    public void receiveVote(Candidate c) {
        count++;
    }

    @Override
    public void setVote(Candidate c) {
        vote = c.getId();
    }

    @Override
    public void setVote(Identity v) {
        vote = v;
    }

    @Override
    public boolean winsAgainst(Candidate c) {
        /*
         * preferred always win against non-preferred.
         * otherwise the rules are the same for arbiters as for 
         * regular candidates.
         */
        if (isPreferred() && !c.isPreferred()) {
            return true;
        }
        if (!isPreferred() && c.isPreferred()) {
            return false;
        }
        return countReceivedVotes() > c.countReceivedVotes()
               || countReceivedVotes() == c.countReceivedVotes()
               && getId().id > c.getId().id;
    }
}
