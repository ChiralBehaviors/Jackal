package org.smartfrog.services.anubis.locator.test;

import org.smartfrog.services.anubis.BasicConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocatorDebugConfiguration extends BasicConfiguration {

    public static void main(String[] argv) {
        new AnnotationConfigApplicationContext(LocatorDebugConfiguration.class);
    }

    @Bean
    public Test tester() {
        Test test = new Test("1");
        test.setLocator(locator());
        return test;
    }
}
