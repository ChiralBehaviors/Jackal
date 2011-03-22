package com.hellblazer.anubis.partition.coms.gossip;

import java.util.Random;

import junit.framework.TestCase;

public class AdaptiveFailureDetectorTest extends TestCase {

    public void testDetector() throws Exception {

        AccrualFailureDetector detector = new AdaptiveFailureDetector();
        Random random = new Random(666);

        long average = 500;
        int variance = 100;
        long now = System.currentTimeMillis();

        for (int i = 0; i < 950; i++) {
            now += average;
            now += (variance / 2) - random.nextInt(variance);
            detector.record(now);
        }

        assertEquals(0.0, detector.p(now + variance), 0.01);

        now += 601;
        assertEquals(0.91, detector.p(now), 0.01);

        assertEquals(1.0, detector.p(now + 30000), 0.01);
    }
}
