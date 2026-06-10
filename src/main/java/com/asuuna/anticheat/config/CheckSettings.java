/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class CheckSettings {

    private final boolean enabled;
    private final double alertVl;
    private final double cancelVl;
    private final Map<String, Double> numbers;

    public CheckSettings(boolean enabled, double alertVl, double cancelVl, Map<String, Double> numbers) {
        this.enabled = enabled;
        this.alertVl = Math.max(0.0D, alertVl);
        this.cancelVl = Math.max(0.0D, cancelVl);
        this.numbers = Collections.unmodifiableMap(new HashMap<>(numbers));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public double getAlertVl() {
        return alertVl;
    }

    public double getCancelVl() {
        return cancelVl;
    }

    public double number(String key, double fallback) {
        return numbers.getOrDefault(key, fallback);
    }
}
