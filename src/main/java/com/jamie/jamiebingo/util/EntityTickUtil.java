package com.jamie.jamiebingo.util;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class EntityTickUtil {
    private EntityTickUtil() {
    }

    public static int getTickCount(Entity entity) {
        if (entity == null) return 0;
        try {
            Method m = entity.getClass().getMethod("getTickCount");
            Object out = m.invoke(entity);
            if (out instanceof Integer i) return i;
        } catch (Throwable ignored) {
        }
        try {
            Method m = entity.getClass().getMethod("tickCount");
            Object out = m.invoke(entity);
            if (out instanceof Integer i) return i;
        } catch (Throwable ignored) {
        }
        try {
            Field f = entity.getClass().getField("tickCount");
            if (f.getType() == int.class) return f.getInt(entity);
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : entity.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (f.getType() != int.class) continue;
                String n = f.getName().toLowerCase();
                if (!n.contains("tick")) continue;
                f.setAccessible(true);
                return f.getInt(entity);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
