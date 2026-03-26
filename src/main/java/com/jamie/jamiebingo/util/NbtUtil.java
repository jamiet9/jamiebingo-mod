package com.jamie.jamiebingo.util;

import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class NbtUtil {
    private static Method PUT_BOOLEAN;
    private static Method PUT_INT;
    private static Method PUT_STRING;
    private static boolean LOOKED_UP;
    private static boolean LOOKED_UP_INT;
    private static boolean LOOKED_UP_STRING;

    private NbtUtil() {}

    public static void putBoolean(CompoundTag tag, String key, boolean value) {
        if (tag == null) return;
        if (!LOOKED_UP) {
            LOOKED_UP = true;
            try {
                PUT_BOOLEAN = CompoundTag.class.getDeclaredMethod("putBoolean", String.class, boolean.class);
            } catch (Throwable ignored) {
                PUT_BOOLEAN = null;
            }
        }
        if (PUT_BOOLEAN != null) {
            try {
                PUT_BOOLEAN.invoke(tag, key, value);
                return;
            } catch (Throwable ignored) {
            }
        }
        tag.putByte(key, (byte) (value ? 1 : 0));
    }

    public static void putInt(CompoundTag tag, String key, int value) {
        if (tag == null) return;
        if (!LOOKED_UP_INT) {
            LOOKED_UP_INT = true;
            try {
                PUT_INT = CompoundTag.class.getDeclaredMethod("putInt", String.class, int.class);
            } catch (Throwable ignored) {
                PUT_INT = null;
            }
            if (PUT_INT == null) {
                for (Method m : CompoundTag.class.getDeclaredMethods()) {
                    if (m.getParameterCount() != 2) continue;
                    Class<?>[] types = m.getParameterTypes();
                    if (types[0] != String.class || types[1] != int.class) continue;
                    if (m.getReturnType() != void.class) continue;
                    PUT_INT = m;
                    break;
                }
            }
        }
        if (PUT_INT != null) {
            try {
                PUT_INT.invoke(tag, key, value);
                return;
            } catch (Throwable ignored) {
            }
        }
        tag.putIntArray(key, new int[]{value});
    }

    public static void putString(CompoundTag tag, String key, String value) {
        if (tag == null) return;
        if (value == null) value = "";
        if (!LOOKED_UP_STRING) {
            LOOKED_UP_STRING = true;
            try {
                PUT_STRING = CompoundTag.class.getDeclaredMethod("putString", String.class, String.class);
            } catch (Throwable ignored) {
                PUT_STRING = null;
            }
            if (PUT_STRING == null) {
                for (Method m : CompoundTag.class.getDeclaredMethods()) {
                    if (m.getParameterCount() != 2) continue;
                    Class<?>[] types = m.getParameterTypes();
                    if (types[0] != String.class || types[1] != String.class) continue;
                    if (m.getReturnType() != void.class) continue;
                    PUT_STRING = m;
                    break;
                }
            }
        }
        if (PUT_STRING != null) {
            try {
                PUT_STRING.invoke(tag, key, value);
                return;
            } catch (Throwable ignored) {
            }
        }
        tag.putByteArray(key, value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static void putTag(CompoundTag tag, String key, net.minecraft.nbt.Tag value) {
        try {
            Method m = CompoundTag.class.getDeclaredMethod("put", String.class, net.minecraft.nbt.Tag.class);
            m.setAccessible(true);
            m.invoke(tag, key, value);
            return;
        } catch (ReflectiveOperationException ignored) {
        }

        for (Method m : CompoundTag.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 2) continue;
            if (m.getParameterTypes()[0] != String.class) continue;
            if (!net.minecraft.nbt.Tag.class.isAssignableFrom(m.getParameterTypes()[1])) continue;
            try {
                m.setAccessible(true);
                m.invoke(tag, key, value);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }

        throw new IllegalStateException("Unable to put Tag into CompoundTag");
    }

    public static boolean getBoolean(CompoundTag tag, String key, boolean fallback) {
        if (tag == null || key == null) return fallback;
        try {
            Method m = CompoundTag.class.getDeclaredMethod("getBoolean", String.class);
            m.setAccessible(true);
            Object out = m.invoke(tag, key);
            Boolean v = coerceBoolean(out);
            if (v != null) return v;
        } catch (Throwable ignored) {
        }

        for (Method m : CompoundTag.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != String.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains("boolean")) continue;
            try {
                m.setAccessible(true);
                Object out = m.invoke(tag, key);
                Boolean v = coerceBoolean(out);
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
        }

        // Fallback: getByte/getInt
        Integer i = getInt(tag, key, null);
        if (i != null) return i != 0;
        return fallback;
    }

    public static int getInt(CompoundTag tag, String key, int fallback) {
        Integer v = getInt(tag, key, null);
        return v != null ? v : fallback;
    }

    private static Integer getInt(CompoundTag tag, String key, Integer fallback) {
        if (tag == null || key == null) return fallback;
        try {
            Method m = CompoundTag.class.getDeclaredMethod("getInt", String.class);
            m.setAccessible(true);
            Object out = m.invoke(tag, key);
            Integer v = coerceInt(out);
            if (v != null) return v;
        } catch (Throwable ignored) {
        }

        for (Method m : CompoundTag.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != String.class) continue;
            if (m.getReturnType() != int.class && m.getReturnType() != Integer.class) continue;
            try {
                m.setAccessible(true);
                Object out = m.invoke(tag, key);
                Integer v = coerceInt(out);
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
        }
        return fallback;
    }

    public static String getString(CompoundTag tag, String key, String fallback) {
        if (tag == null || key == null) return fallback;
        try {
            Method m = CompoundTag.class.getDeclaredMethod("getString", String.class);
            m.setAccessible(true);
            Object out = m.invoke(tag, key);
            String v = coerceString(out);
            if (v != null) return v;
        } catch (Throwable ignored) {
        }

        for (Method m : CompoundTag.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != String.class) continue;
            if (m.getReturnType() != String.class && m.getReturnType() != Object.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains("string")) continue;
            try {
                m.setAccessible(true);
                Object out = m.invoke(tag, key);
                String v = coerceString(out);
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
        }

        return fallback;
    }

    private static Boolean coerceBoolean(Object out) {
        if (out == null) return null;
        if (out instanceof Boolean b) return b;
        if (out instanceof Number n) return n.intValue() != 0;
        if (out instanceof java.util.Optional<?> opt) {
            Object v = opt.orElse(null);
            if (v instanceof Boolean b) return b;
            if (v instanceof Number n) return n.intValue() != 0;
        }
        if (out instanceof java.util.OptionalInt oi) return oi.orElse(0) != 0;
        return null;
    }

    private static Integer coerceInt(Object out) {
        if (out == null) return null;
        if (out instanceof Integer i) return i;
        if (out instanceof Number n) return n.intValue();
        if (out instanceof java.util.Optional<?> opt) {
            Object v = opt.orElse(null);
            if (v instanceof Integer i) return i;
            if (v instanceof Number n) return n.intValue();
        }
        if (out instanceof java.util.OptionalInt oi) return oi.orElse(0);
        return null;
    }

    private static String coerceString(Object out) {
        if (out == null) return null;
        if (out instanceof String s) return s;
        if (out instanceof java.util.Optional<?> opt) {
            Object v = opt.orElse(null);
            if (v instanceof String s) return s;
        }
        return out.toString();
    }

    public static boolean isEmptyTag(CompoundTag tag) {
        if (tag == null) return true;
        try {
            Method m = CompoundTag.class.getDeclaredMethod("isEmpty");
            m.setAccessible(true);
            Object out = m.invoke(tag);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        for (Method m : CompoundTag.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains("empty")) continue;
            try {
                m.setAccessible(true);
                Object out = m.invoke(tag);
                if (out instanceof Boolean b) return b;
            } catch (Throwable ignored) {
            }
        }
        try {
            Method m = CompoundTag.class.getDeclaredMethod("getAllKeys");
            m.setAccessible(true);
            Object out = m.invoke(tag);
            if (out instanceof java.util.Set<?> set) return set.isEmpty();
        } catch (Throwable ignored) {
        }
        for (Method m : CompoundTag.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (!java.util.Set.class.isAssignableFrom(m.getReturnType())) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains("key")) continue;
            try {
                m.setAccessible(true);
                Object out = m.invoke(tag);
                if (out instanceof java.util.Set<?> set) return set.isEmpty();
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static int[] getIntArray(CompoundTag tag, String key) {
        if (tag == null || key == null) return null;
        try {
            Method m = CompoundTag.class.getDeclaredMethod("getIntArray", String.class);
            m.setAccessible(true);
            Object out = m.invoke(tag, key);
            int[] v = coerceIntArray(out);
            if (v != null) return v;
        } catch (Throwable ignored) {
        }

        for (Method m : CompoundTag.class.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != String.class) continue;
            String name = m.getName().toLowerCase();
            if (!name.contains("int") || !name.contains("array")) continue;
            try {
                m.setAccessible(true);
                Object out = m.invoke(tag, key);
                int[] v = coerceIntArray(out);
                if (v != null) return v;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static int[] coerceIntArray(Object out) {
        if (out == null) return null;
        if (out instanceof int[] arr) return arr;
        if (out instanceof java.util.Optional<?> opt) {
            Object v = opt.orElse(null);
            if (v instanceof int[] arr) return arr;
        }
        return null;
    }
}
