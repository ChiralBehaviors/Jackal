package com.hellblazer.satellite;

import junit.framework.TestCase;

import org.smartfrog.services.anubis.locator.AnubisLocator;

import com.hellblazer.anubis.satellite.Launch;

public class TestLaunch extends TestCase {
    public void testLaunch() throws Exception {
        Launch launch = new Launch();
        launch.setConfigPackage("com.hellblazer.satellite.test");
        try {
            AnubisLocator locator = launch.getLocator();
            assertNotNull(locator);
        } finally {
            launch.shutDown();
        }

    }
}
