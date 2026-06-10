/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.listener;

import com.asuuna.anticheat.check.CheckType;
import com.asuuna.anticheat.config.AntiCheatConfig;
import com.asuuna.anticheat.config.CheckSettings;
import com.asuuna.anticheat.service.ViolationService;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class CombatListener implements Listener {

    private final AntiCheatConfig config;
    private final ViolationService violations;

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
        CheckSettings settings = config.settings(CheckType.REACH);
        if (!settings.isEnabled() || player.getWorld() != target.getWorld()) {
            return;
        }

        Location eye = player.getEyeLocation();
        Location targetCenter = target.getLocation().add(0.0D, 1.0D, 0.0D);
        double distance = eye.distance(targetCenter);
        double maxDistance = settings.number("max-distance", 3.35D);
        if (distance > maxDistance) {
            boolean cancel = violations.flag(player, CheckType.REACH, 1.5D,
                "distance=" + round(distance) + " max=" + round(maxDistance));
            if (cancel) {
                event.setCancelled(true);
            }
        }
    }

    private String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
