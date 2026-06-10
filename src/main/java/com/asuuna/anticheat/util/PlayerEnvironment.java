/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.util;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PlayerEnvironment {

    private PlayerEnvironment() {
    }

    public static boolean shouldSkipMovement(Player player) {
        return player.isDead()
            || player.isInsideVehicle()
            || player.getGameMode() == GameMode.CREATIVE
            || player.getGameMode() == GameMode.SPECTATOR
            || player.getAllowFlight()
            || player.isFlying()
            || player.isGliding();
    }

    public static boolean shouldSkipBlockTimings(Player player) {
        return player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR;
    }

    public static int speedAmplifier(Player player) {
        PotionEffect effect = player.getPotionEffect(PotionEffectType.SPEED);
        return effect == null ? 0 : effect.getAmplifier() + 1;
    }

    public static boolean hasVerticalMovementEffect(Player player) {
        return player.hasPotionEffect(PotionEffectType.JUMP)
            || player.hasPotionEffect(PotionEffectType.LEVITATION)
            || player.hasPotionEffect(PotionEffectType.SLOW_FALLING);
    }

    public static boolean isNearSafeMovementBlock(Player player) {
        Location location = player.getLocation();
        return isSafeBlock(location.getBlock())
            || isSafeBlock(location.clone().add(0.0D, -0.5D, 0.0D).getBlock())
            || isSafeBlock(location.clone().add(0.0D, 1.0D, 0.0D).getBlock());
    }

    public static boolean hasGroundNear(Player player) {
        Location location = player.getLocation();
        for (double x = -0.31D; x <= 0.31D; x += 0.62D) {
            for (double z = -0.31D; z <= 0.31D; z += 0.62D) {
                Block block = location.clone().add(x, -0.08D, z).getBlock();
                if (!block.isPassable()) {
                    return true;
                }
            }
        }
        return player.isOnGround();
    }

    public static boolean isOnLiquidSurface(Location location) {
        Block feet = location.getBlock();
        Block below = location.clone().add(0.0D, -0.08D, 0.0D).getBlock();
        return !feet.isLiquid() && below.isLiquid();
    }

    private static boolean isSafeBlock(Block block) {
        Material material = block.getType();
        String name = material.name();
        return material == Material.WATER
            || material == Material.LAVA
            || name.contains("LADDER")
            || name.contains("VINE")
            || name.contains("SCAFFOLDING")
            || name.contains("WEB")
            || name.contains("BUBBLE_COLUMN")
            || name.contains("SLIME")
            || name.contains("HONEY");
    }
}
