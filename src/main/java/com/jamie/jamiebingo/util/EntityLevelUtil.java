package com.jamie.jamiebingo.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class EntityLevelUtil {

    private EntityLevelUtil() {
    }

    public static Level getLevel(Entity entity) {
        if (entity == null) return null;
        try {
            return entity.level();
        } catch (Throwable ignored) {
        }
        try {
            Method m = entity.getClass().getMethod("getLevel");
            Object v = m.invoke(entity);
            if (v instanceof Level level) return level;
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method m = entity.getClass().getMethod("getCommandSenderWorld");
            Object v = m.invoke(entity);
            if (v instanceof Level level) return level;
        } catch (ReflectiveOperationException ignored) {
        }
        for (Field f : entity.getClass().getDeclaredFields()) {
            if (!Level.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(entity);
                if (v instanceof Level level) return level;
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }
}
