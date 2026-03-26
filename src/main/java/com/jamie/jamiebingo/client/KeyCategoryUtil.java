package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.util.IdUtil;
import net.minecraft.client.KeyMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class KeyCategoryUtil {
    private KeyCategoryUtil() {}

    public static KeyMapping.Category getOrCreate(String id) {
        KeyMapping.Category category = tryRegister(id);
        if (category != null) return category;
        category = tryRegister("key.categories.jamiebingo");
        if (category != null) return category;
        return fallbackCategory();
    }

    private static KeyMapping.Category tryRegister(String id) {
        Class<?> clazz = KeyMapping.Category.class;
        Object ident = null;
        try {
            ident = IdUtil.id(id);
        } catch (Throwable ignored) {
        }
        for (Method m : clazz.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (m.getReturnType() != KeyMapping.Category.class) continue;
            if (m.getParameterCount() != 1) continue;
            Class<?> p = m.getParameterTypes()[0];
            Object arg = null;
            if (ident != null && p.isInstance(ident)) {
                arg = ident;
            } else if (p == String.class) {
                arg = id;
            } else {
                continue;
            }
            try {
                m.setAccessible(true);
                Object out = m.invoke(null, arg);
                if (out instanceof KeyMapping.Category c) {
                    return c;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static KeyMapping.Category fallbackCategory() {
        for (Field f : KeyMapping.Category.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) continue;
            if (f.getType() != KeyMapping.Category.class) continue;
            try {
                f.setAccessible(true);
                Object out = f.get(null);
                if (out instanceof KeyMapping.Category c) {
                    return c;
                }
            } catch (Throwable ignored) {
            }
        }
        throw new IllegalStateException("Unable to resolve KeyMapping.Category");
    }
}
