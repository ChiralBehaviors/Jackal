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

import org.smartfrog.services.anubis.locator.AnubisLocator;
import org.smartfrog.services.anubis.locator.Locator;
import org.smartfrog.services.anubis.partition.PartitionManager;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.hellblazer.jackal.configuration.JackalConfig.HeartbeatConfiguration;

/**
 * @author hhildebrand
 * 
 */
@Configuration
public class LocatorsConfig {
    @Autowired
    private HeartbeatConfiguration heartbeatConfiguration;
    @Autowired
    private PartitionManager       partitionManager;
    @Autowired
    private Identity               partitionIdentity;

    @Bean
    @Primary
    public AnubisLocator locator() {
        Locator locator = new Locator(partitionIdentity, partitionManager,
                                      heartbeatConfiguration.heartbeatInterval,
                                      heartbeatConfiguration.heartbeatTimeout);
        return locator;
    }
}
