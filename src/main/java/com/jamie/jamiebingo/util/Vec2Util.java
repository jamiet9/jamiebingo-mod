package com.jamie.jamiebingo.util;

import net.minecraft.world.phys.Vec2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class Vec2Util {
    private Vec2Util() {
    }

    public static float getX(Vec2 vec) {
        return getComponent(vec, "x");
    }

    public static float getY(Vec2 vec) {
        return getComponent(vec, "y");
    }

    private static float getComponent(Vec2 vec, String axis) {
        if (vec == null) return 0.0f;
        try {
            Method m = vec.getClass().getMethod(axis);
            Object out = m.invoke(vec);
            if (out instanceof Number n) return n.floatValue();
        } catch (Throwable ignored) {
        }
        try {
            Method m = vec.getClass().getMethod("get" + axis.toUpperCase());
            Object out = m.invoke(vec);
            if (out instanceof Number n) return n.floatValue();
        } catch (Throwable ignored) {
        }
        try {
            Field f = vec.getClass().getField(axis);
            Object out = f.get(vec);
            if (out instanceof Number n) return n.floatValue();
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : vec.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                Class<?> type = f.getType();
                if (!(type == float.class || type == double.class || Number.class.isAssignableFrom(type))) continue;
                String name = f.getName().toLowerCase();
                if (!name.contains(axis)) continue;
                f.setAccessible(true);
                Object out = f.get(vec);
                if (out instanceof Number n) return n.floatValue();
            }
        } catch (Throwable ignored) {
        }
        return 0.0f;
    }
}
