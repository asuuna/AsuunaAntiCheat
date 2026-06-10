/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.service;

import com.asuuna.anticheat.config.AntiCheatConfig;
import com.asuuna.anticheat.message.MessageService;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class StaffAlertService {

    private final Plugin plugin;
    private final AntiCheatConfig config;
    private final MessageService messages;
    private final Set<UUID> mutedStaff = ConcurrentHashMap.newKeySet();

    public StaffAlertService(Plugin plugin, AntiCheatConfig config, MessageService messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
    }

    public void alert(String player, String check, double violationLevel, String detail) {
        String formatted = messages.formatKey("alert", Map.of(
            "player", player,
            "check", check,
            "vl", String.format(java.util.Locale.ROOT, "%.2f", violationLevel),
            "detail", detail
        ));
        Bukkit.getConsoleSender().sendMessage(formatted);
        for (Player staff : Bukkit.getOnlinePlayers()) {
            if (!staff.hasPermission(config.getAlertPermission()) || mutedStaff.contains(staff.getUniqueId())) {
                continue;
            }
            staff.sendMessage(formatted);
        }
    }

    public boolean toggleAlerts(Player player) {
        if (mutedStaff.remove(player.getUniqueId())) {
            return true;
        }
        mutedStaff.add(player.getUniqueId());
        return false;
    }

    public void clear(Player player) {
        mutedStaff.remove(player.getUniqueId());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        messages.send(sender, key, placeholders);
    }
}
