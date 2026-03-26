package com.jamie.jamiebingo.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class LevelServerUtil {

    private LevelServerUtil() {
    }

    public static MinecraftServer getServer(Level level) {
        if (level == null) return null;
        try {
            if (level instanceof net.minecraft.server.level.ServerLevel sl) {
                return sl.getServer();
            }
        } catch (Throwable ignored) {
        }
        try {
            return level.getServer();
        } catch (Throwable ignored) {
        }
        try {
            Method m = level.getClass().getMethod("getServer");
            Object v = m.invoke(level);
            if (v instanceof MinecraftServer server) return server;
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : level.getClass().getDeclaredFields()) {
            if (!MinecraftServer.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(level);
                if (v instanceof MinecraftServer server) return server;
            } catch (IllegalAccessException ignored) {
            }
        }
        try {
            return net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        } catch (Throwable ignored) {
        }
        return null;
    }
}
