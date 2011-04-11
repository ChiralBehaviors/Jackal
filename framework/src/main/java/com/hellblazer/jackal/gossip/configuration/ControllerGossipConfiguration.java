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
package com.hellblazer.jackal.gossip.configuration;

import static com.hellblazer.jackal.nio.ServerSocketChannelHandler.bind;
import static com.hellblazer.jackal.nio.ServerSocketChannelHandler.getLocalAddress;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
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

import com.hellblazer.jackal.annotations.DeployedPostProcessor;
import com.hellblazer.jackal.gossip.FailureDetectorFactory;
import com.hellblazer.jackal.gossip.Gossip;
import com.hellblazer.jackal.gossip.GossipCommunications;
import com.hellblazer.jackal.gossip.HeartbeatState;
import com.hellblazer.jackal.gossip.SystemView;
import com.hellblazer.jackal.gossip.fd.AdaptiveFailureDetectorFactory;
import com.hellblazer.jackal.gossip.fd.PhiFailureDetectorFactory;
import com.hellblazer.jackal.gossip.fd.TimedFailureDetectorFactory;
import com.hellblazer.jackal.gossip.tcp.TcpCommunications;
import com.hellblazer.jackal.gossip.udp.UdpCommunications;
import com.hellblazer.jackal.nio.SocketOptions;

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
    public GossipCommunications communications() throws IOException {
        return udpCommunications();
    }

    protected GossipCommunications udpCommunications() throws IOException {
        return new UdpCommunications(gossipEndpoint(),
                                     Executors.newFixedThreadPool(3));
    }

    protected GossipCommunications tcpCommunications() throws IOException,
                                                      UnknownHostException {
        ServerSocketChannel channel = bind(socketOptions(), gossipEndpoint());
        return new TcpCommunications("Test controller gossip endpoint handler "
                                     + partitionIdentity(), channel,
                                     getLocalAddress(channel), socketOptions(),
                                     Executors.newFixedThreadPool(3),
                                     Executors.newFixedThreadPool(3));
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
        heartbeat.setTime(0);
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

    protected FailureDetectorFactory adaptiveAccrualFailureDetectorFactory() {
        return new AdaptiveFailureDetectorFactory(0.99, 1000, 0.75,
                                                  heartbeatInterval()
                                                          * heartbeatTimeout(),
                                                  200, 1.0);
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

    protected FailureDetectorFactory phiAccrualFailureDetectorFactory() {
        return new PhiFailureDetectorFactory(3.5, 1000, heartbeatInterval(), 1,
                                             1, false);
    }

    protected FailureDetectorFactory timedFailureDetectorFactory() {
        return new TimedFailureDetectorFactory(heartbeatInterval()
                                               * heartbeatTimeout());
    }

    protected int quarantineDelay() {
        return (int) (heartbeatInterval() * (heartbeatTimeout() + 1));
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
