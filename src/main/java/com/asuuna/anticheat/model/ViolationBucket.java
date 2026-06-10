/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.model;

public final class ViolationBucket {

    private double level;
    private long lastUpdatedMillis;
    private long lastAlertMillis;

    public ViolationBucket(long nowMillis) {
        this.lastUpdatedMillis = nowMillis;
    }

    public synchronized double add(double amount, double decayPerMinute, long nowMillis) {
        decay(decayPerMinute, nowMillis);
        level += Math.max(0.0D, amount);
        lastUpdatedMillis = nowMillis;
        return level;
    }

    public synchronized double get(double decayPerMinute, long nowMillis) {
        decay(decayPerMinute, nowMillis);
        return level;
    }

    public synchronized boolean shouldAlert(long cooldownMillis, long nowMillis) {
        if (nowMillis - lastAlertMillis < cooldownMillis) {
            return false;
        }
        lastAlertMillis = nowMillis;
        return true;
    }

    public synchronized void reset() {
        level = 0.0D;
        lastUpdatedMillis = System.currentTimeMillis();
        lastAlertMillis = 0L;
    }

    private void decay(double decayPerMinute, long nowMillis) {
        if (level <= 0.0D || decayPerMinute <= 0.0D) {
            lastUpdatedMillis = nowMillis;
            return;
        }
        long elapsed = Math.max(0L, nowMillis - lastUpdatedMillis);
        if (elapsed == 0L) {
            return;
        }
        double decay = decayPerMinute * (elapsed / 60000.0D);
        level = Math.max(0.0D, level - decay);
        lastUpdatedMillis = nowMillis;
    }
}
