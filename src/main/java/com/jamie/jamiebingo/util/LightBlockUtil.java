package com.jamie.jamiebingo.util;

import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class LightBlockUtil {

    private LightBlockUtil() {
    }

    public static IntegerProperty levelProperty() {
        // Try the named field first
        try {
            Field f = LightBlock.class.getDeclaredField("LEVEL");
            f.setAccessible(true);
            Object v = f.get(null);
            if (v instanceof IntegerProperty ip) return ip;
        } catch (ReflectiveOperationException ignored) {
        }

        // Fallback: any static IntegerProperty on LightBlock
        for (Field f : LightBlock.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) continue;
            if (!IntegerProperty.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(null);
                if (v instanceof IntegerProperty ip) return ip;
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }
}
