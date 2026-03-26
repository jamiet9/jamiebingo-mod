package com.jamie.jamiebingo.util;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

public final class ItemLookupUtil {
    private static final Map<String, Item> CACHE = new ConcurrentHashMap<>();

    private ItemLookupUtil() {
    }

    public static Item item(String id) {
        if (id == null) return null;
        Item cached = CACHE.get(id);
        if (cached != null || CACHE.containsKey(id)) return cached;
        Item fallback = ItemStackUtil.getItem(ItemStackUtil.empty());
        Identifier key = IdUtil.id(id);
        Item direct = tryForgeRegistry(key);
        if (direct != null) {
            CACHE.put(id, direct);
            return direct;
        }

        Object registry = findItemRegistry();
        if (registry == null) {
            CACHE.put(id, fallback);
            return fallback;
        }

        Object value = invokeGet(registry, key);
        Item item = unwrapItem(value);
        Item resolved = item != null ? item : fallback;
        CACHE.put(id, resolved);
        return resolved;
    }

    public static ItemStack stack(String id) {
        Item item = item(id);
        return item != null ? new ItemStack(item) : ItemStackUtil.empty();
    }

    public static boolean is(ItemStack stack, String id) {
        Item item = item(id);
        if (item == null) return false;
        return item == ItemStackUtil.getItem(stack);
    }

    public static boolean is(Item item, String id) {
        Item expected = item(id);
        return expected != null && expected == item;
    }

    private static Item tryForgeRegistry(Identifier key) {
        try {
            Item item = ForgeRegistries.ITEMS.getValue(key);
            if (item != null) return item;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object findItemRegistry() {
        // Prefer Forge registry if available
        Object forge = findForgeItemRegistry();
        if (forge != null) return forge;

        try {
            Class<?> registriesClass = Class.forName("net.minecraft.core.registries.Registries");
            ResourceKey<?> itemKey = getRegistryKeyConstant(registriesClass, "ITEM");
            Class<?> builtInClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            for (Field f : builtInClass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Registry.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object reg = f.get(null);
                    if (reg == null) continue;
                    if (itemKey != null && itemKey.equals(getRegistryKey(reg))) {
                        return reg;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        }

        // Fallback: pick any registry whose key string contains "item"
        try {
            Class<?> builtInClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            for (Field f : builtInClass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Registry.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object reg = f.get(null);
                    if (reg == null) continue;
                    Object key = getRegistryKey(reg);
                    if (key != null && key.toString().contains("item")) {
                        return reg;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    private static Object findForgeItemRegistry() {
        try {
            Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
            for (Field f : forgeRegistries.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!f.getName().equals("ITEMS")) continue;
                f.setAccessible(true);
                return f.get(null);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static ResourceKey<?> getRegistryKeyConstant(Class<?> registriesClass, String name) {
        try {
            Field f = registriesClass.getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(null);
            if (v instanceof ResourceKey<?> rk) {
                return rk;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Object getRegistryKey(Object registry) {
        try {
            Method key = registry.getClass().getDeclaredMethod("key");
            key.setAccessible(true);
            return key.invoke(registry);
        } catch (ReflectiveOperationException ignored) {
        }
        for (Method m : registry.getClass().getDeclaredMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (!ResourceKey.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                m.setAccessible(true);
                return m.invoke(registry);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Object invokeGet(Object registry, Identifier key) {
        try {
            Method get = registry.getClass().getMethod("get", Identifier.class);
            return get.invoke(registry, key);
        } catch (ReflectiveOperationException ignored) {
        }
        try {
            Method getValue = registry.getClass().getMethod("getValue", Identifier.class);
            return getValue.invoke(registry, key);
        } catch (ReflectiveOperationException ignored) {
        }
        for (Method m : registry.getClass().getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (!Identifier.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
            if (!"get".equals(m.getName()) && !"getValue".equals(m.getName())) continue;
            try {
                return m.invoke(registry, key);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }

    private static Item unwrapItem(Object value) {
        if (value == null) return null;
        if (value instanceof Item item) return item;
        if (value instanceof Optional<?> opt) {
            return unwrapItem(opt.orElse(null));
        }
        try {
            Method valueMethod = value.getClass().getMethod("value");
            Object v = valueMethod.invoke(value);
            if (v instanceof Item item) return item;
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
