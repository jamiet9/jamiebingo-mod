package com.jamie.jamiebingo.util;

import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class SavedDataUtil {

    private SavedDataUtil() {}

    public static void markDirty(SavedData data) {
        if (data == null) return;
        try {
            Method m = data.getClass().getMethod("setDirty");
            m.setAccessible(true);
            m.invoke(data);
            return;
        } catch (Exception ignored) {
        }
        try {
            Method m = data.getClass().getMethod("markDirty");
            m.setAccessible(true);
            m.invoke(data);
            return;
        } catch (Exception ignored) {
        }
        for (Method m : data.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != void.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains("dirty")) continue;
            try {
                m.setAccessible(true);
                m.invoke(data);
                return;
            } catch (Exception ignored) {
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends SavedData> T computeIfAbsent(DimensionDataStorage storage, SavedDataType<T> type) {
        if (storage == null || type == null) return null;
        Class<?> typeClass = type.getClass();
        // First, try strict matches on param type
        for (Method m : storage.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (!SavedData.class.isAssignableFrom(m.getReturnType())) continue;
            if (!m.getParameterTypes()[0].isAssignableFrom(typeClass)) continue;
            try {
                m.setAccessible(true);
                return (T) m.invoke(storage, type);
            } catch (Exception ignored) {
            }
        }
        for (Method m : storage.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (!SavedData.class.isAssignableFrom(m.getReturnType())) continue;
            if (!m.getParameterTypes()[0].isAssignableFrom(typeClass)) continue;
            try {
                m.setAccessible(true);
                return (T) m.invoke(storage, type);
            } catch (Exception ignored) {
            }
        }

        // Next, try any 1-arg method returning SavedData and attempt invoke
        for (Method m : storage.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (!SavedData.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                m.setAccessible(true);
                return (T) m.invoke(storage, type);
            } catch (Exception ignored) {
            }
        }
        for (Method m : storage.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (!SavedData.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                m.setAccessible(true);
                return (T) m.invoke(storage, type);
            } catch (Exception ignored) {
            }
        }

        // Optional<SavedData> methods
        for (Method m : storage.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (!java.util.Optional.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                m.setAccessible(true);
                Object opt = m.invoke(storage, type);
                if (opt instanceof java.util.Optional<?> optional) {
                    Object got = optional.orElse(null);
                    if (got != null) return (T) got;
                }
            } catch (Exception ignored) {
            }
        }
        for (Method m : storage.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (!java.util.Optional.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                m.setAccessible(true);
                Object opt = m.invoke(storage, type);
                if (opt instanceof java.util.Optional<?> optional) {
                    Object got = optional.orElse(null);
                    if (got != null) return (T) got;
                }
            } catch (Exception ignored) {
            }
        }

        // Last resort: try obf method names (a = computeIfAbsent, b = get)
        for (Method m : storage.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            String name = m.getName();
            if (!"a".equals(name) && !"b".equals(name)) continue;
            try {
                m.setAccessible(true);
                Object result = m.invoke(storage, type);
                if (result instanceof java.util.Optional<?> optional) {
                    Object got = optional.orElse(null);
                    if (got != null) return (T) got;
                }
                if (result instanceof SavedData data) {
                    return (T) data;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static boolean set(DimensionDataStorage storage, SavedDataType<? extends SavedData> type, SavedData data) {
        if (storage == null || type == null || data == null) return false;
        Class<?> typeClass = type.getClass();
        try {
            for (Method m : storage.getClass().getMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 2) continue;
                if (m.getReturnType() != void.class) continue;
                Class<?>[] params = m.getParameterTypes();
                if (!params[0].isAssignableFrom(typeClass)) continue;
                if (!params[1].isAssignableFrom(data.getClass())) continue;
                m.setAccessible(true);
                m.invoke(storage, type, data);
                return true;
            }
            for (Method m : storage.getClass().getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 2) continue;
                if (m.getReturnType() != void.class) continue;
                Class<?>[] params = m.getParameterTypes();
                if (!params[0].isAssignableFrom(typeClass)) continue;
                if (!params[1].isAssignableFrom(data.getClass())) continue;
                m.setAccessible(true);
                m.invoke(storage, type, data);
                return true;
            }
            for (Method m : storage.getClass().getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 2) continue;
                if (m.getReturnType() != void.class) continue;
                String name = m.getName();
                if (!"a".equals(name)) continue;
                try {
                    m.setAccessible(true);
                    m.invoke(storage, type, data);
                    return true;
                } catch (Exception ignored) {
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
