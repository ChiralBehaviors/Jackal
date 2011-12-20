package com.hellblazer.slp.anubis;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.BasicConfiguration;
import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.ControllerConfiguration;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.uuid.NoArgGenerator;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.hellblazer.jackal.annotations.DeployedPostProcessor;
import com.hellblazer.slp.ServiceScope;

public class MulticastE2ETest extends EndToEndTest {

    @Configuration
    static class MyControllerConfig extends ControllerConfiguration {
        @Override
        @Bean
        public DeployedPostProcessor deployedPostProcessor() {
            return new DeployedPostProcessor();
        }

        @Override
        public int heartbeatGroupTTL() {
            return 0;
        }

        @Override
        public int magic() {
            try {
                return Identity.getMagicFromLocalIpAddress();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        protected Controller constructController() throws IOException {
            return new MyController(timer(), 1000, 300000, partitionIdentity(),
                                    heartbeatTimeout(), heartbeatInterval(),
                                    socketOptions(), dispatchExecutor(),
                                    wireSecurity());
        }
    }

    @Configuration
    static class node0 extends slpConfig {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class node1 extends slpConfig {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class node2 extends slpConfig {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends slpConfig {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends slpConfig {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends slpConfig {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends slpConfig {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends slpConfig {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends slpConfig {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends slpConfig {
        @Override
        public int node() {
            return 9;
        }
    }

    static class slpConfig extends BasicConfiguration {

        @Bean
        public ServiceScope anubisScope() {
            return new AnubisScope(stateName(), locator(),
                                   Executors.newSingleThreadExecutor(),
                                   uuidGenerator());
        }

        @Override
        public int getMagic() {
            try {
                return Identity.getMagicFromLocalIpAddress();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public int heartbeatGroupTTL() {
            return 0;
        }

        protected String stateName() {
            return "Test Scope";
        }

        protected NoArgGenerator uuidGenerator() {
            return new RandomBasedGenerator(new Random(node()));
        }
    }

    @Override
    protected Class<?>[] getConfigs() {
        return new Class<?>[] { node0.class, node1.class, node2.class,
                        node3.class, node4.class, node5.class, node6.class,
                        node7.class, node8.class, node9.class };
    }

    @Override
    protected Class<?> getControllerConfig() {
        return MyControllerConfig.class;
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(MulticastE2ETest.class.getCanonicalName());
    }

}
