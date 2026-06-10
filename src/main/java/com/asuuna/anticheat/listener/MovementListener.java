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
import com.asuuna.anticheat.util.ServerMetrics;
import java.util.ArrayDeque;
import java.util.Deque;
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
        MovementState state = state(player);
        if (from.distanceSquared(to) == 0.0D || PlayerEnvironment.shouldSkipMovement(player)) {
            state.resetAirTicks();
            state.resetLiquidTicks();
            return;
        }

        if (state.consumeTeleportGrace()) {
            return;
        }

        checkTimer(event, player, state, from, to);
        checkSpeed(event, player, from, to);
        checkFlight(event, player, state, from, to);
        checkStep(event, player, from, to);
        checkLiquidWalk(event, player, state, from, to);
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

    private void checkTimer(PlayerMoveEvent event, Player player, MovementState state, Location from, Location to) {
        CheckSettings settings = config.settings(CheckType.TIMER);
        if (!settings.isEnabled()) {
            return;
        }

        double horizontal = horizontalDistance(from, to);
        if (horizontal < settings.number("min-horizontal-distance", 0.015D)) {
            return;
        }

        long now = System.currentTimeMillis();
        long sampleMillis = Math.max(500L, (long) settings.number("sample-millis", 1000.0D));
        int samples = state.recordMoveSample(now, sampleMillis);
        int maxMoves = (int) settings.number("max-moves", 28.0D);
        int pingAllowance = Math.min(6, Math.max(0, ServerMetrics.ping(player) / 75));
        int allowed = maxMoves + pingAllowance;
        if (samples > allowed) {
            boolean cancel = violations.flag(player, CheckType.TIMER, 0.8D,
                "moves=" + samples + " allowed=" + allowed + " window=" + sampleMillis + "ms");
            if (cancel) {
                event.setTo(from);
            }
        }
    }

    private void checkFlight(PlayerMoveEvent event, Player player, MovementState state, Location from, Location to) {
        CheckSettings settings = config.settings(CheckType.FLIGHT);
        if (!settings.isEnabled() || PlayerEnvironment.isNearSafeMovementBlock(player)) {
            state.resetAirTicks();
            return;
        }

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

    private void checkStep(PlayerMoveEvent event, Player player, Location from, Location to) {
        CheckSettings settings = config.settings(CheckType.STEP);
        if (!settings.isEnabled() || PlayerEnvironment.isNearSafeMovementBlock(player)
            || PlayerEnvironment.hasVerticalMovementEffect(player)) {
            return;
        }

        double dy = to.getY() - from.getY();
        double horizontal = horizontalDistance(from, to);
        double maxGain = settings.number("max-y-gain", 0.95D);
        double minHorizontal = settings.number("min-horizontal-distance", 0.08D);
        if (dy > maxGain && horizontal > minHorizontal && player.getVelocity().getY() < 0.35D) {
            boolean cancel = violations.flag(player, CheckType.STEP, 1.2D,
                "dy=" + round(dy) + " horizontal=" + round(horizontal) + " max=" + round(maxGain));
            if (cancel) {
                event.setTo(from);
            }
        }
    }

    private void checkLiquidWalk(PlayerMoveEvent event, Player player, MovementState state, Location from, Location to) {
        CheckSettings settings = config.settings(CheckType.LIQUID_WALK);
        if (!settings.isEnabled()) {
            state.resetLiquidTicks();
            return;
        }

        double horizontal = horizontalDistance(from, to);
        double dy = Math.abs(to.getY() - from.getY());
        if (!PlayerEnvironment.isOnLiquidSurface(to)
            || horizontal < settings.number("min-horizontal-distance", 0.08D)
            || dy > settings.number("max-y-change", 0.04D)) {
            state.resetLiquidTicks();
            return;
        }

        int ticks = state.incrementLiquidTicks();
        int maxTicks = (int) settings.number("max-surface-ticks", 8.0D);
        if (ticks > maxTicks) {
            boolean cancel = violations.flag(player, CheckType.LIQUID_WALK, 1.0D,
                "surfaceTicks=" + ticks + " horizontal=" + round(horizontal));
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

    private double horizontalDistance(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static final class MovementState {
        private final Deque<Long> moveSamples = new ArrayDeque<>();
        private int airTicks;
        private int teleportGraceTicks;
        private int liquidTicks;

        private int incrementAirTicks() {
            return ++airTicks;
        }

        private void resetAirTicks() {
            airTicks = 0;
        }

        private int recordMoveSample(long nowMillis, long sampleMillis) {
            moveSamples.addLast(nowMillis);
            while (!moveSamples.isEmpty() && nowMillis - moveSamples.peekFirst() > sampleMillis) {
                moveSamples.removeFirst();
            }
            return moveSamples.size();
        }

        private int incrementLiquidTicks() {
            return ++liquidTicks;
        }

        private void resetLiquidTicks() {
            liquidTicks = 0;
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
