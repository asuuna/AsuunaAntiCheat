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
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class InventoryListener implements Listener {

    private final AntiCheatConfig config;
    private final ViolationService violations;
    private final Map<UUID, Long> openInventories = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<Long>> inventoryClicks = new ConcurrentHashMap<>();

    public InventoryListener(AntiCheatConfig config, ViolationService violations) {
        this.config = config;
        this.violations = violations;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player)) {
            return;
        }
        if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING) {
            return;
        }
        Player player = (Player) event.getPlayer();
        openInventories.put(player.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        openInventories.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        CheckSettings settings = config.settings(CheckType.INVENTORY_CLICK_SPEED);
        if (!settings.isEnabled() || player.hasPermission(config.getBypassPermission())) {
            return;
        }

        long now = System.currentTimeMillis();
        long sampleMillis = Math.max(250L, (long) settings.number("sample-millis", 1000.0D));
        Deque<Long> samples = inventoryClicks.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayDeque<>());
        int clicks;
        synchronized (samples) {
            samples.addLast(now);
            while (!samples.isEmpty() && now - samples.peekFirst() > sampleMillis) {
                samples.removeFirst();
            }
            clicks = samples.size();
        }

        int maxClicks = (int) settings.number("max-clicks", 18.0D);
        if (clicks > maxClicks) {
            boolean cancel = violations.flag(player, CheckType.INVENTORY_CLICK_SPEED, 0.8D,
                "clicks=" + clicks + " max=" + maxClicks + " window=" + sampleMillis + "ms");
            if (cancel) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null || PlayerEnvironment.shouldSkipMovement(player)) {
            return;
        }

        Long openedAt = openInventories.get(player.getUniqueId());
        if (openedAt == null) {
            return;
        }

        CheckSettings settings = config.settings(CheckType.INVENTORY_MOVE);
        if (!settings.isEnabled()) {
            return;
        }

        long age = System.currentTimeMillis() - openedAt;
        if (age < (long) settings.number("open-grace-millis", 250.0D)) {
            return;
        }

        double horizontal = Math.hypot(to.getX() - event.getFrom().getX(), to.getZ() - event.getFrom().getZ());
        if (horizontal > settings.number("min-horizontal-distance", 0.04D)) {
            boolean cancel = violations.flag(player, CheckType.INVENTORY_MOVE, 0.8D,
                "horizontal=" + round(horizontal) + " openAge=" + age + "ms");
            if (cancel) {
                event.setTo(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        openInventories.remove(uuid);
        inventoryClicks.remove(uuid);
    }

    private String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
