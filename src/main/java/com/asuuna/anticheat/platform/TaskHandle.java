/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.platform;

@FunctionalInterface
public interface TaskHandle {

    TaskHandle NOOP = () -> { };

    void cancel();
}
