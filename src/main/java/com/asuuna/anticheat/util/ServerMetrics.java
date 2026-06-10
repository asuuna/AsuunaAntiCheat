/*
 * AsuunaAntiCheat
 * Copyright (c) 2026 asuuna. All rights reserved.
 */
package com.asuuna.anticheat.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class ServerMetrics {

    private static volatile Method getPingMethod;
    private static volatile Method getTpsMethod;

    private ServerMetrics() {
    }

    public static String context(Player player) {
        return "ping=" + ping(player) + "ms tps=" + tps();
    }

    public static int ping(Player player) {
        try {
            Method method = getPingMethod;
            if (method == null) {
                method = player.getClass().getMethod("getPing");
                getPingMethod = method;
            }
            Object value = method.invoke(player);
            return value instanceof Number ? Math.max(0, ((Number) value).intValue()) : -1;
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException ignored) {
            return -1;
        }
    }

    public static String tps() {
        try {
            Method method = getTpsMethod;
            if (method == null) {
                method = Bukkit.getServer().getClass().getMethod("getTPS");
                getTpsMethod = method;
            }
            Object value = method.invoke(Bukkit.getServer());
            if (!(value instanceof double[]) || ((double[]) value).length == 0) {
                return "n/a";
            }
            return String.format(Locale.ROOT, "%.2f", ((double[]) value)[0]);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | RuntimeException ignored) {
            return "n/a";
        }
    }
}
