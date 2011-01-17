package org.smartfrog.services.anubis.partition.test.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Timer;

import org.smartfrog.services.anubis.basiccomms.multicasttransport.MulticastAddress;
import org.smartfrog.services.anubis.partition.util.Epoch;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.anubis.annotations.DeployedPostProcessor;

@Configuration
public class ControllerConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(ControllerConfiguration.class);
    }

    @Bean
    public Controller controller() throws UnknownHostException {
        Controller controller = constructController();
        controller.setAddress(heartbeatGroup());
        controller.setTimer(timer());
        controller.setCheckPeriod(1000);
        controller.setExpirePeriod(300000);
        controller.setIdentity(partitionIdentity());
        controller.setHeartbeatTimeout(heartbeatTimeout());
        controller.setHeartbeatInterval(heartbeatInterval());
        return controller;
    }

    @Bean
    public DeployedPostProcessor deployedPostProcessor() {
        return new DeployedPostProcessor();
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
    public Identity partitionIdentity() throws UnknownHostException {
        return new Identity(magic(), node(), epoch().longValue());
    }

    @Bean
    public Timer timer() {
        return new Timer("Partition timer", true);
    }

    protected Controller constructController() {
        return new Controller();
    }

    protected Epoch epoch() {
        return new Epoch();
    }

    protected int magic() {
        return 12345;
    }

    protected int node() throws UnknownHostException {
        return Identity.getProcessUniqueId();
    }
}
