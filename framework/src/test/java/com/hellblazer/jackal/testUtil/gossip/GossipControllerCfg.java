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
package com.hellblazer.jackal.testUtil.gossip;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

import com.hellblazer.jackal.configuration.GossipHeartbeatAndDiscoveryConfig;
import com.hellblazer.jackal.configuration.GossipSnoopConfig;
import com.hellblazer.jackal.testUtil.TestCfg;
import com.hellblazer.jackal.testUtil.TestControllerConfig;
import com.hellblazer.jackal.testUtil.TestNodeCfg;

/**
 * @author hhildebrand
 * 
 */
@Configuration
@Import({ TestControllerConfig.class, GossipSnoopConfig.class, TestCfg.class,
         GossipHeartbeatAndDiscoveryConfig.class, GossipTestCfg.class })
public class GossipControllerCfg {
    private final static Logger log = LoggerFactory.getLogger(GossipControllerCfg.class);

    @Bean(name = "gossipDispatchers")
    @Lazy
    @Autowired
    public ExecutorService gossipDispatchers(Identity partitionIdentity) {
        final int id = partitionIdentity.id;
        return Executors.newCachedThreadPool(new ThreadFactory() {
            int count = 0;

            @Override
            public Thread newThread(Runnable target) {
                Thread t = new Thread(
                                      target,
                                      String.format("Gossip Dispatcher[%s] for node[%s]",
                                                    count++, id));
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.error(String.format("Exception on %s", t), e);
                    }
                });
                return t;
            }
        });
    }

    @Bean(name = "gossipEndpoint")
    public InetSocketAddress gossipEndpoint() {
        return new InetSocketAddress("127.0.0.1", 0);
    }

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
