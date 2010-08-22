package org.smartfrog.services.anubis;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import junit.framework.TestCase;

public class TestBasicConfiguration extends TestCase {
 
    public void testConstruct() {
        @SuppressWarnings("unused")
        ApplicationContext ctx = new AnnotationConfigApplicationContext(BasicConfiguration.class);
        
    }
}
