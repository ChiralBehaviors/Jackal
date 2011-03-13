/** 
 * (C) Copyright 2011 Hal Hildebrand, All Rights Reserved
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

import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocol;
import org.smartfrog.services.anubis.partition.protocols.heartbeat.HeartbeatProtocolFactory;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.views.ViewListener;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

/**
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class PhiTimedProtocolFactory implements HeartbeatProtocolFactory {
    private final Gossip gossip;

    public PhiTimedProtocolFactory(Gossip gossip) {
        super();
        this.gossip = gossip;
    }

    @Override
    public Heartbeat createMsg(Identity identity, InetSocketAddress address) {
        return new HeartbeatState(address, identity, gossip.getLocalAddress());
    }

    @Override
    public HeartbeatProtocol createProtocol(Heartbeat hb, ViewListener vl,
                                            Heartbeat sharedHeartbeat) {
        return new PhiTimedProtocol(hb, vl, sharedHeartbeat, gossip);
    }
}
