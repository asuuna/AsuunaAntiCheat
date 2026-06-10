/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.config;

import com.asuuna.anticheat.check.CheckType;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

public final class AntiCheatConfig {

    private final Plugin plugin;
    private volatile Map<CheckType, CheckSettings> checks = Map.of();
    private volatile String alertPermission = "asuunaac.alerts";
    private volatile String bypassPermission = "asuunaac.bypass";
    private volatile double violationDecayPerMinute = 4.0D;
    private volatile long alertCooldownMillis = 1200L;
    private volatile boolean logAlerts = true;
    private volatile Set<String> disabledWorlds = Set.of();
    private volatile boolean punishmentsEnabled;
    private volatile double punishmentMaxViolations = 25.0D;
    private volatile List<String> punishmentCommands = List.of();
    private volatile boolean debug;

    public AntiCheatConfig(Plugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        debug = plugin.getConfig().getBoolean("settings.debug", false);
        alertPermission = plugin.getConfig().getString("settings.alert-permission", "asuunaac.alerts");
        bypassPermission = plugin.getConfig().getString("settings.bypass-permission", "asuunaac.bypass");
        violationDecayPerMinute = Math.max(0.0D, plugin.getConfig().getDouble("settings.violation-decay-per-minute", 4.0D));
        alertCooldownMillis = Math.max(0L, plugin.getConfig().getLong("settings.alert-cooldown-millis", 1200L));
        logAlerts = plugin.getConfig().getBoolean("settings.log-alerts", true);
        disabledWorlds = readWorldList(plugin.getConfig().getStringList("settings.disabled-worlds"));
        punishmentsEnabled = plugin.getConfig().getBoolean("punishments.enabled", false);
        punishmentMaxViolations = Math.max(1.0D, plugin.getConfig().getDouble("punishments.max-violations", 25.0D));
        punishmentCommands = List.copyOf(plugin.getConfig().getStringList("punishments.commands"));
        checks = loadChecks();
    }

    public CheckSettings settings(CheckType type) {
        return checks.getOrDefault(type, new CheckSettings(false, 0.0D, 999.0D, Map.of()));
    }

    public boolean isBypassPermission(String permission) {
        return bypassPermission.equals(permission);
    }

    public String getAlertPermission() {
        return alertPermission;
    }

    public String getBypassPermission() {
        return bypassPermission;
    }

    public double getViolationDecayPerMinute() {
        return violationDecayPerMinute;
    }

    public long getAlertCooldownMillis() {
        return alertCooldownMillis;
    }

    public boolean isLogAlerts() {
        return logAlerts;
    }

    public boolean isWorldDisabled(String worldName) {
        return worldName != null && disabledWorlds.contains(worldName.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean isPunishmentsEnabled() {
        return punishmentsEnabled;
    }

    public double getPunishmentMaxViolations() {
        return punishmentMaxViolations;
    }

    public List<String> getPunishmentCommands() {
        return punishmentCommands;
    }

    public boolean isDebug() {
        return debug;
    }

    public int countEnabledChecks() {
        int count = 0;
        for (CheckSettings settings : checks.values()) {
            if (settings.isEnabled()) {
                count++;
            }
        }
        return count;
    }

    private Map<CheckType, CheckSettings> loadChecks() {
        EnumMap<CheckType, CheckSettings> loaded = new EnumMap<>(CheckType.class);
        for (CheckType type : CheckType.values()) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("checks." + type.getConfigKey());
            if (section == null) {
                loaded.put(type, new CheckSettings(false, 0.0D, 999.0D, Map.of()));
                continue;
            }
            loaded.put(type, new CheckSettings(
                section.getBoolean("enabled", true),
                section.getDouble("alert-vl", 4.0D),
                section.getDouble("cancel-vl", 8.0D),
                readNumbers(section)
            ));
        }
        return Map.copyOf(loaded);
    }

    private Map<String, Double> readNumbers(ConfigurationSection section) {
        Map<String, Double> values = new HashMap<>();
        for (String key : section.getKeys(false)) {
            Object value = section.get(key);
            if (value instanceof Number) {
                values.put(key, ((Number) value).doubleValue());
            }
        }
        return values;
    }

    private Set<String> readWorldList(List<String> worlds) {
        Set<String> values = new HashSet<>();
        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                values.add(world.toLowerCase(java.util.Locale.ROOT));
            }
        }
        return Set.copyOf(values);
    }
}
