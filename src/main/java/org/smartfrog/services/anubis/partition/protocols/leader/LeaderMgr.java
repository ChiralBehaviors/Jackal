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
import org.smartfrog.services.anubis.partition.views.View;

import java.util.Map;
import java.util.Iterator;

public class LeaderMgr {

    protected Map           candidates     = null;
    protected Candidate     localCandidate = null;


    public LeaderMgr(Map candidateMap, Candidate local) {
        candidates       = candidateMap;
        localCandidate   = local;
        localCandidate.setVote(localCandidate.getId());
    }

    /**
     * Returns the current leader - assumes there has been an election.
     * The local candidate always votes for the winner of the local
     * election. So return the local candidates vote
     * @return  Identity
     */
    public Identity getLeader() {
        return localCandidate.getVote();
    }

    /**
     * The election is based on the criteria for picking a leader set
     * in CandidateInfo. The election has two passes - the first just initialises
     * the candidates, the second pass counts votes and keeps a running note
     * on the best candidate (to avoid a third pass to find the best candidate).
     * the winner is returned.
     *
     * The election is relative to the view passed in (this will be the
     * local parititon). Votes are only valid if the node voting and the
     * node voted for are both in the partition.
     *
     * @param v
     * @return  Candidate
     */
    private synchronized Candidate election(View v) {

        Iterator iter;

        /**
         * reset the candidates (clear the votes)
         */
        iter = candidates.values().iterator();
        while( iter.hasNext() )
            ((Candidate)iter.next()).clearReceivedVotes();


        /**
         * Count all the votes and note which has the highest identity - this
         * will be the winner in the event of no votes!
         */
        Candidate bestSoFar = localCandidate;
        if( !v.contains(localCandidate.getVote()) ) {
            localCandidate.setVote(localCandidate.getId());
        }

        iter = candidates.values().iterator();
        while( iter.hasNext() ) {
            Candidate voter = (Candidate)iter.next();

            /**
             * If the voter is in the view then the voter is valid.
             */
            if( v.contains( voter.getId() ) ) {

                /**
                 * Note if the voter itself is the best candidate we have
                 * seen so far.
                 */
                if( voter.winsAgainst( bestSoFar ) )
                    bestSoFar = voter;

                /**
                 * If the vote is valid count it.
                 */
                if( v.contains(voter.getVote()) ) {

                    Candidate votedFor = (Candidate)candidates.get(voter.getVote());
                    votedFor.receiveVote(voter);

                    /**
                     * Note if the voted for is the best candidate we have seen
                     * so far.
                     */
                    if( votedFor.winsAgainst( bestSoFar ) )
                        bestSoFar = votedFor;
                }
            }
        }

        /**
         * return the winner
         */
        return bestSoFar;
    }

    /**
     * electLeader(v) performs an election amoung the members of the view
     * v and sets the local candidate's vote to that member. The election
     * uses one of two selection criteria depending on the stability of
     * the view.
     *
     * @param v
     * @return  Identity
     */
    public synchronized Identity electLeader(View v) {
        return (v.isStable() ? stableElection(v) : unstableElection(v));
    }


    /**
     * Perform the stable election (most votes wins - highest id as tie-breaker)
     *
     * @param v
     * @return Identity
     */
    private Identity stableElection(View v) {
        localCandidate.setVote(election(v));
        return localCandidate.getVote();
    }


    /**
     * Perform the unstable election (previous winner wins if still in view,
     * otherwise local candidate)
     *
     * @param v
     * @return  Identity
     */
    private Identity unstableElection(View v) {
        if( !v.contains(localCandidate.getVote()) )
            localCandidate.setVote( localCandidate );
        return localCandidate.getVote();
    }


    /**
     * predictLeader(v) performs an election amoung the members of the view
     * v but does not set the local candidate's vote.
     *
     * This method can be used to predict which candidate would win the
     * election at stability if the votes remain unchanged. There is no
     * guarantee on accuracy, but it can be used as a guide.
     *
     * @param v
     * @return Identity
     */
    public synchronized Identity predictLeader(View v) {
        return election(v).getId();
    }


}
