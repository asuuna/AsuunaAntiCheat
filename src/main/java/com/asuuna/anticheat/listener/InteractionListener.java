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
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (PlayerEnvironment.shouldSkipBlockTimings(player)) {
            return;
        }
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
}
