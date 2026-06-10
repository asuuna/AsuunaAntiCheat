/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.message;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public final class MessageService {

    private final Plugin plugin;
    private volatile FileConfiguration messages;
    private volatile String prefix;

    public MessageService(Plugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
        prefix = color(messages.getString("prefix", "&8[&cAsuunaAC&8]&r "));
    }

    public void send(CommandSender sender, String key) {
        send(sender, key, Collections.emptyMap());
    }

    public void send(CommandSender sender, String key, Map<String, String> placeholders) {
        sender.sendMessage(formatKey(key, placeholders));
    }

    public void sendList(CommandSender sender, String key) {
        List<String> lines = messages.getStringList("messages." + key);
        if (lines.isEmpty()) {
            send(sender, key);
            return;
        }
        for (String line : lines) {
            sender.sendMessage(format(line, Map.of()));
        }
    }

    public String format(String text, Map<String, String> placeholders) {
        String result = text == null ? "" : text;
        result = result.replace("%prefix%", prefix);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return color(result);
    }

    public String formatKey(String key, Map<String, String> placeholders) {
        return format(messages.getString("messages." + key, key), placeholders);
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
