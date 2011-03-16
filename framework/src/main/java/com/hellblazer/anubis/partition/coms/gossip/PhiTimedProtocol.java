/** 
 * (C) Copyright 2010 Hal Hildebrand, All Rights Reserved
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.hellblazer.anubis.partition.coms.gossip;

import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocol;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.BitView;
import org.smartfrog.services.anubis.partition.views.ViewListener;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

/**
 * A variation of the timed protocol which relies on the phi accrual failure
 * detector to determine whether a heartbeat connection is timely or not.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class PhiTimedProtocol extends BitView implements HeartbeatProtocol {

    private static final Logger     log              = Logger.getLogger(PhiTimedProtocol.class.getCanonicalName());
    private static final long       serialVersionUID = 1L;

    private final InetSocketAddress address;
    private final Gossip            gossip;
    private final ViewListener      listener;
    private final Identity          sender;
    private boolean                 terminated       = false;
    private long                    time             = 0;
    private long                    viewNumber       = 0;
    private final InetSocketAddress heartbeatAddress;

    public PhiTimedProtocol(Heartbeat hb, ViewListener vl,
                            Heartbeat sharedHeartbeat, Gossip gossipService) {
        super(hb.getView());
        assert hb instanceof HeartbeatState;
        time = hb.getTime();
        viewNumber = hb.getViewNumber();
        listener = vl;
        sender = hb.getSender();
        address = hb.getSenderAddress();
        assert address != null;
        gossip = gossipService;
        heartbeatAddress = ((HeartbeatState) hb).getHeartbeatAddress();
        assert heartbeatAddress != null;
    }

    @Override
    public Identity getSender() {
        return sender;
    }

    @Override
    public InetSocketAddress getSenderAddress() {
        return address;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public boolean isNotTimely(long timenow, long timebound) {
        return gossip.shouldConvict(heartbeatAddress, timenow);
    }

    @Override
    public boolean isQuiesced(long timenow, long quiesce) {
        return timenow - time > quiesce;
    }

    @Override
    public boolean measuresClockSkew() {
        return true;
    }

    @Override
    public boolean receiveHeartbeat(Heartbeat hb) {

        if (terminated) {
            return false;
        }

        /**
         * Only bother with a heartbeat if it superceeds the last one (ordered
         * delivery is not guaranteed!)
         */
        if (hb.getTime() > time) {
            time = hb.getTime();
            boolean viewChanged = false;
            boolean timeStampChanged = false;

            /**
             * Check for a new view time stamp (as opposed to the heartbeat
             * time!)
             */
            if (timeStamp != hb.getView().getTimeStamp()) {
                timeStamp = hb.getView().getTimeStamp();
                timeStampChanged = true;
            }

            /**
             * check for a new view (view number) if the view numbers are
             * different but the views are the same then the view has changed
             * and chaged back without us noticing! We need to pick this up as a
             * real change.
             */
            if (hb.getViewNumber() != viewNumber) {
                viewNumber = hb.getViewNumber();
                view = hb.getView().toBitSet();
                stable = hb.getView().isStable();
                viewChanged = true;
            }

            /**
             * generate appropriate notification to next protocol layer if
             * something changed.
             */
            if (viewChanged) {
                listener.newView(sender, this);
            } else if (timeStampChanged) {
                listener.newViewTime(sender, this);
            }

            if (log.isLoggable(Level.FINEST)) {
                log.finest("Accepting heart beat: " + hb);
            }
            return true;

        }
        if (log.isLoggable(Level.FINEST)) {
            log.finest("Rejecting heart beat: " + hb);
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
    }

}