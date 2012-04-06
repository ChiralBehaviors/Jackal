package org.smartfrog.services.anubis;

import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.testUtil.TestNodeCfg;
import com.hellblazer.jackal.testUtil.multicast.MulticastControllerConfig;
import com.hellblazer.jackal.testUtil.multicast.MulticastNodeCfg;

public class MulticastPartitionTest extends PartitionTest {

    @Configuration
    static class member extends MulticastNodeCfg {
        private static final AtomicInteger id = new AtomicInteger(-1);

        public static void reset() {
            id.set(-1);
        }

        @Override
        @Bean
        public int node() {
            return id.incrementAndGet();
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
    protected Class<?> getControllerConfig() {
        return MulticastControllerConfig.class;
    }

    @Override
    protected Logger getLogger() {
        return LoggerFactory.getLogger(getClass().getCanonicalName());
    }

    @Override
    protected void setUp() throws Exception {
        TestNodeCfg.nextMagic();
        member.reset();
        super.setUp();
    }
}
