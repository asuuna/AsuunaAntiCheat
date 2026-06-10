/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.service;

import com.asuuna.anticheat.platform.PlatformScheduler;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.logging.Level;
import org.bukkit.plugin.Plugin;

public final class AlertLogService {

    private final Plugin plugin;
    private final PlatformScheduler scheduler;
    private final File logFile;

    public AlertLogService(Plugin plugin, PlatformScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.logFile = new File(plugin.getDataFolder(), "logs/alerts.log");
    }

    public void log(String player, String check, double violationLevel, String detail) {
        scheduler.runAsync(() -> {
            File parent = logFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Unable to create alert log directory: " + parent.getAbsolutePath());
                return;
            }
            String line = Instant.now() + " | " + player + " | " + check
                + " | VL " + String.format(java.util.Locale.ROOT, "%.2f", violationLevel)
                + " | " + detail + System.lineSeparator();
            try {
                Files.writeString(
                    logFile.toPath(),
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            } catch (IOException exception) {
                plugin.getLogger().log(Level.WARNING, "Unable to write anti-cheat alert log", exception);
            }
        });
    }
}
