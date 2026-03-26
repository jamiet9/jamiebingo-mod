package com.jamie.jamiebingo.util;

import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class PlayerExperienceUtil {
    private PlayerExperienceUtil() {
    }

    public static int getExperienceLevel(Player player) {
        if (player == null) return 0;
        // Prefer the mapped field directly; this is the authoritative XP level.
        try {
            return Math.max(0, player.experienceLevel);
        } catch (Throwable ignored) {
        }
        try {
            Method m = player.getClass().getMethod("getExperienceLevel");
            Object out = m.invoke(player);
            if (out instanceof Integer i) return Math.max(0, i);
        } catch (Throwable ignored) {
        }
        try {
            Method m = player.getClass().getMethod("experienceLevel");
            Object out = m.invoke(player);
            if (out instanceof Integer i) return Math.max(0, i);
        } catch (Throwable ignored) {
        }
        Integer field = readIntFieldExact(player, "experienceLevel");
        if (field != null) return Math.max(0, field);
        field = readIntFieldExact(player, "xpLevel");
        if (field != null) return Math.max(0, field);
        field = readIntFieldContaining(player, "experiencelevel");
        if (field != null) return Math.max(0, field);
        field = readIntFieldContaining(player, "xplevel");
        return field != null ? Math.max(0, field) : 0;
    }

    public static void setExperienceLevel(Player player, int level) {
        if (player == null) return;
        try {
            Method m = player.getClass().getMethod("setExperienceLevel", int.class);
            m.invoke(player, level);
            return;
        } catch (Throwable ignored) {
        }
        if (writeIntFieldExact(player, "experienceLevel", level)) return;
        if (writeIntFieldExact(player, "xpLevel", level)) return;
        if (writeIntFieldContaining(player, "experiencelevel", level)) return;
        writeIntFieldContaining(player, "xplevel", level);
    }

    public static void resetExperience(Player player) {
        if (player == null) return;
        // Prefer direct fields first so server authoritative values are updated.
        boolean apiApplied = false;
        try {
            player.totalExperience = 0;
            player.experienceLevel = 0;
            player.experienceProgress = 0.0F;
            apiApplied = true;
        } catch (Throwable ignored) {
        }

        if (!apiApplied) {
            setExperienceLevel(player, 0);
        }

        // Try dedicated setters first.
        try {
            Method m = player.getClass().getMethod("setExperiencePoints", int.class);
            m.invoke(player, 0);
        } catch (Throwable ignored) {
        }
        try {
            Method m = player.getClass().getMethod("setExperienceProgress", float.class);
            m.invoke(player, 0.0F);
        } catch (Throwable ignored) {
        }

        // Field-level hard reset for compatibility.
        writeIntFieldExact(player, "totalExperience", 0);
        writeIntFieldExact(player, "experienceLevel", 0);
        writeIntFieldExact(player, "xpLevel", 0);
        writeFloatFieldExact(player, "experienceProgress", 0.0F);
        writeFloatFieldExact(player, "xpProgress", 0.0F);

        if (player instanceof ServerPlayer sp && sp.connection != null) {
            sp.connection.send(new ClientboundSetExperiencePacket(
                    sp.experienceProgress,
                    sp.totalExperience,
                    sp.experienceLevel
            ));
        }
    }

    private static Integer readIntFieldContaining(Player player, String token) {
        Class<?> type = player.getClass();
        while (type != null && type != Object.class) {
            for (Field f : type.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() != int.class) continue;
                if (!f.getName().toLowerCase().contains(token)) continue;
                try {
                    f.setAccessible(true);
                    return f.getInt(player);
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static Integer readIntFieldExact(Player player, String fieldName) {
        Class<?> type = player.getClass();
        while (type != null && type != Object.class) {
            for (Field f : type.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() != int.class) continue;
                if (!f.getName().equals(fieldName)) continue;
                try {
                    f.setAccessible(true);
                    return f.getInt(player);
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static boolean writeIntFieldContaining(Player player, String token, int value) {
        Class<?> type = player.getClass();
        while (type != null && type != Object.class) {
            for (Field f : type.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() != int.class) continue;
                if (!f.getName().toLowerCase().contains(token)) continue;
                try {
                    f.setAccessible(true);
                    f.setInt(player, value);
                    return true;
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean writeIntFieldExact(Player player, String fieldName, int value) {
        Class<?> type = player.getClass();
        while (type != null && type != Object.class) {
            for (Field f : type.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() != int.class) continue;
                if (!f.getName().equals(fieldName)) continue;
                try {
                    f.setAccessible(true);
                    f.setInt(player, value);
                    return true;
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static boolean writeFloatFieldExact(Player player, String fieldName, float value) {
        Class<?> type = player.getClass();
        while (type != null && type != Object.class) {
            for (Field f : type.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() != float.class) continue;
                if (!f.getName().equals(fieldName)) continue;
                try {
                    f.setAccessible(true);
                    f.setFloat(player, value);
                    return true;
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }
}
