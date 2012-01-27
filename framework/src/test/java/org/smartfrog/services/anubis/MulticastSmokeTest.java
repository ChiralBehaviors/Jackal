package org.smartfrog.services.anubis;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.testUtil.multicast.MulticastNodeCfg;

public class MulticastSmokeTest extends SmokeTest {

    @Configuration
    static class test extends MulticastNodeCfg {
        private static final AtomicInteger id = new AtomicInteger(-1);

        @Override
        @Bean
        public int node() {
            return id.incrementAndGet();
        }
    }

    @Override
    protected Class<?>[] getConfigurations() {
        return new Class[] { test.class, test.class, test.class, test.class,
                test.class, test.class, test.class };
    }

}
