/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.listener;

import com.asuuna.anticheat.service.StaffAlertService;
import com.asuuna.anticheat.service.ViolationService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class ConnectionListener implements Listener {

    private final ViolationService violations;
    private final StaffAlertService staffAlerts;

    public ConnectionListener(ViolationService violations, StaffAlertService staffAlerts) {
        this.violations = violations;
        this.staffAlerts = staffAlerts;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        violations.remove(event.getPlayer().getUniqueId());
        staffAlerts.clear(event.getPlayer());
    }
}
