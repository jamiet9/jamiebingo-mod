package com.jamie.jamiebingo.util;


import com.jamie.jamiebingo.util.ServerLevelUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Iterator;

public final class ServerLevelUtil {

    private ServerLevelUtil() {}

    public static ServerLevel getOverworld(MinecraftServer server) {
        if (server == null) return null;

        // Preferred: no-arg overworld() / getOverworld()
        try {
            for (Method m : server.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!ServerLevel.class.isAssignableFrom(m.getReturnType())) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("overworld")) continue;
                return (ServerLevel) m.invoke(server);
            }
            for (Method m : server.getClass().getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!ServerLevel.class.isAssignableFrom(m.getReturnType())) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("overworld")) continue;
                m.setAccessible(true);
                return (ServerLevel) m.invoke(server);
            }
        } catch (Exception ignored) {
        }

        // Next: any getLevel(ResourceKey<Level>) if we can resolve a key
        try {
            Object key = resolveOverworldKey();
            if (key != null) {
                Class<?> keyClass = key.getClass();
                for (Method m : server.getClass().getMethods()) {
                    if (Modifier.isStatic(m.getModifiers())) continue;
                    if (m.getParameterCount() != 1) continue;
                    if (!ServerLevel.class.isAssignableFrom(m.getReturnType())) continue;
                    if (!m.getParameterTypes()[0].isAssignableFrom(keyClass)) continue;
                    return (ServerLevel) m.invoke(server, key);
                }
                for (Method m : server.getClass().getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers())) continue;
                    if (m.getParameterCount() != 1) continue;
                    if (!ServerLevel.class.isAssignableFrom(m.getReturnType())) continue;
                    if (!m.getParameterTypes()[0].isAssignableFrom(keyClass)) continue;
                    m.setAccessible(true);
                    return (ServerLevel) m.invoke(server, key);
                }
            }
        } catch (Exception ignored) {
        }

        // Final fallback: any available level from server collections/fields or players
        ServerLevel any = getAnyLevel(server);
        if (any != null) return any;

        return null;
    }

    private static Object resolveOverworldKey() {
        try {
            Class<?> levelClass = Class.forName("net.minecraft.world.level.Level");
            for (Field f : levelClass.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    if (!f.getType().getName().contains("ResourceKey")) continue;
                    String name = f.getName().toLowerCase();
                    if (!name.contains("overworld")) continue;
                    f.setAccessible(true);
                    return f.get(null);
                }
            }
            for (Field f : levelClass.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    if (!f.getType().getName().contains("ResourceKey")) continue;
                    f.setAccessible(true);
                    Object key = f.get(null);
                    if (key != null) return key;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static ServerLevel getAnyLevel(MinecraftServer server) {
        if (server == null) return null;
        try {
            for (Method m : server.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                Object result = m.invoke(server);
                ServerLevel level = firstLevelFrom(result);
                if (level != null) return level;
            }
            for (Method m : server.getClass().getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                m.setAccessible(true);
                Object result = m.invoke(server);
                ServerLevel level = firstLevelFrom(result);
                if (level != null) return level;
            }
            for (Field f : server.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object result = f.get(server);
                ServerLevel level = firstLevelFrom(result);
                if (level != null) return level;
            }
        } catch (Exception ignored) {
        }

        // Fallback: first player's level
        try {
            java.util.List<net.minecraft.server.level.ServerPlayer> players =
                    com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server);
            if (!players.isEmpty()) {
                ServerLevel level = com.jamie.jamiebingo.util.ServerPlayerUtil.getLevel(players.get(0));
                if (level != null) return level;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private static ServerLevel firstLevelFrom(Object result) {
        if (result == null) return null;
        if (result instanceof ServerLevel level) return level;
        if (result instanceof Iterable<?> iterable) {
            Iterator<?> it = iterable.iterator();
            while (it.hasNext()) {
                Object value = it.next();
                if (value instanceof ServerLevel l) return l;
            }
            return null;
        }
        if (result instanceof java.util.Map<?, ?> map) {
            for (Object value : map.values()) {
                if (value instanceof ServerLevel l) return l;
            }
            return null;
        }
        if (result.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(result);
            for (int i = 0; i < len; i++) {
                Object value = java.lang.reflect.Array.get(result, i);
                if (value instanceof ServerLevel l) return l;
            }
            return null;
        }
        if (result instanceof java.util.stream.Stream<?> stream) {
            try {
                Object value = stream.findFirst().orElse(null);
                if (value instanceof ServerLevel l) return l;
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}

