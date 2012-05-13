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
package org.smartfrog.services.anubis.partition.protocols.heartbeat.ping;

import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocol;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.ViewListener;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.smartfrog.services.anubis.partition.wire.msg.PingHeartbeatMsg;

/**
 * This version of the heartbeat protocol measures round trip times for pings
 * using the local clock. The heartbeat messages carry bits that represent
 * pings. One end initiates pinging and the other responds. So the one end sends
 * a set bit until it sees a set bit in return. When it does it records the time
 * and changes to sending a cleared bit. When it sees a cleared bit it records
 * the time and changes back to sending a set bit. The other end does the
 * reverse - it sends a cleared bit until it sees a set bit, and then sends a
 * set bit until it sees a cleared bit.
 * 
 * The time between one bit flip and the next is a round trip time. The protocol
 * uses the (local clock) times to measure the round trip and determine
 * timeliness, so this protocol works without synchronized clocks. Essentially
 * it only measures measage transmission delay and scheduling delay, not clock
 * skew.
 * 
 * However, as the clocks are not measured, the upper layers cannot rely on
 * timestamps to represent a global time reference. Accordingly the partition
 * stability time reference is meansingless across the partition, it only
 * reflects the local time when the partition stablizes. Hence view time stamps
 * are not reported.
 * 
 */
public class PingProtocolImpl extends BitView implements HeartbeatProtocol {

    /**
     * 
     */
    private static final long                serialVersionUID = 1L;
    private InetSocketAddress                address          = null;
    private boolean                          expected         = true;
    private transient final ViewListener     listener;
    private static final Logger              log              = LoggerFactory.getLogger(PingProtocolImpl.class.getCanonicalName());
    private Identity                         me               = null;
    private long                             pingTime         = 0;
    private Identity                         sender           = null;
    private final transient PingHeartbeatMsg sharedData;
    private boolean                          terminated       = false;
    private long                             time             = 0;
    private long                             viewNumber       = 0;

    /**
     * Constructor - create a heartbeat protocol implementation using the
     * information provided in a heartbeat message
     * 
     * @param hb
     * @param vl
     */
    public PingProtocolImpl(Heartbeat hb, ViewListener vl,
                            Heartbeat sharedHeartbeat) {
        super(hb.getView());
        time = hb.getTime();
        viewNumber = hb.getViewNumber();
        listener = vl;
        sender = hb.getSender();
        address = hb.getSenderAddress();
        sharedData = (PingHeartbeatMsg) sharedHeartbeat;
        me = sharedData.getSender();
        pingTime = System.currentTimeMillis();
        if (me.id < sender.id) {
            sharedData.setPingBit(sender);
            expected = true;
        } else {
            sharedData.clearPingBit(sender);
            expected = true;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * Sender interface
     * 
     * @return identity
     */
    @Override
    public Identity getSender() {
        return sender;
    }

    @Override
    public InetSocketAddress getSenderAddress() {
        return address;
    }

    /**
     * Timed interface
     * 
     * @return long
     */
    @Override
    public long getTime() {
        return time;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * indicates if the heartbeat protocol is timely. This method is called
     * periodically by the connectionSet as part of a connection cleanup action.
     * "Timelyness" is relative to the current time (timenow) and the timebound.
     * 
     * @param timenow
     *            current time
     * @param timebound
     *            expiration period
     * @return true if expried, false if not
     */
    @Override
    public boolean isNotTimely(long timenow, long timebound) {
        if (terminated) {
            return true;
        }
        return timenow - pingTime > timebound;
    }

    @Override
    public boolean isNotTimelyMsgConnection(long timenow, long timebound) {
        return isNotTimely(timenow, timebound);
    }

    /**
     * indicates if the heartbeat protocol has quiesced. When the protocol has
     * expired it will be inactive for a period before it can be removed. This
     * is used to avoid another instance of the protocol being created as soon
     * as this one expires (the partition protocol requires that connections
     * remain inactive for a period after they expire). The quiescence is
     * relative to the current time and the quiesce timeout.
     * 
     * @param timenow
     *            current time
     * @param quiesce
     *            quesce timeout - must be greater than expire timeout*2
     * @return true if expired, false if not
     */
    @Override
    public boolean isQuiesced(long timenow, long quiesce) {
        return timenow - pingTime > quiesce;
    }

    /**
     * This protocol does not measure clock skew. It is in fact independent of
     * clock skew because it uses only the local clock times to determine
     * timeliness.
     * 
     * @return boolean
     */
    @Override
    public boolean measuresClockSkew() {
        return false;
    }

    /**
     * receives a heartbeat message and deals with it according to the heartbeat
     * protocol. This is protocol uses straight-forward timing of messages -
     * only he most recent message time and its information counts.
     * 
     * @param hb
     *            - heartbeat message
     * @return boolean
     */
    @Override
    public boolean receiveHeartbeat(Heartbeat hb) {

        if (!(hb instanceof PingHeartbeatMsg)) {
            log.error(me + " ping protocol received a non-ping heartbeat");
            return false;
        }

        if (terminated) {
            return false;
        }

        PingHeartbeatMsg pinghb = (PingHeartbeatMsg) hb;

        /**
         * Only bother with a heartbeat if it superceeds the last one (ordered
         * delivery is not guaranteed!)
         */
        if (pinghb.getTime() > time) {

            time = pinghb.getTime();

            /**
             * if the expected ping has been received record the time and
             * change;
             */
            if (expected == pinghb.getPingBit(me)) {
                pingTime = System.currentTimeMillis();
                expected = !expected;
                sharedData.flipPingBit(sender);
            }

            /**
             * check for a new view (view number) if the view numbers are
             * different but the views are the same then the view has changed
             * and chaged back without us noticing! We need to pick this up as a
             * real change.
             */
            if (hb.getViewNumber() != viewNumber) {
                viewNumber = hb.getViewNumber();
                view.copyFrom(hb.getView().toBitSet());
                stable.set(hb.getView().isStable());
                listener.newView(sender, this);
            }

            if (log.isTraceEnabled()) {
                log.trace("Accepting heart beat: " + hb);
            }
            return true;

        }
        if (log.isTraceEnabled()) {
            log.trace("Rejecting heart beat: " + hb);
        }
        return false;
    }

    @Override
    public void setTime(long t) {
        time = t;
    }

    @Override
    public void terminate() {
        terminated = true;
        sharedData.clearPingBit(me);
    }
}
