package com.jamie.jamiebingo.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class LevelDataStorageUtil {

    private LevelDataStorageUtil() {}

    public static DimensionDataStorage getDataStorage(ServerLevel level) {
        if (level == null) return null;
        try {
            for (Method m : level.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!DimensionDataStorage.class.isAssignableFrom(m.getReturnType())) continue;
                m.setAccessible(true);
                return (DimensionDataStorage) m.invoke(level);
            }
            for (Method m : level.getClass().getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!DimensionDataStorage.class.isAssignableFrom(m.getReturnType())) continue;
                m.setAccessible(true);
                return (DimensionDataStorage) m.invoke(level);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
