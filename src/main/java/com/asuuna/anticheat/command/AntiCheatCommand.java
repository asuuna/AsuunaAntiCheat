/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.command;

import com.asuuna.anticheat.AsuunaAntiCheatPlugin;
import com.asuuna.anticheat.check.CheckType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public final class AntiCheatCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_PERMISSION = "asuunaac.command";
    private static final String ADMIN_PERMISSION = "asuunaac.admin";

    private final AsuunaAntiCheatPlugin plugin;

    public AntiCheatCommand(AsuunaAntiCheatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(COMMAND_PERMISSION)) {
            plugin.getMessages().send(sender, "no-permission");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            plugin.getMessages().sendList(sender, "help");
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "status":
                sendStatus(sender);
                return true;
            case "alerts":
                toggleAlerts(sender);
                return true;
            case "profile":
                sendProfile(sender, args);
                return true;
            case "reset":
                resetPlayer(sender, args);
                return true;
            case "reload":
                reload(sender);
                return true;
            default:
                plugin.getMessages().send(sender, "unknown-command");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(COMMAND_PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> entries = new ArrayList<>(List.of("help", "status", "alerts", "profile"));
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                entries.add("reload");
                entries.add("reset");
            }
            return filter(entries, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("profile") || args[0].equalsIgnoreCase("reset"))) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return filter(players, args[1]);
        }

        return Collections.emptyList();
    }

    private void sendStatus(CommandSender sender) {
        plugin.getMessages().send(sender, "status", Map.of(
            "checks", Integer.toString(plugin.getAntiCheatConfig().countEnabledChecks()),
            "players", Integer.toString(plugin.getViolationService().trackedPlayers())
        ));
    }

    private void toggleAlerts(CommandSender sender) {
        if (!(sender instanceof Player)) {
            plugin.getMessages().send(sender, "player-only");
            return;
        }
        Player player = (Player) sender;
        if (!player.hasPermission(plugin.getAntiCheatConfig().getAlertPermission())) {
            plugin.getMessages().send(sender, "no-permission");
            return;
        }
        boolean enabled = plugin.getStaffAlertService().toggleAlerts(player);
        plugin.getMessages().send(player, enabled ? "alerts-on" : "alerts-off");
    }

    private void sendProfile(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.getMessages().send(sender, "unknown-command");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessages().send(sender, "player-not-found", Map.of("player", args[1]));
            return;
        }

        plugin.getMessages().send(sender, "profile-header", Map.of("player", target.getName()));
        Map<CheckType, Double> snapshot = plugin.getViolationService().snapshot(target.getUniqueId());
        if (snapshot.isEmpty()) {
            plugin.getMessages().send(sender, "profile-empty");
            return;
        }
        for (Map.Entry<CheckType, Double> entry : snapshot.entrySet()) {
            sender.sendMessage(plugin.getMessages().formatKey("profile-entry", Map.of(
                "check", entry.getKey().getDisplayName(),
                "vl", String.format(Locale.ROOT, "%.2f", entry.getValue())
            )));
        }
    }

    private void resetPlayer(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessages().send(sender, "no-permission");
            return;
        }
        if (args.length < 2) {
            plugin.getMessages().send(sender, "unknown-command");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.getMessages().send(sender, "player-not-found", Map.of("player", args[1]));
            return;
        }
        UUID uuid = target.getUniqueId();
        plugin.getViolationService().reset(uuid);
        plugin.getMessages().send(sender, "reset-player", Map.of("player", target.getName()));
    }

    private void reload(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            plugin.getMessages().send(sender, "no-permission");
            return;
        }
        plugin.reloadPluginState();
        plugin.getMessages().send(sender, "reloaded");
    }

    private List<String> filter(List<String> entries, String prefix) {
        String normalized = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String entry : entries) {
            if (entry.toLowerCase(Locale.ROOT).startsWith(normalized)) {
                result.add(entry);
            }
        }
        return result;
    }
}
