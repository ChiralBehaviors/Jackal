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
package org.smartfrog.services.anubis.partition.test.controller;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartfrog.services.anubis.partition.comms.multicast.HeartbeatCommsIntf;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;

/**
 * A snoop which allows the controller to participate in the
 * discovery/replication of the heartbeat gossip
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
public class GossipSnoop {
    private final static Logger            log = LoggerFactory.getLogger(GossipSnoop.class.getCanonicalName());

    private final HeartbeatCommsIntf       underlying;
    private volatile ScheduledFuture<?>    updateTask;
    private final ScheduledExecutorService scheduler;
    private final long                     updateInterval;
    private final TimeUnit                 unit;
    private final Heartbeat                state;

    public GossipSnoop(Heartbeat heartbeat, HeartbeatCommsIntf comms,
                       long interval, TimeUnit u) {
        state = heartbeat;
        underlying = comms;
        updateInterval = interval;
        unit = u;
        scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread daemon = new Thread(r,
                                           "Controller Gossip discovery servicing thread");
                daemon.setDaemon(true);
                return daemon;
            }
        });
    }

    @PostConstruct
    public void start() {
        if (updateTask == null) {
            underlying.start(state);
            updateTask = scheduler.schedule(updateTask(), updateInterval, unit);
        }
    }

    @PreDestroy
    public void terminate() {
        if (updateTask != null) {
            updateTask.cancel(true);
            underlying.terminate();
            updateTask = null;
        }
    }

    private Runnable updateTask() {
        return new Runnable() {

            @Override
            public void run() {
                try {
                    update();
                    underlying.sendHeartbeat(state);
                } catch (Throwable e) {
                    log.warn("Problem updating", e);
                }
            }
        };
    }

    protected void update() {
        state.setTime(state.getTime() + 1);
        underlying.sendHeartbeat(state);
    }
}
