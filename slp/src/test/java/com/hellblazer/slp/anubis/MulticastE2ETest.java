package com.hellblazer.slp.anubis;

import java.util.logging.Logger;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.hellblazer.jackal.testUtil.multicast.MulticastControllerConfig;
import com.hellblazer.jackal.testUtil.multicast.MulticastNodeCfg;

public class MulticastE2ETest extends EndToEndTest {

    @Configuration
    static class node0 extends multicastSlpConfig {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class node1 extends multicastSlpConfig {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class node2 extends multicastSlpConfig {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class node3 extends multicastSlpConfig {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class node4 extends multicastSlpConfig {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class node5 extends multicastSlpConfig {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class node6 extends multicastSlpConfig {
        @Override
        public int node() {
            return 6;
        }
    }

    @Configuration
    static class node7 extends multicastSlpConfig {
        @Override
        public int node() {
            return 7;
        }
    }

    @Configuration
    static class node8 extends multicastSlpConfig {
        @Override
        public int node() {
            return 8;
        }
    }

    @Configuration
    static class node9 extends multicastSlpConfig {
        @Override
        public int node() {
            return 9;
        }
    }

    @Import({ SlpConfig.class })
    @Configuration
    static abstract class multicastSlpConfig extends MulticastNodeCfg {

    }

    @Override
    protected Class<?>[] getConfigs() {
        return new Class<?>[] { node0.class, node1.class, node2.class,
                node3.class, node4.class, node5.class, node6.class,
                node7.class, node8.class, node9.class };
    }

    @Override
    protected Class<?> getControllerConfig() {
        return MulticastControllerConfig.class;
    }

    @Override
    protected Logger getLogger() {
        return Logger.getLogger(MulticastE2ETest.class.getCanonicalName());
    }

}
