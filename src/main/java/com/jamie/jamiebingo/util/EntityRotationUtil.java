package com.jamie.jamiebingo.util;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;

public final class EntityRotationUtil {

    private EntityRotationUtil() {
    }

    public static float getYRot(Entity entity) {
        if (entity == null) return 0.0f;
        try {
            return com.jamie.jamiebingo.util.EntityRotationUtil.getYRot(entity);
        } catch (Throwable ignored) {
        }
        Float v = invokeFloatGetter(entity, "getYRot");
        if (v != null) return v;
        v = invokeFloatGetter(entity, "yRot");
        if (v != null) return v;
        v = invokeFloatGetter(entity, "getRotationYaw");
        if (v != null) return v;
        return 0.0f;
    }

    public static float getXRot(Entity entity) {
        if (entity == null) return 0.0f;
        try {
            return com.jamie.jamiebingo.util.EntityRotationUtil.getXRot(entity);
        } catch (Throwable ignored) {
        }
        Float v = invokeFloatGetter(entity, "getXRot");
        if (v != null) return v;
        v = invokeFloatGetter(entity, "xRot");
        if (v != null) return v;
        v = invokeFloatGetter(entity, "getRotationPitch");
        if (v != null) return v;
        return 0.0f;
    }

    private static Float invokeFloatGetter(Entity entity, String name) {
        try {
            Method m = entity.getClass().getMethod(name);
            if (m.getReturnType() == float.class || m.getReturnType() == Float.class) {
                Object v = m.invoke(entity);
                if (v instanceof Float f) return f;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}

