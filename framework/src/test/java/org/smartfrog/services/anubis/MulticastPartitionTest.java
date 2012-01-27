package org.smartfrog.services.anubis;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.testUtil.multicast.MulticastControllerConfig;
import com.hellblazer.jackal.testUtil.multicast.MulticastNodeCfg;

public class MulticastPartitionTest extends PartitionTest {

    @Configuration
    static class member extends MulticastNodeCfg {
        private static final AtomicInteger id = new AtomicInteger(-1);

        @Override
        @Bean
        public int node() {
            return id.incrementAndGet();
        }

        public static void reset() {
            id.set(-1);
        }
    }

    @Override
    protected Class<?>[] getConfigs() {
        return new Class[] { member.class, member.class, member.class,
                member.class, member.class, member.class, member.class,
                member.class, member.class, member.class, member.class,
                member.class, member.class, member.class, member.class,
                member.class, member.class, member.class, member.class,
                member.class };
    }

    @Override
    protected void setUp() throws Exception {
        member.reset();
        super.setUp();
    }

    @Override
    protected Class<?> getControllerConfig() {
        return MulticastControllerConfig.class;
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(getClass().getCanonicalName());
    }
}
