package org.smartfrog.services.anubis;

import java.io.IOException;

import org.smartfrog.services.anubis.partition.util.Identity;
import org.springframework.context.annotation.Configuration;


public class MulticastSmokeTest extends SmokeTest {
    static class noTestCfg extends BasicConfiguration {

        @Override
        public boolean isControllable() {
            return false;
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

    }

    @Configuration
    static class testA extends noTestCfg {
        @Override
        public int node() {
            return 0;
        }
    }

    @Configuration
    static class testB extends noTestCfg {
        @Override
        public int node() {
            return 1;
        }
    }

    @Configuration
    static class testC extends noTestCfg {
        @Override
        public int node() {
            return 2;
        }
    }

    @Configuration
    static class testD extends noTestCfg {
        @Override
        public int node() {
            return 3;
        }
    }

    @Configuration
    static class testE extends noTestCfg {
        @Override
        public int node() {
            return 4;
        }
    }

    @Configuration
    static class testF extends noTestCfg {
        @Override
        public int node() {
            return 5;
        }
    }

    @Configuration
    static class testG extends noTestCfg {
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
