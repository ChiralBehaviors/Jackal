package org.smartfrog.services.anubis;

import org.springframework.context.annotation.Configuration;

import com.hellblazer.jackal.testUtil.multicast.MulticastNodeCfg;

public class MulticastSmokeTest extends SmokeTest {

    @Configuration
    static class testA extends MulticastNodeCfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class testB extends MulticastNodeCfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class testC extends MulticastNodeCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class testD extends MulticastNodeCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class testE extends MulticastNodeCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class testF extends MulticastNodeCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class testG extends MulticastNodeCfg {
        @Override
        public int node() {
            return 6;
        }
    }

    @Override
    protected Class<?>[] getConfigurations() {
        return new Class[] { testA.class, testB.class, testC.class,
                testD.class, testE.class, testF.class, testG.class };
    }

}
