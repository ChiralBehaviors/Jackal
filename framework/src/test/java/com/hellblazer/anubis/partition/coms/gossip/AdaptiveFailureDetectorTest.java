package com.hellblazer.anubis.partition.coms.gossip;

import java.util.Random;

import junit.framework.TestCase;

public class AdaptiveFailureDetectorTest extends TestCase {

    public void testDetector() throws Exception {

        AccrualFailureDetector detector = new AdaptiveFailureDetectorFactory(
                                                                             0.95,
                                                                             1000,
                                                                             0.95,
                                                                             500,
                                                                             0,
                                                                             0.0).create();
        Random random = new Random(666);

        long average = 500;
        int variance = 100;
        long now = System.currentTimeMillis();

        for (int i = 0; i < 950; i++) {
            now += average;
            now += (variance / 2) - random.nextInt(variance);
            detector.record(now);
        }

        assertEquals(false, detector.shouldConvict(now + variance));

        now += 573;
        assertFalse(detector.shouldConvict(now));

        assertTrue(detector.shouldConvict(now + 30000));
    }
}
