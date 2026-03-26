package com.jamie.jamiebingo.util;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

public final class BlockLookupUtil {

    private BlockLookupUtil() {
    }

    public static Block block(String id) {
        Identifier key = IdUtil.id(id);
        Block direct = tryForgeRegistry(key);
        if (direct != null) return direct;

        Object registry = findBlockRegistry();
        if (registry == null) {
            return null;
        }

        Object value = invokeGet(registry, key);
        return unwrapBlock(value);
    }

    private static Block tryForgeRegistry(Identifier key) {
        try {
            Block block = ForgeRegistries.BLOCKS.getValue(key);
            if (block != null) return block;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object findBlockRegistry() {
        // Prefer Forge registry if available
        Object forge = findForgeBlockRegistry();
        if (forge != null) return forge;

        try {
            Class<?> registriesClass = Class.forName("net.minecraft.core.registries.Registries");
            ResourceKey<?> blockKey = getRegistryKeyConstant(registriesClass, "BLOCK");
            Class<?> builtInClass = Class.forName("net.minecraft.core.registries.BuiltInRegistries");
            for (Field f : builtInClass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Registry.class.isAssignableFrom(f.getType())) continue;
                try {
                    f.setAccessible(true);
                    Object reg = f.get(null);
                    if (reg == null) continue;
                    if (blockKey != null && blockKey.equals(getRegistryKey(reg))) {
                        return reg;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        }

        // Fallback: pick any registry whose key string contains "block"
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
                    if (key != null && key.toString().contains("block")) {
                        return reg;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        } catch (ClassNotFoundException ignored) {
        }
        return null;
    }

    private static Object findForgeBlockRegistry() {
        try {
            Class<?> forgeRegistries = Class.forName("net.minecraftforge.registries.ForgeRegistries");
            for (Field f : forgeRegistries.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!f.getName().equals("BLOCKS")) continue;
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

    private static Block unwrapBlock(Object value) {
        if (value == null) return null;
        if (value instanceof Block block) return block;
        if (value instanceof Optional<?> opt) {
            return unwrapBlock(opt.orElse(null));
        }
        try {
            Method valueMethod = value.getClass().getMethod("value");
            Object v = valueMethod.invoke(value);
            if (v instanceof Block block) return block;
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
