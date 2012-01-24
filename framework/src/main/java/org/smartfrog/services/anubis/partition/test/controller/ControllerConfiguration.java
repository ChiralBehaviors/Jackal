package org.smartfrog.services.anubis.partition.test.controller;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.util.Epoch;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.smartfrog.services.anubis.partition.wire.security.NoSecurityImpl;
import org.smartfrog.services.anubis.partition.wire.security.WireSecurity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.pinkie.SocketOptions;

@Configuration
public class ControllerConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(ControllerConfiguration.class);
    }

    @Bean
    public Controller controller() throws IOException {
        Controller controller = constructController();
        return controller;
    }

    @Bean
    public MulticastAddress heartbeatGroup() throws UnknownHostException {
        return new MulticastAddress(heartbeatGroupMulticastAddress(),
                                    heartbeatGroupPort(), heartbeatGroupTTL());
    }

    public InetAddress heartbeatGroupMulticastAddress()
                                                       throws UnknownHostException {
        return InetAddress.getByName("233.1.2.30");
    }

    public int heartbeatGroupPort() {
        return 1966;
    }

    public int heartbeatGroupTTL() {
        return 1;
    }

    public long heartbeatInterval() {
        return 2000L;
    }

    public long heartbeatTimeout() {
        return 3L;
    }

    @Bean
    public Identity partitionIdentity() throws IOException {
        return new Identity(magic(), node(), epoch().longValue());
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public Snoop snoop() throws UnknownHostException, IOException {
        return new Snoop(
                         "Anubis: Partition Manager Test Console heartbeat snoop",
                         new MulticastAddress(heartbeatGroupMulticastAddress(),
                                              heartbeatGroupPort(),
                                              heartbeatGroupTTL()),
                         partitionIdentity(), controller());
    }

    @Bean
    public Timer timer() {
        return new Timer("Partition timer", true);
    }

    protected Controller constructController() throws IOException {
        return new Controller(timer(), 1000, 300000, partitionIdentity(),
                              heartbeatTimeout(), heartbeatInterval(),
                              socketOptions(), dispatchExecutor(),
                              wireSecurity());
    }

    @Bean
    public WireSecurity wireSecurity() {
        return new NoSecurityImpl();
    }

    protected SocketOptions socketOptions() {
        return new SocketOptions();
    }

    protected ExecutorService dispatchExecutor() {
        return Executors.newCachedThreadPool();
    }

    protected Epoch epoch() {
        return new Epoch();
    }

    protected int magic() {
        return 12345;
    }

    protected int node() throws IOException {
        return Identity.getProcessUniqueId();
    }
}
