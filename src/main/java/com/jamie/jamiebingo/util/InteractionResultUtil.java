package com.jamie.jamiebingo.util;

import net.minecraft.world.InteractionResult;

public final class InteractionResultUtil {
    private InteractionResultUtil() {
    }

    public static InteractionResult success() {
        return byName("SUCCESS");
    }

    public static InteractionResult pass() {
        return byName("PASS");
    }

    private static InteractionResult byName(String name) {
        if (name == null) return null;
        try {
            java.lang.reflect.Field f = InteractionResult.class.getField(name);
            Object v = f.get(null);
            if (v instanceof InteractionResult r) return r;
        } catch (Throwable ignored) {
        }
        try {
            for (java.lang.reflect.Field f : InteractionResult.class.getFields()) {
                if (!InteractionResult.class.isAssignableFrom(f.getType())) continue;
                if (!f.getName().equalsIgnoreCase(name)) continue;
                Object v = f.get(null);
                if (v instanceof InteractionResult r) return r;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
