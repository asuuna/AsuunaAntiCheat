/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.check;

import java.util.Locale;

public enum CheckType {
    MOVEMENT_SPEED("movement-speed", "Movement Speed"),
    FLIGHT("flight", "Flight"),
    TIMER("timer", "Timer"),
    STEP("step", "Step"),
    LIQUID_WALK("liquid-walk", "Liquid Walk"),
    REACH("reach", "Reach"),
    COMBAT_ANGLE("combat-angle", "Combat Angle"),
    MULTI_AURA("multi-aura", "Multi Aura"),
    AUTOCLICKER("autoclicker", "AutoClicker"),
    FAST_PLACE("fast-place", "Fast Place"),
    FAST_BREAK("fast-break", "Fast Break"),
    BLOCK_REACH("block-reach", "Block Reach"),
    NUKER("nuker", "Nuker"),
    FAST_BOW("fast-bow", "Fast Bow"),
    FAST_EAT("fast-eat", "Fast Eat"),
    SCAFFOLD("scaffold", "Scaffold"),
    INVENTORY_MOVE("inventory-move", "Inventory Move"),
    INVENTORY_CLICK_SPEED("inventory-click-speed", "Inventory Click Speed"),
    NOFALL("nofall", "NoFall");

    private final String configKey;
    private final String displayName;

    CheckType(String configKey, String displayName) {
        this.configKey = configKey;
        this.displayName = displayName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static CheckType fromConfigKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (CheckType type : values()) {
            if (type.configKey.equals(normalized)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown check type: " + key);
    }
}
