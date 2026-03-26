package com.jamie.jamiebingo.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;

public final class DamageSourceUtil {
    private DamageSourceUtil() {
    }

    public static Entity getEntity(DamageSource source) {
        return getEntityLike(source, "getEntity");
    }

    public static Entity getDirectEntity(DamageSource source) {
        return getEntityLike(source, "getDirect");
    }

    private static Entity getEntityLike(DamageSource source, String nameHint) {
        if (source == null) return null;
        try {
            Method m = source.getClass().getMethod("getEntity");
            Object out = m.invoke(source);
            if (out instanceof Entity e) return e;
        } catch (Throwable ignored) {
        }
        try {
            Method m = source.getClass().getMethod("getDirectEntity");
            Object out = m.invoke(source);
            if (out instanceof Entity e) return e;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : source.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!Entity.class.isAssignableFrom(m.getReturnType())) continue;
                String n = m.getName().toLowerCase();
                if (!n.contains("entity")) continue;
                if (!n.contains(nameHint.toLowerCase()) && nameHint != null) {
                    // allow any entity accessor, prefer name hint if present
                    if (nameHint.equals("getDirect")) {
                        if (!n.contains("direct")) continue;
                    }
                }
                Object out = m.invoke(source);
                if (out instanceof Entity e) return e;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
