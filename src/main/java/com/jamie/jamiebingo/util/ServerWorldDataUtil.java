package com.jamie.jamiebingo.util;

import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;

public final class ServerWorldDataUtil {
    private ServerWorldDataUtil() {
    }

    public static Object getWorldData(MinecraftServer server) {
        if (server == null) return null;
        try {
            Method m = server.getClass().getMethod("getWorldData");
            return m.invoke(server);
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : server.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                Class<?> rt = m.getReturnType();
                if (rt == null) continue;
                String name = rt.getName().toLowerCase();
                if (!name.contains("worlddata") && !name.contains("leveldata")) continue;
                m.setAccessible(true);
                return m.invoke(server);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static String getDifficultyKey(MinecraftServer server) {
        Object worldData = getWorldData(server);
        if (worldData == null) return "normal";
        try {
            Method getDifficulty = worldData.getClass().getMethod("getDifficulty");
            Object difficulty = getDifficulty.invoke(worldData);
            if (difficulty != null) {
                try {
                    Method getKey = difficulty.getClass().getMethod("getKey");
                    Object key = getKey.invoke(difficulty);
                    if (key != null) return key.toString();
                } catch (Throwable ignored) {
                }
                try {
                    Method getName = difficulty.getClass().getMethod("getSerializedName");
                    Object key = getName.invoke(difficulty);
                    if (key != null) return key.toString();
                } catch (Throwable ignored) {
                }
                return difficulty.toString();
            }
        } catch (Throwable ignored) {
        }
        return "normal";
    }
}
