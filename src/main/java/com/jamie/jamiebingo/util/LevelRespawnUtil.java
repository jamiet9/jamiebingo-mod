package com.jamie.jamiebingo.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelData;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class LevelRespawnUtil {

    private LevelRespawnUtil() {
    }

    public static boolean setRespawnData(ServerLevel level, ResourceKey<Level> dimension, BlockPos pos, float yaw, float pitch) {
        if (level == null || dimension == null || pos == null) return false;
        Object respawnData = createRespawnData(dimension, pos, yaw, pitch);
        if (respawnData == null) return false;

        // Try direct method setRespawnData
        try {
            Method m = level.getClass().getMethod("setRespawnData", respawnData.getClass());
            m.invoke(level, respawnData);
            return true;
        } catch (ReflectiveOperationException ignored) {
        }

        // Try any method that accepts LevelData.RespawnData
        for (Method m : level.getClass().getMethods()) {
            Class<?>[] p = m.getParameterTypes();
            if (p.length != 1) continue;
            if (!p[0].getName().equals(LevelData.RespawnData.class.getName())) continue;
            try {
                m.invoke(level, respawnData);
                return true;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return false;
    }

    private static Object createRespawnData(ResourceKey<Level> dimension, BlockPos pos, float yaw, float pitch) {
        // Preferred: RespawnData.of(...)
        try {
            Method of = LevelData.RespawnData.class.getMethod("of", ResourceKey.class, BlockPos.class, float.class, float.class);
            return of.invoke(null, dimension, pos, yaw, pitch);
        } catch (ReflectiveOperationException ignored) {
        }
        // Fallback: any static method returning RespawnData with 4 params
        for (Method m : LevelData.RespawnData.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            if (m.getReturnType() != LevelData.RespawnData.class) continue;
            Class<?>[] p = m.getParameterTypes();
            if (p.length == 4 && p[0] == ResourceKey.class && p[1] == BlockPos.class && p[2] == float.class && p[3] == float.class) {
                try {
                    m.setAccessible(true);
                    return m.invoke(null, dimension, pos, yaw, pitch);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        // Constructor fallback
        for (Constructor<?> c : LevelData.RespawnData.class.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 4 && p[0] == ResourceKey.class && p[1] == BlockPos.class && p[2] == float.class && p[3] == float.class) {
                try {
                    c.setAccessible(true);
                    return c.newInstance(dimension, pos, yaw, pitch);
                } catch (ReflectiveOperationException ignored) {
                }
            }
        }
        return null;
    }
}
