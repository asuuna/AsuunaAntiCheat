/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat;

import com.asuuna.anticheat.command.AntiCheatCommand;
import com.asuuna.anticheat.config.AntiCheatConfig;
import com.asuuna.anticheat.listener.CombatListener;
import com.asuuna.anticheat.listener.ConnectionListener;
import com.asuuna.anticheat.listener.InventoryListener;
import com.asuuna.anticheat.listener.InteractionListener;
import com.asuuna.anticheat.listener.MovementListener;
import com.asuuna.anticheat.message.MessageService;
import com.asuuna.anticheat.platform.PlatformScheduler;
import com.asuuna.anticheat.service.AlertLogService;
import com.asuuna.anticheat.service.StaffAlertService;
import com.asuuna.anticheat.service.ViolationService;
import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AsuunaAntiCheatPlugin extends JavaPlugin {

    private AntiCheatConfig antiCheatConfig;
    private PlatformScheduler scheduler;
    private MessageService messages;
    private AlertLogService alertLogService;
    private StaffAlertService staffAlertService;
    private ViolationService violationService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveBundledResource("messages.yml");

        scheduler = new PlatformScheduler(this);
        antiCheatConfig = new AntiCheatConfig(this);
        messages = new MessageService(this);
        alertLogService = new AlertLogService(this, scheduler);
        staffAlertService = new StaffAlertService(this, antiCheatConfig, messages);
        violationService = new ViolationService(this, antiCheatConfig, scheduler, staffAlertService, alertLogService);

        reloadPluginState();
        registerListeners();
        registerCommands();

        getLogger().info("AsuunaAntiCheat enabled on " + scheduler.getPlatformName()
            + ". Active checks: " + antiCheatConfig.countEnabledChecks());
    }

    @Override
    public void onDisable() {
        if (scheduler != null) {
            scheduler.cancelPluginTasks();
        }
    }

    public void reloadPluginState() {
        reloadConfig();
        saveBundledResource("messages.yml");
        antiCheatConfig.reload();
        messages.reload();
    }

    public AntiCheatConfig getAntiCheatConfig() {
        return antiCheatConfig;
    }

    public MessageService getMessages() {
        return messages;
    }

    public StaffAlertService getStaffAlertService() {
        return staffAlertService;
    }

    public ViolationService getViolationService() {
        return violationService;
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MovementListener(antiCheatConfig, violationService), this);
        getServer().getPluginManager().registerEvents(new CombatListener(antiCheatConfig, violationService), this);
        getServer().getPluginManager().registerEvents(new InteractionListener(antiCheatConfig, violationService), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(antiCheatConfig, violationService), this);
        getServer().getPluginManager().registerEvents(new ConnectionListener(violationService, staffAlertService), this);
    }

    private void registerCommands() {
        AntiCheatCommand commandHandler = new AntiCheatCommand(this);
        PluginCommand command = Objects.requireNonNull(getCommand("sac"), "Command sac is missing in plugin.yml");
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);
    }

    private void saveBundledResource(String name) {
        File file = new File(getDataFolder(), name);
        if (!file.exists()) {
            try {
                saveResource(name, false);
            } catch (IllegalArgumentException exception) {
                getLogger().log(Level.WARNING, "Bundled resource missing: " + name, exception);
            }
        }
    }
}
