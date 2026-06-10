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
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class InteractionListener implements Listener {

    private final AntiCheatConfig config;
    private final ViolationService violations;
    private final Map<UUID, Deque<Long>> swings = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastPlace = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastBreak = new ConcurrentHashMap<>();

    public InteractionListener(AntiCheatConfig config, ViolationService violations) {
        this.config = config;
        this.violations = violations;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        CheckSettings settings = config.settings(CheckType.AUTOCLICKER);
        if (!settings.isEnabled() || player.hasPermission(config.getBypassPermission())) {
            return;
        }

        long now = System.currentTimeMillis();
        long sampleMillis = Math.max(250L, (long) settings.number("sample-millis", 1000.0D));
        Deque<Long> samples = swings.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        synchronized (samples) {
            samples.addLast(now);
            while (!samples.isEmpty() && now - samples.peekFirst() > sampleMillis) {
                samples.removeFirst();
            }
            int cps = samples.size();
            int maxCps = (int) settings.number("max-cps", 18.0D);
            if (cps > maxCps) {
                violations.flag(player, CheckType.AUTOCLICKER, 0.75D, "cps=" + cps + " max=" + maxCps);
            }
            int minConsistentSamples = (int) settings.number("min-consistent-samples", 12.0D);
            int minConsistentCps = (int) settings.number("min-consistent-cps", 12.0D);
            double minStdDev = settings.number("min-interval-std-dev", 4.0D);
            if (cps >= minConsistentCps && samples.size() >= minConsistentSamples) {
                double stdDev = intervalStdDev(samples);
                if (stdDev >= 0.0D && stdDev < minStdDev) {
                    violations.flag(player, CheckType.AUTOCLICKER, 0.9D,
                        "cps=" + cps + " intervalStdDev=" + round(stdDev) + "ms");
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (PlayerEnvironment.shouldSkipBlockTimings(player)) {
            return;
        }
        checkScaffold(event, player);

        CheckSettings settings = config.settings(CheckType.FAST_PLACE);
        if (!settings.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastPlace.put(player.getUniqueId(), now);
        if (previous == null) {
            return;
        }
        long elapsed = now - previous;
        long minDelay = (long) settings.number("min-delay-millis", 45.0D);
        if (elapsed < minDelay) {
            boolean cancel = violations.flag(player, CheckType.FAST_PLACE, 1.0D,
                "delay=" + elapsed + "ms min=" + minDelay + "ms");
            if (cancel) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (PlayerEnvironment.shouldSkipBlockTimings(player)) {
            return;
        }
        CheckSettings settings = config.settings(CheckType.FAST_BREAK);
        if (!settings.isEnabled()) {
            return;
        }
        long now = System.currentTimeMillis();
        Long previous = lastBreak.put(player.getUniqueId(), now);
        if (previous == null) {
            return;
        }
        long elapsed = now - previous;
        long minDelay = (long) settings.number("min-delay-millis", 65.0D);
        if (elapsed < minDelay) {
            boolean cancel = violations.flag(player, CheckType.FAST_BREAK, 1.0D,
                "delay=" + elapsed + "ms min=" + minDelay + "ms");
            if (cancel) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        swings.remove(uuid);
        lastPlace.remove(uuid);
        lastBreak.remove(uuid);
    }

    private void checkScaffold(BlockPlaceEvent event, Player player) {
        CheckSettings settings = config.settings(CheckType.SCAFFOLD);
        if (!settings.isEnabled() || player.isSneaking()) {
            return;
        }

        Location playerLocation = player.getLocation();
        Location blockCenter = event.getBlockPlaced().getLocation().add(0.5D, 0.5D, 0.5D);
        double horizontal = Math.hypot(playerLocation.getX() - blockCenter.getX(), playerLocation.getZ() - blockCenter.getZ());
        double vertical = playerLocation.getY() - blockCenter.getY();
        float pitch = playerLocation.getPitch();
        boolean underPlayer = vertical > settings.number("min-vertical-gap", 0.45D)
            && vertical < settings.number("max-vertical-gap", 1.85D)
            && horizontal < settings.number("max-horizontal-gap", 1.35D);

        if (underPlayer && pitch < settings.number("min-down-pitch", 45.0D)) {
            boolean cancel = violations.flag(player, CheckType.SCAFFOLD, 1.0D,
                "pitch=" + round(pitch) + " horizontal=" + round(horizontal) + " vertical=" + round(vertical));
            if (cancel) {
                event.setCancelled(true);
            }
        }
    }

    private double intervalStdDev(Deque<Long> samples) {
        if (samples.size() < 3) {
            return -1.0D;
        }
        long previous = -1L;
        double sum = 0.0D;
        int count = 0;
        for (Long sample : samples) {
            if (previous >= 0L) {
                sum += sample - previous;
                count++;
            }
            previous = sample;
        }
        if (count <= 1) {
            return -1.0D;
        }
        double average = sum / count;
        previous = -1L;
        double variance = 0.0D;
        for (Long sample : samples) {
            if (previous >= 0L) {
                double delta = (sample - previous) - average;
                variance += delta * delta;
            }
            previous = sample;
        }
        return Math.sqrt(variance / count);
    }

    private String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
