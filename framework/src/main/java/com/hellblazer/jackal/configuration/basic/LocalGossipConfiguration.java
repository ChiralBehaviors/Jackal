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
package com.hellblazer.jackal.configuration.basic;

import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.configuration.GossipHeartbeatAndDiscovery;
import com.hellblazer.jackal.configuration.Jackal;
import com.hellblazer.jackal.configuration.StandardConfiguration;
import com.hellblazer.pinkie.SocketOptions;

/**
 * @author hhildebrand
 * 
 */
@Configuration
@Import({ Jackal.class, StandardConfiguration.class,
         GossipHeartbeatAndDiscovery.class, GossipSeedHosts.class })
public class LocalGossipConfiguration {
    private static final Logger log = Logger.getLogger(LocalGossipConfiguration.class.getCanonicalName());

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(LocalGossipConfiguration.class);
    }

    @Bean
    public SocketOptions socketOptions() {
        return new SocketOptions();
    }

    @Bean(name = "connectionSetEndpoint")
    public InetSocketAddress connectionSetEndpoint() {
        return new InetSocketAddress("127.0.0.1", 0);
    }

    @Bean
    public Identity paritionIdentity() {
        return new Identity(0x1638, node(), System.currentTimeMillis());
    }

    private int node() {
        try {
            return Identity.getProcessUniqueId();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    public ExecutorService commonExecutors() {
        return Executors.newCachedThreadPool(new ThreadFactory() {
            int count = 0;

            @Override
            public Thread newThread(Runnable target) {
                Thread t = new Thread(
                                      target,
                                      String.format("Common executor[%s] for node[%s]",
                                                    count++, node()));
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.SEVERE,
                                String.format("Exception on %s", t), e);
                    }
                });
                // t.setPriority(Thread.MAX_PRIORITY);
                return t;
            }
        });
    }

    @Bean(name = "gossipEndpoint")
    public InetSocketAddress gossipEndpoint() {
        return new InetSocketAddress("127.0.0.1", 0);
    }
}
