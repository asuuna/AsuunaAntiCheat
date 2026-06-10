/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.listener;

import com.asuuna.anticheat.check.CheckType;
import com.asuuna.anticheat.config.AntiCheatConfig;
import com.asuuna.anticheat.config.CheckSettings;
import com.asuuna.anticheat.service.ViolationService;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;

public final class CombatListener implements Listener {

    private final AntiCheatConfig config;
    private final ViolationService violations;
    private final Map<UUID, Deque<AttackSample>> attackSamples = new ConcurrentHashMap<>();

    public CombatListener(AntiCheatConfig config, ViolationService violations) {
        this.config = config;
        this.violations = violations;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getDamager();
        Entity target = event.getEntity();
        if (player.getWorld() != target.getWorld()) {
            return;
        }

        Location eye = player.getEyeLocation();
        Location targetCenter = target.getLocation().add(0.0D, 1.0D, 0.0D);
        double distance = eye.distance(targetCenter);
        checkReach(event, player, distance);
        checkCombatAngle(event, player, eye, targetCenter, distance);
        checkMultiAura(event, player, target);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        attackSamples.remove(event.getPlayer().getUniqueId());
    }

    private void checkReach(EntityDamageByEntityEvent event, Player player, double distance) {
        CheckSettings settings = config.settings(CheckType.REACH);
        if (!settings.isEnabled()) {
            return;
        }
        double maxDistance = settings.number("max-distance", 3.35D);
        if (distance > maxDistance) {
            boolean cancel = violations.flag(player, CheckType.REACH, 1.5D,
                "distance=" + round(distance) + " max=" + round(maxDistance));
            if (cancel) {
                event.setCancelled(true);
            }
        }
    }

    private void checkCombatAngle(
        EntityDamageByEntityEvent event,
        Player player,
        Location eye,
        Location targetCenter,
        double distance
    ) {
        CheckSettings settings = config.settings(CheckType.COMBAT_ANGLE);
        if (!settings.isEnabled() || distance < settings.number("min-distance", 1.25D)) {
            return;
        }

        Vector direction = eye.getDirection();
        Vector toTarget = targetCenter.toVector().subtract(eye.toVector());
        if (direction.lengthSquared() == 0.0D || toTarget.lengthSquared() == 0.0D) {
            return;
        }

        double dot = direction.normalize().dot(toTarget.normalize());
        dot = Math.max(-1.0D, Math.min(1.0D, dot));
        double angle = Math.toDegrees(Math.acos(dot));
        double maxAngle = settings.number("max-angle", 78.0D);
        if (angle > maxAngle) {
            boolean cancel = violations.flag(player, CheckType.COMBAT_ANGLE, 1.0D,
                "angle=" + round(angle) + " max=" + round(maxAngle) + " distance=" + round(distance));
            if (cancel) {
                event.setCancelled(true);
            }
        }
    }

    private void checkMultiAura(EntityDamageByEntityEvent event, Player player, Entity target) {
        CheckSettings settings = config.settings(CheckType.MULTI_AURA);
        if (!settings.isEnabled()) {
            return;
        }

        long now = System.currentTimeMillis();
        long sampleMillis = Math.max(250L, (long) settings.number("sample-millis", 700.0D));
        Deque<AttackSample> samples = attackSamples.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        int distinctTargets;
        synchronized (samples) {
            samples.addLast(new AttackSample(now, target.getUniqueId()));
            while (!samples.isEmpty() && now - samples.peekFirst().timeMillis > sampleMillis) {
                samples.removeFirst();
            }
            Set<UUID> targets = new HashSet<>();
            for (AttackSample sample : samples) {
                targets.add(sample.targetId);
            }
            distinctTargets = targets.size();
        }

        int maxTargets = (int) settings.number("max-targets", 3.0D);
        if (distinctTargets > maxTargets) {
            boolean cancel = violations.flag(player, CheckType.MULTI_AURA, 1.2D,
                "targets=" + distinctTargets + " max=" + maxTargets + " window=" + sampleMillis + "ms");
            if (cancel) {
                event.setCancelled(true);
            }
        }
    }

    private String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static final class AttackSample {
        private final long timeMillis;
        private final UUID targetId;

        private AttackSample(long timeMillis, UUID targetId) {
            this.timeMillis = timeMillis;
            this.targetId = targetId;
        }
    }
}
