package com.jamie.jamiebingo.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class LevelSideUtil {
    private LevelSideUtil() {}

    public static boolean isClientSide(Object level) {
        if (level == null) return false;

        // If we can detect ServerLevel, it's not client side.
        try {
            Class<?> serverLevel = Class.forName("net.minecraft.server.level.ServerLevel");
            if (serverLevel.isInstance(level)) return false;
        } catch (Throwable ignored) {
        }

        String name = level.getClass().getName().toLowerCase();
        if (name.contains("client")) return true;

        // Try method lookups
        Boolean value = invokeBooleanMethod(level, "isClientSide");
        if (value != null) return value;
        value = invokeBooleanMethodContaining(level, "client");
        if (value != null) return value;

        // Try boolean field lookups
        Boolean field = readBooleanFieldContaining(level, "client");
        if (field != null) return field;

        return false;
    }

    private static Boolean invokeBooleanMethod(Object target, String name) {
        try {
            Method m = target.getClass().getMethod(name);
            if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class) {
                Object out = m.invoke(target);
                return (Boolean) out;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = target.getClass().getDeclaredMethod(name);
            if (m.getReturnType() == boolean.class || m.getReturnType() == Boolean.class) {
                m.setAccessible(true);
                Object out = m.invoke(target);
                return (Boolean) out;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Boolean invokeBooleanMethodContaining(Object target, String token) {
        for (Method m : target.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            String n = m.getName().toLowerCase();
            if (!n.contains(token)) continue;
            if (m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class) continue;
            try {
                Object out = m.invoke(target);
                return (Boolean) out;
            } catch (Throwable ignored) {
            }
        }
        for (Method m : target.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            String n = m.getName().toLowerCase();
            if (!n.contains(token)) continue;
            if (m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class) continue;
            try {
                m.setAccessible(true);
                Object out = m.invoke(target);
                return (Boolean) out;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static Boolean readBooleanFieldContaining(Object target, String token) {
        for (Field f : target.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            String n = f.getName().toLowerCase();
            if (!n.contains(token)) continue;
            if (f.getType() != boolean.class && f.getType() != Boolean.class) continue;
            try {
                f.setAccessible(true);
                Object out = f.get(target);
                return (Boolean) out;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
