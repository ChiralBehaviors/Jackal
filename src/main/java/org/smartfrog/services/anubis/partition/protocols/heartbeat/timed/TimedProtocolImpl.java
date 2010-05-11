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
package org.smartfrog.services.anubis.partition.protocols.heartbeat.timed;


import org.smartfrog.services.anubis.basiccomms.connectiontransport.ConnectionAddress;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocol;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.View;
import org.smartfrog.services.anubis.partition.views.ViewListener;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.HeartbeatMsg;



public class TimedProtocolImpl extends BitView implements HeartbeatProtocol {

    private long              time       = 0;
    private long              viewNumber = 0;
    private ViewListener      listener   = null;
    private Identity          sender     = null;
    private ConnectionAddress address    = null;
    private boolean           terminated = false;

    /**
     * Constructor - create a heartbeat protocol implementation using the
     *               information provided in a heartbeat message
     * @param hb
     * @param vl
     */
    public TimedProtocolImpl(Heartbeat hb, ViewListener vl, HeartbeatMsg sharedHeartbeat) {
        super(hb.getView());
        time       = hb.getTime();
        viewNumber = hb.getViewNumber();
        listener   = vl;
        sender     = hb.getSender();
        address    = hb.getSenderAddress();
    }

    public void terminate() {
        terminated = true;
    }

    /**
     * indicates if the heartbeat protocol is timely. This method is called
     * periodically by the connectionSet as part of a connection cleanup
     * action. "Timelyness" is relative to the current time (timenow)
     * and the timebound.
     *
     * @param timenow current time
     * @param timebound expiration period
     * @return true if expried, false if not
     */
    public boolean isNotTimely(long timenow, long timebound) {
        return ((timenow - time) > timebound) || ((time - timenow) > timebound);
    }

    /**
     * indicates if the heartbeat protocol has quiesced. When the protocol has
     * expired it will be inactive for a period before it can be removed. This
     * is used to avoid another instance of the protocol being created as
     * soon as this one expires (the partition protocol requires that connections
     * remain inactive for a period after they expire). The quiescence is relative
     * to the current time and the quiesce timeout.
     *
     * @param timenow current time
     * @param quiesce quesce timeout - must be greater than expire timeout*2
     * @return true if expired, false if not
     */
    public boolean isQuiesced(long timenow, long quiesce) {
        return (timenow - time) > quiesce;
    }

    /**
     * This protocol measures clock skew as part of the accumulative
     * time bound that it checks. This is because the timeliness check
     * compares timestamps from the remote end with clock times at this
     * end.
     * @return boolean
     */
    public boolean measuresClockSkew() {
        return true;
    }


    /**
     * receives a heartbeat message and deals with it according to the
     * heartbeat protocol. This is protocol uses straight-forward
     * timing of messages - only he most recent message time and its
     * information counts.
     *
     * @param hb - heartbeat message
     * @return  boolean false if terminated or failed
     */
    public boolean receiveHeartbeat(Heartbeat hb) {

        if( terminated )
            return false;

        /**
         * Only bother with a heartbeat if it superceeds
         * the last one (ordered delivery is not guaranteed!)
         */
        if( hb.getTime() > time ) {
            time = hb.getTime();
            boolean viewChanged      = false;
            boolean timeStampChanged = false;

            /**
             * Check for a new view time stamp (as opposed to
             * the heartbeat time!)
             */
            if( timeStamp != hb.getView().getTimeStamp() ) {
                timeStamp = hb.getView().getTimeStamp();
                timeStampChanged = true;
            }

            /**
             * check for a new view (view number) if the view numbers are different
             * but the views are the same then the view has changed and chaged back
             * without us noticing! We need to pick this up as a real change.
             */
            if( hb.getViewNumber() != viewNumber ) {
                viewNumber = hb.getViewNumber();
                view   = hb.getView().toBitSet();
                stable = hb.getView().isStable();
                viewChanged = true;
            }

            /**
             * generate appropriate notification to next protocol
             * layer if something changed.
             */
            if( viewChanged ) {
                listener.newView( sender, (View)this );
            } else if( timeStampChanged ) {
                listener.newViewTime( sender, (View)this );
            }

            return true;

        } else {
            return false;
        }
    }

    /**
     * Timed interface
     * @return  time
     */
    public long              getTime()          { return time; }
    public void              setTime(long t)    { time = t; }

    /**
     * Sender interface
     * @return  identity
     */
    public Identity          getSender()        { return sender; }
    public ConnectionAddress getSenderAddress() { return address; }

}
