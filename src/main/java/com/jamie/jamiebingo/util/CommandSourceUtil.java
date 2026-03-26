package com.jamie.jamiebingo.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class CommandSourceUtil {
    private CommandSourceUtil() {}

    public static Entity getEntity(CommandSourceStack src) {
        if (src == null) return null;
        try {
            Method m = src.getClass().getMethod("getEntity");
            Object out = m.invoke(src);
            if (out instanceof Entity e) return e;
        } catch (Throwable ignored) {
        }
        try {
            Method m = src.getClass().getMethod("getEntityOrException");
            Object out = m.invoke(src);
            if (out instanceof Entity e) return e;
        } catch (Throwable ignored) {
        }
        for (Method m : src.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!Entity.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(src);
                if (out instanceof Entity e) return e;
            } catch (Throwable ignored) {
            }
        }
        for (Field f : src.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (!Entity.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object out = f.get(src);
                if (out instanceof Entity e) return e;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static boolean hasPermission(CommandSourceStack src, int level) {
        if (src == null) return false;
        try {
            Method m = src.getClass().getMethod("hasPermission", int.class);
            Object out = m.invoke(src, level);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        try {
            Method m = src.getClass().getMethod("permissions");
            Object perms = m.invoke(src);
            if (perms != null) {
                // If this is a LevelBasedPermissionSet, compare its level id.
                try {
                    Method levelMethod = perms.getClass().getMethod("level");
                    Object lvl = levelMethod.invoke(perms);
                    if (lvl != null) {
                        Method idMethod = lvl.getClass().getMethod("id");
                        Object id = idMethod.invoke(lvl);
                        if (id instanceof Integer i) {
                            return i >= level;
                        }
                    }
                } catch (Throwable ignored2) {
                }
                // If ALL_PERMISSIONS or unknown, be permissive so commands remain available.
                try {
                    Field allField = perms.getClass().getField("ALL_PERMISSIONS");
                    Object all = allField.get(null);
                    if (perms.equals(all)) return true;
                } catch (Throwable ignored2) {
                }
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static void sendSuccess(CommandSourceStack src, java.util.function.Supplier<net.minecraft.network.chat.Component> message, boolean broadcastToOps) {
        if (src == null || message == null) return;
        try {
            Method m = src.getClass().getMethod("sendSuccess", java.util.function.Supplier.class, boolean.class);
            m.invoke(src, message, broadcastToOps);
            return;
        } catch (Throwable ignored) {
        }
        // Fallback to system message
        try {
            Method m = src.getClass().getMethod("sendSystemMessage", net.minecraft.network.chat.Component.class);
            m.invoke(src, message.get());
        } catch (Throwable ignored) {
        }
    }

    public static void sendFailure(CommandSourceStack src, net.minecraft.network.chat.Component message) {
        if (src == null || message == null) return;
        try {
            Method m = src.getClass().getMethod("sendFailure", net.minecraft.network.chat.Component.class);
            m.invoke(src, message);
            return;
        } catch (Throwable ignored) {
        }
        try {
            Method m = src.getClass().getMethod("sendSystemMessage", net.minecraft.network.chat.Component.class);
            m.invoke(src, message);
        } catch (Throwable ignored) {
        }
    }

    public static net.minecraft.server.MinecraftServer getServer(CommandSourceStack src) {
        if (src == null) return null;
        try {
            Method m = src.getClass().getMethod("getServer");
            Object out = m.invoke(src);
            if (out instanceof net.minecraft.server.MinecraftServer s) return s;
        } catch (Throwable ignored) {
        }
        for (Method m : src.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!net.minecraft.server.MinecraftServer.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(src);
                if (out instanceof net.minecraft.server.MinecraftServer s) return s;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
