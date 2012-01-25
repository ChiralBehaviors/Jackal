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
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.configuration.JackalConfig.HeartbeatConfiguration;
import com.hellblazer.pinkie.SocketOptions;

/**
 * @author hhildebrand
 * 
 */
@Configuration
public class PartitionControllerConfig {
    private static final Logger    log = Logger.getLogger(PartitionControllerConfig.class.getCanonicalName());

    @Autowired
    private HeartbeatConfiguration heartbeatConfiguration;
    @Autowired
    private Identity               partitionIdentity;
    @Autowired
    private SocketOptions          socketOptions;
    @Autowired
    private WireSecurity           wireSecurity;

    @Bean
    public Controller controller() throws IOException {
        return new Controller(partitionIdentity,
                              heartbeatConfiguration.heartbeatTimeout,
                              heartbeatConfiguration.heartbeatInterval,
                              socketOptions, controllerDispatchers(),
                              wireSecurity);
    }

    protected ExecutorService controllerDispatchers() {
        final int id = partitionIdentity.id;
        return Executors.newCachedThreadPool(new ThreadFactory() {
            int count = 0;

            @Override
            public Thread newThread(Runnable target) {
                Thread t = new Thread(
                                      target,
                                      String.format("Partition Controller Dispatcher[%s]",
                                                    count++, id));
                t.setDaemon(true);
                t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread t, Throwable e) {
                        log.log(Level.SEVERE,
                                String.format("Exception on %s", t), e);
                    }
                });
                return t;
            }
        });
    }
}
