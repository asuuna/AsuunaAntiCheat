/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ViolationBucketTest {

    @Test
    void decaysViolationLevelOverTime() {
        ViolationBucket bucket = new ViolationBucket(0L);

        assertEquals(10.0D, bucket.add(10.0D, 4.0D, 0L), 0.001D);
        assertEquals(6.0D, bucket.get(4.0D, 60_000L), 0.001D);
        assertEquals(0.0D, bucket.get(4.0D, 180_000L), 0.001D);
    }

    @Test
    void ignoresNegativeAdditions() {
        ViolationBucket bucket = new ViolationBucket(0L);

        assertEquals(0.0D, bucket.add(-5.0D, 0.0D, 0L), 0.001D);
    }

    @Test
    void respectsAlertCooldown() {
        ViolationBucket bucket = new ViolationBucket(0L);

        assertTrue(bucket.shouldAlert(1000L, 1000L));
        assertFalse(bucket.shouldAlert(1000L, 1500L));
        assertTrue(bucket.shouldAlert(1000L, 2000L));
    }
}
