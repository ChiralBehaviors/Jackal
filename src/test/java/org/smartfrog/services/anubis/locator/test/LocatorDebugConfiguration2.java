package org.smartfrog.services.anubis.locator.test;

import java.net.UnknownHostException;

import org.smartfrog.services.anubis.DefaultConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocatorDebugConfiguration2 extends DefaultConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(LocatorDebugConfiguration2.class);
    }

    @Override
    public int node() {
        return 2;
    }

    @Bean
    public Test tester() throws UnknownHostException {
        Test test = new Test("2");
        test.setLocator(locator());
        return test;
    }
}
