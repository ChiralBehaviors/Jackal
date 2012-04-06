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
package com.hellblazer.jackal.testUtil.multicast;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.configuration.MulticastHeartbeatAndDiscoveryConfig;
import com.hellblazer.jackal.configuration.MulticastSnoopConfig;
import com.hellblazer.jackal.testUtil.TestCfg;
import com.hellblazer.jackal.testUtil.TestControllerConfig;
import com.hellblazer.jackal.testUtil.TestNodeCfg;

/**
 * @author hhildebrand
 * 
 */
@Configuration
@Import({ TestControllerConfig.class, MulticastSnoopConfig.class,
         TestCfg.class, MulticastHeartbeatAndDiscoveryConfig.class,
         MulticastTestCfg.class })
public class MulticastControllerConfig {

    @Bean
    public Identity partitionIdentity() {
        return new Identity(getMagic(), node(), System.currentTimeMillis());
    }

    protected int getMagic() {
        return TestNodeCfg.getMagicValue();
    }

    protected int node() {
        return 2047;
    }
}
