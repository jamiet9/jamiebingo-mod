package com.jamie.jamiebingo.util;

import net.minecraft.world.item.Item;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ItemPropertiesUtil {

    private ItemPropertiesUtil() {}

    public static Item.Properties withId(Item.Properties props, Object resourceKey, String descriptionId) {
        if (props == null) return null;
        if (resourceKey != null) {
            Class<?> keyClass = resourceKey.getClass();
            // Prefer direct field injection to avoid obf method ambiguity
            for (Field f : Item.Properties.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!"f".equals(f.getName())) continue;
                try {
                    f.setAccessible(true);
                    f.set(props, resourceKey);
                    return props;
                } catch (Exception ignored) {
                }
            }
            for (Field f : Item.Properties.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                String typeName = f.getType().getName();
                if (!typeName.contains("ResourceKey")) continue;
                try {
                    f.setAccessible(true);
                    f.set(props, resourceKey);
                    return props;
                } catch (Exception ignored) {
                }
            }
            for (Method m : Item.Properties.class.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getReturnType() != Item.Properties.class) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(keyClass)) {
                    try {
                        m.setAccessible(true);
                        return (Item.Properties) m.invoke(props, resourceKey);
                    } catch (Exception ignored) {
                    }
                }
            }
            for (Field f : Item.Properties.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!f.getType().isAssignableFrom(keyClass)) continue;
                try {
                    f.setAccessible(true);
                    f.set(props, resourceKey);
                    return props;
                } catch (Exception ignored) {
                }
            }
            for (Field f : Item.Properties.class.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!f.getName().toLowerCase().contains("id")) continue;
                try {
                    f.setAccessible(true);
                    f.set(props, resourceKey);
                    return props;
                } catch (Exception ignored) {
                }
            }
        }
        if (descriptionId != null && !descriptionId.isBlank()) {
            try {
                for (Method m : Item.Properties.class.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers())) continue;
                    if (m.getReturnType() != Item.Properties.class) continue;
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 1 && params[0] == String.class) {
                        m.setAccessible(true);
                        return (Item.Properties) m.invoke(props, descriptionId);
                    }
                }
                for (Field f : Item.Properties.class.getDeclaredFields()) {
                    if (Modifier.isStatic(f.getModifiers())) continue;
                    if (f.getType() != String.class) continue;
                    f.setAccessible(true);
                    f.set(props, descriptionId);
                    return props;
                }
            } catch (Exception ignored) {
            }
        }
        return props;
    }
}
