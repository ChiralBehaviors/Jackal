package org.smartfrog.services.anubis;

import java.util.logging.Logger;

import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.testUtil.multicast.MulticastControllerConfig;
import com.hellblazer.jackal.testUtil.multicast.MulticastNodeCfg;

public class MulticastPartitionTest extends PartitionTest {

    @Configuration
    static class node0 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class node1 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class node10 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 10;
        }
    }

    @Configuration
    static class node11 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 11;
        }
    }

    @Configuration
    static class node12 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 12;
        }
    }

    @Configuration
    static class node13 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 13;
        }
    }

    @Configuration
    static class node14 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 14;
        }
    }

    @Configuration
    static class node15 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 15;
        }
    }

    @Configuration
    static class node16 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 16;
        }
    }

    @Configuration
    static class node17 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 17;
        }
    }

    @Configuration
    static class node18 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 18;
        }
    }

    @Configuration
    static class node19 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 19;
        }
    }

    @Configuration
    static class node2 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends MulticastNodeCfg {
        @Override
        public int node() {
            return 9;
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
        return MulticastControllerConfig.class;
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(getClass().getCanonicalName());
    }
}
