/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CheckTypeTest {

    @Test
    void resolvesConfigKeysCaseInsensitively() {
        assertEquals(CheckType.MOVEMENT_SPEED, CheckType.fromConfigKey("movement-speed"));
        assertEquals(CheckType.AUTOCLICKER, CheckType.fromConfigKey("AUTOCLICKER"));
    }

    @Test
    void everyCheckResolvesItsOwnConfigKey() {
        for (CheckType type : CheckType.values()) {
            assertEquals(type, CheckType.fromConfigKey(type.getConfigKey()));
        }
    }

    @Test
    void rejectsUnknownKeys() {
        assertThrows(IllegalArgumentException.class, () -> CheckType.fromConfigKey("unknown"));
    }
}
