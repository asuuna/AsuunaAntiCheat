/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.model;

import com.asuuna.anticheat.check.CheckType;
import java.util.EnumMap;
import java.util.Map;

public final class PlayerViolationProfile {

    private final EnumMap<CheckType, ViolationBucket> buckets = new EnumMap<>(CheckType.class);

    public ViolationBucket bucket(CheckType type, long nowMillis) {
        return buckets.computeIfAbsent(type, ignored -> new ViolationBucket(nowMillis));
    }

    public Map<CheckType, Double> snapshot(double decayPerMinute, long nowMillis) {
        EnumMap<CheckType, Double> result = new EnumMap<>(CheckType.class);
        for (Map.Entry<CheckType, ViolationBucket> entry : buckets.entrySet()) {
            double value = entry.getValue().get(decayPerMinute, nowMillis);
            if (value > 0.01D) {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    public void reset() {
        for (ViolationBucket bucket : buckets.values()) {
            bucket.reset();
        }
        buckets.clear();
    }
}
