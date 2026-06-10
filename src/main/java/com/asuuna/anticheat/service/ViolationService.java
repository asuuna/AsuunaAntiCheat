/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.service;

import com.asuuna.anticheat.check.CheckType;
import com.asuuna.anticheat.config.AntiCheatConfig;
import com.asuuna.anticheat.config.CheckSettings;
import com.asuuna.anticheat.model.PlayerViolationProfile;
import com.asuuna.anticheat.model.ViolationBucket;
import com.asuuna.anticheat.platform.PlatformScheduler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class ViolationService {

    private final Plugin plugin;
    private final AntiCheatConfig config;
    private final PlatformScheduler scheduler;
    private final StaffAlertService alerts;
    private final AlertLogService alertLogService;
    private final Map<UUID, PlayerViolationProfile> profiles = new ConcurrentHashMap<>();

    public ViolationService(
        Plugin plugin,
        AntiCheatConfig config,
        PlatformScheduler scheduler,
        StaffAlertService alerts,
        AlertLogService alertLogService
    ) {
        this.plugin = plugin;
        this.config = config;
        this.scheduler = scheduler;
        this.alerts = alerts;
        this.alertLogService = alertLogService;
    }

    public boolean flag(Player player, CheckType type, double amount, String detail) {
        if (player == null || !player.isOnline() || player.hasPermission(config.getBypassPermission())) {
            return false;
        }

        CheckSettings settings = config.settings(type);
        if (!settings.isEnabled()) {
            return false;
        }

        long now = System.currentTimeMillis();
        PlayerViolationProfile profile = profiles.computeIfAbsent(player.getUniqueId(), ignored -> new PlayerViolationProfile());
        ViolationBucket bucket = profile.bucket(type, now);
        double violationLevel = bucket.add(amount, config.getViolationDecayPerMinute(), now);

        if (violationLevel >= settings.getAlertVl() && bucket.shouldAlert(config.getAlertCooldownMillis(), now)) {
            alerts.alert(player.getName(), type.getDisplayName(), violationLevel, detail);
            if (config.isLogAlerts()) {
                alertLogService.log(player.getName(), type.getDisplayName(), violationLevel, detail);
            }
        }

        if (config.isPunishmentsEnabled() && violationLevel >= config.getPunishmentMaxViolations()) {
            punish(player, type, violationLevel);
        }

        return violationLevel >= settings.getCancelVl();
    }

    public Map<CheckType, Double> snapshot(UUID uuid) {
        PlayerViolationProfile profile = profiles.get(uuid);
        if (profile == null) {
            return Map.of();
        }
        return profile.snapshot(config.getViolationDecayPerMinute(), System.currentTimeMillis());
    }

    public void reset(UUID uuid) {
        PlayerViolationProfile profile = profiles.remove(uuid);
        if (profile != null) {
            profile.reset();
        }
    }

    public void remove(UUID uuid) {
        profiles.remove(uuid);
    }

    public int trackedPlayers() {
        return profiles.size();
    }

    private void punish(Player player, CheckType type, double violationLevel) {
        for (String command : config.getPunishmentCommands()) {
            if (command == null || command.isBlank()) {
                continue;
            }
            String parsed = command
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%check%", type.getDisplayName())
                .replace("%vl%", String.format(java.util.Locale.ROOT, "%.2f", violationLevel));
            scheduler.runGlobal(() -> {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
                } catch (RuntimeException exception) {
                    plugin.getLogger().log(Level.WARNING, "Anti-cheat punishment command failed: " + parsed, exception);
                }
            });
        }
    }
}
