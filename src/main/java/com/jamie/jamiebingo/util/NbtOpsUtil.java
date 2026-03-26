package com.jamie.jamiebingo.util;

import net.minecraft.nbt.NbtOps;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class NbtOpsUtil {
    private static volatile NbtOps INSTANCE;

    private NbtOpsUtil() {
    }

    public static NbtOps instance() {
        NbtOps cached = INSTANCE;
        if (cached != null) return cached;
        synchronized (NbtOpsUtil.class) {
            if (INSTANCE != null) return INSTANCE;
            try {
                for (Field f : NbtOps.class.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) continue;
                    if (!NbtOps.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    Object out = f.get(null);
                    if (out instanceof NbtOps ops) {
                        INSTANCE = ops;
                        return ops;
                    }
                }
            } catch (Throwable ignored) {
            }
            try {
                for (Method m : NbtOps.class.getDeclaredMethods()) {
                    if (!Modifier.isStatic(m.getModifiers())) continue;
                    if (m.getParameterCount() != 0) continue;
                    if (!NbtOps.class.isAssignableFrom(m.getReturnType())) continue;
                    m.setAccessible(true);
                    Object out = m.invoke(null);
                    if (out instanceof NbtOps ops) {
                        INSTANCE = ops;
                        return ops;
                    }
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }
}
