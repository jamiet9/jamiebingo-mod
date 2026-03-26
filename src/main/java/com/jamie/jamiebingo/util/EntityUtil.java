package com.jamie.jamiebingo.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.UUID;

public final class EntityUtil {
    private EntityUtil() {}

    public static UUID getUUID(Object entity) {
        if (entity == null) return null;
        try {
            Method m = entity.getClass().getMethod("getUUID");
            Object out = m.invoke(entity);
            if (out instanceof UUID id) return id;
        } catch (Throwable ignored) {
        }
        try {
            Method m = entity.getClass().getMethod("getGameProfile");
            Object profile = m.invoke(entity);
            if (profile != null) {
                Method idMethod = profile.getClass().getMethod("getId");
                Object out = idMethod.invoke(profile);
                if (out instanceof UUID id) return id;
            }
        } catch (Throwable ignored) {
        }
        for (Method m : entity.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!UUID.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(entity);
                if (out instanceof UUID id) return id;
            } catch (Throwable ignored) {
            }
        }
        for (Field f : entity.getClass().getDeclaredFields()) {
            if (!UUID.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object out = f.get(entity);
                if (out instanceof UUID id) return id;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }
}
