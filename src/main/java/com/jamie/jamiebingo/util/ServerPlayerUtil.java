package com.jamie.jamiebingo.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ServerPlayerUtil {
    private ServerPlayerUtil() {}

    public static ServerLevel getLevel(ServerPlayer player) {
        if (player == null) return null;
        try {
            Method m = player.getClass().getMethod("getLevel");
            Object out = m.invoke(player);
            if (out instanceof ServerLevel level) return level;
        } catch (Throwable ignored) {
        }
        try {
            Method m = player.getClass().getMethod("level");
            Object out = m.invoke(player);
            if (out instanceof ServerLevel level) return level;
        } catch (Throwable ignored) {
        }
        for (Method m : player.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!ServerLevel.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(player);
                if (out instanceof ServerLevel level) return level;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static MinecraftServer getServer(ServerPlayer player) {
        if (player == null) return null;
        try {
            Method m = player.getClass().getMethod("getServer");
            Object out = m.invoke(player);
            if (out instanceof MinecraftServer server) return server;
        } catch (Throwable ignored) {
        }
        ServerLevel level = getLevel(player);
        if (level != null) {
            try {
                Method m = level.getClass().getMethod("getServer");
                Object out = m.invoke(level);
                if (out instanceof MinecraftServer server) return server;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
