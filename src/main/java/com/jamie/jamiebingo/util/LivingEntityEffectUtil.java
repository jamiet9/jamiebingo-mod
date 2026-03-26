package com.jamie.jamiebingo.util;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;

public final class LivingEntityEffectUtil {
    private LivingEntityEffectUtil() {
    }

    public static boolean hasActiveEffects(LivingEntity entity) {
        if (entity == null) return false;
        Collection<?> effects = getActiveEffects(entity);
        return effects != null && !effects.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public static Collection<?> getActiveEffects(LivingEntity entity) {
        if (entity == null) return null;
        try {
            Method m = entity.getClass().getMethod("getActiveEffects");
            Object out = m.invoke(entity);
            if (out instanceof Collection<?> c) return c;
        } catch (Throwable ignored) {
        }
        try {
            Method m = entity.getClass().getMethod("getActiveEffectsMap");
            Object out = m.invoke(entity);
            if (out instanceof Map<?, ?> map) return map.values();
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : entity.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                Class<?> rt = m.getReturnType();
                if (Collection.class.isAssignableFrom(rt)) {
                    String name = m.getName().toLowerCase();
                    if (!name.contains("effect")) continue;
                    Object out = m.invoke(entity);
                    if (out instanceof Collection<?> c) return c;
                } else if (Map.class.isAssignableFrom(rt)) {
                    String name = m.getName().toLowerCase();
                    if (!name.contains("effect")) continue;
                    Object out = m.invoke(entity);
                    if (out instanceof Map<?, ?> map) return map.values();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : entity.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                Class<?> type = f.getType();
                if (!(Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type))) continue;
                String name = f.getName().toLowerCase();
                if (!name.contains("effect")) continue;
                f.setAccessible(true);
                Object out = f.get(entity);
                if (out instanceof Collection<?> c) return c;
                if (out instanceof Map<?, ?> map) return map.values();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
