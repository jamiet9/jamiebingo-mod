package com.jamie.jamiebingo.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelData;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class LevelSpawnUtil {
    private LevelSpawnUtil() {}

    public static LevelData.RespawnData getRespawnData(ServerLevel level) {
        if (level == null) return null;

        // Try direct method on ServerLevel
        for (Method m : level.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!LevelData.RespawnData.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(level);
                if (out instanceof LevelData.RespawnData data) return data;
            } catch (Throwable ignored) {
            }
        }

        // Try via LevelData
        LevelData levelData = getLevelData(level);
        if (levelData != null) {
            for (Method m : levelData.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!LevelData.RespawnData.class.isAssignableFrom(m.getReturnType())) continue;
                try {
                    Object out = m.invoke(levelData);
                    if (out instanceof LevelData.RespawnData data) return data;
                } catch (Throwable ignored) {
                }
            }
        }

        return null;
    }

    public static BlockPos getSpawnPos(ServerLevel level) {
        LevelData.RespawnData respawn = getRespawnData(level);
        if (respawn != null) {
            BlockPos pos = getRespawnPos(respawn);
            if (pos != null) return pos;
        }

        // Fallback: any no-arg method on ServerLevel returning BlockPos
        if (level != null) {
            for (Method m : level.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!BlockPos.class.isAssignableFrom(m.getReturnType())) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("spawn") && !name.contains("shared")) continue;
                try {
                    Object out = m.invoke(level);
                    if (out instanceof BlockPos pos) return pos;
                } catch (Throwable ignored) {
                }
            }
        }

        return BlockPos.ZERO;
    }

    public static float getSpawnYaw(ServerLevel level) {
        LevelData.RespawnData respawn = getRespawnData(level);
        if (respawn != null) {
            Float yaw = getRespawnYaw(respawn);
            if (yaw != null) return yaw;
        }

        // Fallback: any no-arg method on ServerLevel returning float
        if (level != null) {
            for (Method m : level.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (m.getReturnType() != float.class && m.getReturnType() != Float.class) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("spawn") && !name.contains("angle") && !name.contains("yaw")) continue;
                try {
                    Object out = m.invoke(level);
                    if (out instanceof Float f) return f;
                    if (out != null) return (float) out;
                } catch (Throwable ignored) {
                }
            }
        }

        return 0.0f;
    }

    private static LevelData getLevelData(ServerLevel level) {
        if (level == null) return null;
        for (Method m : level.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!LevelData.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(level);
                if (out instanceof LevelData data) return data;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static BlockPos getRespawnPos(LevelData.RespawnData respawn) {
        try {
            Method m = respawn.getClass().getMethod("pos");
            Object out = m.invoke(respawn);
            if (out instanceof BlockPos pos) return pos;
        } catch (Throwable ignored) {
        }
        for (Method m : respawn.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!BlockPos.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(respawn);
                if (out instanceof BlockPos pos) return pos;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Float getRespawnYaw(LevelData.RespawnData respawn) {
        try {
            Method m = respawn.getClass().getMethod("yaw");
            Object out = m.invoke(respawn);
            if (out instanceof Float f) return f;
            if (out != null) return (float) out;
        } catch (Throwable ignored) {
        }
        for (Method m : respawn.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != float.class && m.getReturnType() != Float.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains("yaw") && !name.contains("angle")) continue;
            try {
                Object out = m.invoke(respawn);
                if (out instanceof Float f) return f;
                if (out != null) return (float) out;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
