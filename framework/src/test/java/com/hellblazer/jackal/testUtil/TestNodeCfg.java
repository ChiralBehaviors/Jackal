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
package com.hellblazer.jackal.testUtil;

import java.security.SecureRandom;
import java.util.Random;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.configuration.JackalConfig;
import com.hellblazer.jackal.configuration.PartitionAgentConfig;
import com.hellblazer.jackal.configuration.StandardConfigurationConfig;
import com.hellblazer.jackal.configuration.ThreadConfig;

/**
 * @author hhildebrand
 * 
 */
@Configuration
@Import({ JackalConfig.class, StandardConfigurationConfig.class,
         ThreadConfig.class, PartitionAgentConfig.class, TestCfg.class })
abstract public class TestNodeCfg {

    private static final Random random = new SecureRandom();

    private static volatile int magic  = random.nextInt();

    public static int getMagicValue() {
        return magic;
    }

    public static void nextMagic() {
        magic = random.nextInt();
    }

    @Bean
    public Identity partitionIdentity() {
        return new Identity(getMagic(), node(), System.currentTimeMillis());
    }

    protected int getMagic() {
        return magic;
    }

    abstract protected int node();

}
