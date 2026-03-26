package com.jamie.jamiebingo.util;

import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ServerTickUtil {

    private ServerTickUtil() {}

    private static volatile int fallbackTickCount = 0;
    private static volatile MinecraftServer lastServerInstance = null;

    private static void syncServerInstance(MinecraftServer server) {
        if (server == null) return;
        MinecraftServer previous = lastServerInstance;
        if (previous != server) {
            lastServerInstance = server;
            fallbackTickCount = 0;
        }
    }

    /**
     * Called once per server tick (Post) to keep a reliable tick counter.
     */
    public static void tick(MinecraftServer server) {
        syncServerInstance(server);
        fallbackTickCount++;
        int real = tryGetTickCount(server);
        if (real > 0) {
            fallbackTickCount = real;
        }
    }

    public static int getTickCount(MinecraftServer server) {
        syncServerInstance(server);
        if (server == null) return fallbackTickCount;
        int real = tryGetTickCount(server);
        if (real > 0) {
            fallbackTickCount = real;
            return real;
        }
        return fallbackTickCount;
    }

    private static int tryGetTickCount(MinecraftServer server) {
        if (server == null) return 0;
        try {
            Method getTickCount = findMethod(server.getClass(), "getTickCount");
            if (getTickCount != null) {
                return (int) getTickCount.invoke(server);
            }

            Field tickCount = findField(server.getClass(), "tickCount");
            if (tickCount != null) {
                return tickCount.getInt(server);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private static Method findMethod(Class<?> type, String name) {
        try {
            Method m = type.getMethod(name);
            m.setAccessible(true);
            return m;
        } catch (Exception ignored) {
        }
        try {
            Method m = type.getDeclaredMethod(name);
            m.setAccessible(true);
            return m;
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        try {
            Field f = type.getField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception ignored) {
        }
        try {
            Field f = type.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception ignored) {
        }
        return null;
    }
}
