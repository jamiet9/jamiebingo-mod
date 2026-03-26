package com.jamie.jamiebingo.util;

import net.minecraft.core.BlockPos;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class BlockPosUtil {
    private BlockPosUtil() {}

    public static int getX(BlockPos pos) {
        Integer v = callInt(pos, "getx", "x");
        return v != null ? v : 0;
    }

    public static int getY(BlockPos pos) {
        Integer v = callInt(pos, "gety", "y");
        return v != null ? v : 0;
    }

    public static int getZ(BlockPos pos) {
        Integer v = callInt(pos, "getz", "z");
        return v != null ? v : 0;
    }

    private static Integer callInt(BlockPos pos, String primary, String secondary) {
        if (pos == null) return null;
        for (Method m : pos.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != int.class && m.getReturnType() != Integer.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains(primary) && !name.equals(secondary)) continue;
            try {
                Object out = m.invoke(pos);
                if (out instanceof Integer i) return i;
                if (out != null) return (int) out;
            } catch (Throwable ignored) {
            }
        }
        // fallback: any int method with single-letter name
        for (Method m : pos.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != int.class && m.getReturnType() != Integer.class) continue;
            String name = m.getName();
            if (name.length() != 1) continue;
            try {
                Object out = m.invoke(pos);
                if (out instanceof Integer i) return i;
                if (out != null) return (int) out;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
