package org.smartfrog.services.anubis;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.Logger;

import org.smartfrog.services.anubis.partition.test.controller.Controller;
import org.smartfrog.services.anubis.partition.test.controller.ControllerConfiguration;
import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.annotations.DeployedPostProcessor;

public class MulticastPartitionTest extends PartitionTest {

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
        protected Controller constructController() throws UnknownHostException {
            return new MyController(timer(), 1000, 300000, partitionIdentity(),
                                    heartbeatTimeout(), heartbeatInterval());
        }

    }

    @Configuration
    static class node0 extends nodeCfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class node1 extends nodeCfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class node10 extends nodeCfg {
        @Override
        public int node() {
            return 10;
        }
    }

    @Configuration
    static class node11 extends nodeCfg {
        @Override
        public int node() {
            return 11;
        }
    }

    @Configuration
    static class node12 extends nodeCfg {
        @Override
        public int node() {
            return 12;
        }
    }

    @Configuration
    static class node13 extends nodeCfg {
        @Override
        public int node() {
            return 13;
        }
    }

    @Configuration
    static class node14 extends nodeCfg {
        @Override
        public int node() {
            return 14;
        }
    }

    @Configuration
    static class node15 extends nodeCfg {
        @Override
        public int node() {
            return 15;
        }
    }

    @Configuration
    static class node16 extends nodeCfg {
        @Override
        public int node() {
            return 16;
        }
    }

    @Configuration
    static class node17 extends nodeCfg {
        @Override
        public int node() {
            return 17;
        }
    }

    @Configuration
    static class node18 extends nodeCfg {
        @Override
        public int node() {
            return 18;
        }
    }

    @Configuration
    static class node19 extends nodeCfg {
        @Override
        public int node() {
            return 19;
        }
    }

    @Configuration
    static class node2 extends nodeCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends nodeCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends nodeCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends nodeCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends nodeCfg {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends nodeCfg {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends nodeCfg {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends nodeCfg {
        @Override
        public int node() {
            return 9;
        }
    }

    static class nodeCfg extends BasicConfiguration {
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
    }

    @Override
    protected Class<?>[] getConfigs() {
        return new Class[] { node0.class, node1.class, node2.class,
                node3.class, node4.class, node5.class, node6.class,
                node7.class, node8.class, node9.class, node10.class,
                node11.class, node12.class, node13.class, node14.class,
                node15.class, node16.class, node17.class, node18.class,
                node19.class };
    }

    @Override
    protected Class<?> getControllerConfig() {
        return MyControllerConfig.class;
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(getClass().getCanonicalName());
    }
}
