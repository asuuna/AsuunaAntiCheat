/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.listener;

import com.asuuna.anticheat.check.CheckType;
import com.asuuna.anticheat.config.AntiCheatConfig;
import com.asuuna.anticheat.config.CheckSettings;
import com.asuuna.anticheat.service.ViolationService;
import com.asuuna.anticheat.util.PlayerEnvironment;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public final class MovementListener implements Listener {

    private final AntiCheatConfig config;
    private final ViolationService violations;
    private final Map<UUID, MovementState> states = new ConcurrentHashMap<>();

    public MovementListener(AntiCheatConfig config, ViolationService violations) {
        this.config = config;
        this.violations = violations;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }
        if (from.distanceSquared(to) == 0.0D || PlayerEnvironment.shouldSkipMovement(player)) {
            state(player).resetAirTicks();
            return;
        }

        checkSpeed(event, player, from, to);
        checkFlight(event, player, from, to);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        MovementState state = state(event.getPlayer());
        state.markTeleportGrace();
        state.resetAirTicks();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        states.remove(event.getPlayer().getUniqueId());
    }

    private void checkSpeed(PlayerMoveEvent event, Player player, Location from, Location to) {
        CheckSettings settings = config.settings(CheckType.MOVEMENT_SPEED);
        if (!settings.isEnabled() || PlayerEnvironment.isNearSafeMovementBlock(player)) {
            return;
        }

        MovementState state = state(player);
        if (state.consumeTeleportGrace()) {
            return;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double allowed = settings.number("max-horizontal-distance", 0.85D)
            + PlayerEnvironment.speedAmplifier(player) * settings.number("speed-potion-bonus", 0.08D)
            + velocityAllowance(player.getVelocity());

        if (horizontal > allowed) {
            boolean cancel = violations.flag(player, CheckType.MOVEMENT_SPEED, 1.2D,
                "horizontal=" + round(horizontal) + " allowed=" + round(allowed));
            if (cancel) {
                event.setTo(from);
            }
        }
    }

    private void checkFlight(PlayerMoveEvent event, Player player, Location from, Location to) {
        CheckSettings settings = config.settings(CheckType.FLIGHT);
        if (!settings.isEnabled() || PlayerEnvironment.isNearSafeMovementBlock(player)) {
            state(player).resetAirTicks();
            return;
        }

        MovementState state = state(player);
        if (PlayerEnvironment.hasGroundNear(player)) {
            state.resetAirTicks();
            return;
        }

        double dy = to.getY() - from.getY();
        int airTicks = state.incrementAirTicks();
        int maxAirTicks = (int) settings.number("max-air-ticks", 24.0D);
        double maxHoverDelta = settings.number("max-hover-delta", 0.03D);

        if (airTicks > maxAirTicks && dy > -maxHoverDelta) {
            boolean cancel = violations.flag(player, CheckType.FLIGHT, 1.0D,
                "airTicks=" + airTicks + " dy=" + round(dy));
            if (cancel) {
                event.setTo(from);
            }
        }
    }

    private MovementState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), ignored -> new MovementState());
    }

    private double velocityAllowance(Vector velocity) {
        return Math.min(1.25D, Math.max(0.0D, Math.abs(velocity.getX()) + Math.abs(velocity.getZ())));
    }

    private String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static final class MovementState {
        private int airTicks;
        private int teleportGraceTicks;

        private int incrementAirTicks() {
            return ++airTicks;
        }

        private void resetAirTicks() {
            airTicks = 0;
        }

        private void markTeleportGrace() {
            teleportGraceTicks = 3;
        }

        private boolean consumeTeleportGrace() {
            if (teleportGraceTicks <= 0) {
                return false;
            }
            teleportGraceTicks--;
            return true;
        }
    }
}
