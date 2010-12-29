package org.smartfrog.services.anubis.partition.test.mainconsole;

import java.net.UnknownHostException;

import org.smartfrog.services.anubis.BasicConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PmConfiguration extends BasicConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(PmConfiguration.class);
    }

    @Bean
    public Controller controller() throws UnknownHostException {
        Controller controller = new Controller();
        controller.setAddress(heartbeatGroup());
        controller.setTimer(timer());
        controller.setCheckPeriod(1000);
        controller.setExpirePeriod(10000);
        controller.setIdentity(partitionIdentity());
        controller.setHeartbeatTimeout(heartbeatTimeout());
        controller.setHeartbeatInterval(heartbeatInterval());
        return controller;
    }
}
