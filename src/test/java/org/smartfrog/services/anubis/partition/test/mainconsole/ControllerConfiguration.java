package org.smartfrog.services.anubis.partition.test.mainconsole;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ControllerConfiguration extends PmConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(ControllerConfiguration.class);
    } 
    @Override
    public int getNode() {
        return 3;
    }
}
