package com.jamie.jamiebingo.util;

import net.minecraft.server.level.ServerLevel;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class LevelBoundsUtil {
    private LevelBoundsUtil() {}

    public static int getMinY(ServerLevel level) {
        Integer val = findInt(level, "min", "build");
        if (val != null) return val;
        // Reasonable fallback for modern versions
        return -64;
    }

    public static int getMaxY(ServerLevel level) {
        Integer val = findInt(level, "max", "build");
        if (val != null) return val;
        // Reasonable fallback for modern versions
        return 320;
    }

    private static Integer findInt(ServerLevel level, String mustContain1, String mustContain2) {
        if (level == null) return null;
        for (Method m : level.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != int.class && m.getReturnType() != Integer.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains(mustContain1)) continue;
            if (!name.contains(mustContain2)) continue;
            try {
                Object out = m.invoke(level);
                if (out instanceof Integer i) return i;
                if (out != null) return (int) out;
            } catch (Throwable ignored) {
            }
        }
        // Try any int method that looks like min/max without "build"
        for (Method m : level.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != int.class && m.getReturnType() != Integer.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains(mustContain1)) continue;
            try {
                Object out = m.invoke(level);
                if (out instanceof Integer i) return i;
                if (out != null) return (int) out;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
