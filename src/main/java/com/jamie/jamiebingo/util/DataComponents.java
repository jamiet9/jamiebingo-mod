package com.jamie.jamiebingo.util;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.ResolvableProfile;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Launcher-safe DataComponents lookup (avoids static field names that are obfuscated in production).
 */
public final class DataComponents {

    public static DataComponentType<Component> CUSTOM_NAME = typed("minecraft:custom_name");
    public static DataComponentType<CustomData> CUSTOM_DATA = typed("minecraft:custom_data");
    public static DataComponentType<ItemLore> LORE = typed("minecraft:lore");
    public static DataComponentType<Boolean> ENCHANTMENT_GLINT_OVERRIDE = typed("minecraft:enchantment_glint_override");
    public static DataComponentType<LodestoneTracker> LODESTONE_TRACKER = typed("minecraft:lodestone_tracker");
    public static DataComponentType<ResolvableProfile> PROFILE = typed("minecraft:profile");
    public static DataComponentType<DyedItemColor> DYED_COLOR = typed("minecraft:dyed_color");
    public static DataComponentType<Object> POTION_CONTENTS = typed("minecraft:potion_contents");
    public static DataComponentType<Fireworks> FIREWORKS = typed("minecraft:fireworks");
    public static DataComponentType<?> FOOD = typeRaw("minecraft:food");
    public static DataComponentType<Object> JUKEBOX_PLAYABLE = typed("minecraft:jukebox_playable");
    public static DataComponentType<Object> TRIM = typed("minecraft:trim");
    public static DataComponentType<Object> BUNDLE_CONTENTS = typed("minecraft:bundle_contents");
    public static DataComponentType<Object> BANNER_PATTERNS = typed("minecraft:banner_patterns");
    public static DataComponentType<Object> ENCHANTMENTS = typed("minecraft:enchantments");
    public static DataComponentType<Object> STORED_ENCHANTMENTS = typed("minecraft:stored_enchantments");

    private DataComponents() {
    }

    public static void refresh() {
        if (CUSTOM_NAME == null) CUSTOM_NAME = typed("minecraft:custom_name");
        if (CUSTOM_DATA == null) CUSTOM_DATA = typed("minecraft:custom_data");
        if (LORE == null) LORE = typed("minecraft:lore");
        if (ENCHANTMENT_GLINT_OVERRIDE == null) ENCHANTMENT_GLINT_OVERRIDE = typed("minecraft:enchantment_glint_override");
        if (LODESTONE_TRACKER == null) LODESTONE_TRACKER = typed("minecraft:lodestone_tracker");
        if (PROFILE == null) PROFILE = typed("minecraft:profile");
        if (DYED_COLOR == null) DYED_COLOR = typed("minecraft:dyed_color");
        if (POTION_CONTENTS == null) POTION_CONTENTS = typed("minecraft:potion_contents");
        if (FIREWORKS == null) FIREWORKS = typed("minecraft:fireworks");
        if (FOOD == null) FOOD = typeRaw("minecraft:food");
        if (JUKEBOX_PLAYABLE == null) JUKEBOX_PLAYABLE = typed("minecraft:jukebox_playable");
        if (TRIM == null) TRIM = typed("minecraft:trim");
        if (BUNDLE_CONTENTS == null) BUNDLE_CONTENTS = typed("minecraft:bundle_contents");
        if (BANNER_PATTERNS == null) BANNER_PATTERNS = typed("minecraft:banner_patterns");
        if (ENCHANTMENTS == null) ENCHANTMENTS = typed("minecraft:enchantments");
        if (STORED_ENCHANTMENTS == null) STORED_ENCHANTMENTS = typed("minecraft:stored_enchantments");
    }

    private static DataComponentType<?> typeRaw(String id) {
        // 0) Try vanilla DataComponents static fields first (most reliable in production).
        try {
            Class<?> dc = Class.forName("net.minecraft.core.component.DataComponents");
            String simple = id;
            int idx = id.indexOf(':');
            if (idx >= 0 && idx + 1 < id.length()) {
                simple = id.substring(idx + 1);
            }
            String fieldName = simple.toUpperCase();
            for (Field f : dc.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!DataComponentType.class.isAssignableFrom(f.getType())) continue;
                if (!f.getName().equalsIgnoreCase(fieldName)) continue;
                f.setAccessible(true);
                Object v = f.get(null);
                if (v instanceof DataComponentType<?> dct) return dct;
            }
        } catch (Throwable ignored) {
        }
        Identifier key = IdUtil.id(id);
        Object registry = findDataComponentRegistry();
        if (registry == null) return null;
        Object value = invokeGet(registry, key);
        if (value instanceof DataComponentType<?> dct) return dct;
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> DataComponentType<T> typed(String id) {
        return (DataComponentType<T>) typeRaw(id);
    }

    private static Object findDataComponentRegistry() {
        // Try BuiltInRegistries with key match.
        try {
            Class<?> registriesClass = Class.forName("net.minecraft.core.registries.Registries");
            ResourceKey<?> dcKey = getRegistryKeyConstant(registriesClass, "DATA_COMPONENT_TYPE");
            Class<?> builtInClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            for (Field f : builtInClass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Registry.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object reg = f.get(null);
                if (reg == null) continue;
                if (dcKey != null && dcKey.equals(getRegistryKey(reg))) {
                    return reg;
                }
            }
        } catch (Throwable ignored) {
        }

        // Fallback: any registry whose key contains "data_component_type".
        try {
            Class<?> builtInClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            for (Field f : builtInClass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Registry.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object reg = f.get(null);
                if (reg == null) continue;
                Object key = getRegistryKey(reg);
                if (key != null && key.toString().contains("data_component_type")) {
                    return reg;
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static ResourceKey<?> getRegistryKeyConstant(Class<?> registriesClass, String name) {
        try {
            Field f = registriesClass.getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(null);
            if (v instanceof ResourceKey<?> rk) return rk;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object getRegistryKey(Object registry) {
        try {
            Method key = registry.getClass().getDeclaredMethod("key");
            key.setAccessible(true);
            return key.invoke(registry);
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : registry.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!m.getName().toLowerCase().contains("key")) continue;
                m.setAccessible(true);
                return m.invoke(registry);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object invokeGet(Object registry, Identifier key) {
        try {
            Method get = registry.getClass().getMethod("get", Identifier.class);
            return get.invoke(registry, key);
        } catch (Throwable ignored) {
        }
        try {
            Method getValue = registry.getClass().getMethod("getValue", Identifier.class);
            return getValue.invoke(registry, key);
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : registry.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!m.getParameterTypes()[0].isAssignableFrom(Identifier.class)) continue;
                m.setAccessible(true);
                return m.invoke(registry, key);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
