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
package com.hellblazer.jackal.configuration;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.GossipSnoop;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.hellblazer.jackal.configuration.JackalConfig.HeartbeatConfiguration;
import com.hellblazer.jackal.gossip.Gossip;
import com.hellblazer.jackal.gossip.HeartbeatState;

/**
 * @author hhildebrand
 * 
 */
@Configuration
public class GossipSnoopConfig {

    @Bean
    @Primary
    @Autowired
    public GossipSnoop snoop(Gossip gossip, Controller controller,
                             Identity partitionIdentity,
                             HeartbeatConfiguration heartbeatConfig)
                                                                    throws IOException {
        Heartbeat heartbeat = new HeartbeatState(gossip.getLocalAddress(), 0,
                                                 partitionIdentity);
        heartbeat.setTime(0);
        gossip.create(controller);
        return new GossipSnoop(heartbeat, gossip,
                               heartbeatConfig.heartbeatInterval,
                               TimeUnit.MILLISECONDS);
    }
}
