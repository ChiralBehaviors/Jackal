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
package com.hellblazer.anubis.partition.coms.gossip.configuration;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.Timer;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.GossipSnoop;
import org.smartfrog.services.anubis.partition.util.Epoch;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.msg.Heartbeat;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.anubis.annotations.DeployedPostProcessor;
import com.hellblazer.anubis.basiccomms.nio.SocketOptions;
import com.hellblazer.anubis.partition.coms.gossip.AdaptiveFailureDetectorFactory;
import com.hellblazer.anubis.partition.coms.gossip.Communications;
import com.hellblazer.anubis.partition.coms.gossip.FailureDetectorFactory;
import com.hellblazer.anubis.partition.coms.gossip.Gossip;
import com.hellblazer.anubis.partition.coms.gossip.HeartbeatState;
import com.hellblazer.anubis.partition.coms.gossip.PhiFailureDetectorFactory;
import com.hellblazer.anubis.partition.coms.gossip.SystemView;

/**
 * Gossip based configuration for the partition manager testing.
 * 
 * @author <a href="mailto:hal.hildebrand@gmail.com">Hal Hildebrand</a>
 * 
 */
@Configuration
public class ControllerGossipConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(
                                               ControllerGossipConfiguration.class);
    }

    @Bean
    public Communications communications() throws IOException {
        return new Communications("Test controller gossip endpoint handler "
                                  + partitionIdentity(), gossipEndpoint(),
                                  socketOptions(),
                                  Executors.newFixedThreadPool(3),
                                  Executors.newFixedThreadPool(10));

    }

    public InetAddress contactHost() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }

    @Bean
    public Controller controller() throws IOException {
        Controller controller = constructController();
        gossip().create(controller);
        return controller;
    }

    @Bean
    public DeployedPostProcessor deployedPostProcessor() {
        return new DeployedPostProcessor();
    }

    @Bean
    public FailureDetectorFactory failureDetectorFactory() {
        return phiAccrualFailureDetectorFactory();
    }

    protected FailureDetectorFactory phiAccrualFailureDetectorFactory() {
        return new PhiFailureDetectorFactory(14, 1000, heartbeatInterval()
                                                       * heartbeatTimeout(),
                                             10, 1, false);
    }

    protected FailureDetectorFactory adaptiveAccrualFailureDetectorFactory() {
        return new AdaptiveFailureDetectorFactory(0.99, 1000, 0.75,
                                                  heartbeatInterval()
                                                          * heartbeatTimeout(),
                                                  200, 1.0);
    }

    @Bean
    public Gossip gossip() throws IOException {
        return new Gossip(systemView(), new SecureRandom(), communications(),
                          gossipInterval(), gossipIntervalTimeUnit(),
                          failureDetectorFactory());
    }

    public long heartbeatInterval() {
        return 2000L;
    }

    public long heartbeatTimeout() {
        return 3L;
    }

    @Bean
    public Identity partitionIdentity() throws UnknownHostException {
        return new Identity(magic(), node(), epoch().longValue());
    }

    @Bean
    public GossipSnoop snoop() throws IOException {
        Heartbeat heartbeat = new HeartbeatState(gossip().getLocalAddress(), 0,
                                                 partitionIdentity());
        return new GossipSnoop(heartbeat, gossip(), heartbeatInterval(),
                               TimeUnit.MILLISECONDS);
    }

    @Bean
    public SystemView systemView() throws IOException {
        return new SystemView(new SecureRandom(),
                              communications().getLocalAddress(), seedHosts(),
                              quarantineDelay(), unreachableNodeDelay());
    }

    @Bean
    public Timer timer() {
        return new Timer("Partition timer", true);
    }

    protected Controller constructController() throws UnknownHostException {
        return new Controller(timer(), 1000, 300000, partitionIdentity(),
                              heartbeatTimeout(), heartbeatInterval());
    }

    protected Epoch epoch() {
        return new Epoch();
    }

    protected InetSocketAddress gossipEndpoint() throws UnknownHostException {
        return new InetSocketAddress(contactHost(), 0);
    }

    protected int gossipInterval() {
        return 1;
    }

    protected TimeUnit gossipIntervalTimeUnit() {
        return TimeUnit.SECONDS;
    }

    protected int magic() {
        return 12345;
    }

    protected int node() throws UnknownHostException {
        return Identity.getProcessUniqueId();
    }

    protected int quarantineDelay() {
        return 1000;
    }

    protected Collection<InetSocketAddress> seedHosts()
                                                       throws UnknownHostException {
        return asList(gossipEndpoint());
    }

    protected SocketOptions socketOptions() {
        return new SocketOptions();
    }

    protected int unreachableNodeDelay() {
        return 500000;
    }
}
