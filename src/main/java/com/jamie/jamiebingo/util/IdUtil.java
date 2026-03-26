package com.jamie.jamiebingo.util;

import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class IdUtil {

    private IdUtil() {}

    public static Identifier id(String value) {
        // 1) Preferred: parse(full) -> Identifier (usually named "a" in obf)
        for (Method m : Identifier.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (!"a".equals(m.getName())) continue;
            if (m.getReturnType() != Identifier.class) continue;
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 1 && params[0] == String.class) {
                try {
                    m.setAccessible(true);
                    return (Identifier) m.invoke(null, value);
                } catch (Exception ignored) {
                }
            }
        }
        // 2) Fallback: any static Identifier(String)
        for (Method m : Identifier.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (m.getReturnType() != Identifier.class) continue;
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 1 && params[0] == String.class) {
                try {
                    m.setAccessible(true);
                    return (Identifier) m.invoke(null, value);
                } catch (Exception ignored) {
                }
            }
        }

        int idx = value.indexOf(':');
        if (idx > 0 && idx < value.length() - 1) {
            String ns = value.substring(0, idx);
            String path = value.substring(idx + 1);
            // 3) fromNamespaceAndPath(ns, path)
            for (Method m : Identifier.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (!"a".equals(m.getName())) continue;
                if (m.getReturnType() != Identifier.class) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2 && params[0] == String.class && params[1] == String.class) {
                    try {
                        m.setAccessible(true);
                        return (Identifier) m.invoke(null, ns, path);
                    } catch (Exception ignored) {
                    }
                }
            }
            for (Method m : Identifier.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getReturnType() != Identifier.class) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 2 && params[0] == String.class && params[1] == String.class) {
                    try {
                        m.setAccessible(true);
                        return (Identifier) m.invoke(null, ns, path);
                    } catch (Exception ignored) {
                    }
                }
            }
        }

        // 4) parse with separator (String, char)
        for (Method m : Identifier.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (m.getReturnType() != Identifier.class) continue;
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 2 && params[0] == String.class && params[1] == char.class) {
                try {
                    m.setAccessible(true);
                    return (Identifier) m.invoke(null, value, ':');
                } catch (Exception ignored) {
                }
            }
        }

        // 5) Optional<Identifier> parse(String)
        for (Method m : Identifier.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != String.class) continue;
            if (!java.util.Optional.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                m.setAccessible(true);
                Object opt = m.invoke(null, value);
                if (opt instanceof java.util.Optional<?> optional) {
                    Object got = optional.orElse(null);
                    if (got instanceof Identifier id) return id;
                }
            } catch (Exception ignored) {
            }
        }

        // 6) DataResult<Identifier> parse(String)
        for (Method m : Identifier.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0] != String.class) continue;
            if (!m.getReturnType().getName().contains("DataResult")) continue;
            try {
                m.setAccessible(true);
                Object dataResult = m.invoke(null, value);
                if (dataResult != null) {
                    try {
                        Method resultMethod = dataResult.getClass().getMethod("result");
                        Object opt = resultMethod.invoke(dataResult);
                        if (opt instanceof java.util.Optional<?> optional) {
                            Object got = optional.orElse(null);
                            if (got instanceof Identifier id) return id;
                        }
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ignored) {
            }
        }
        throw new IllegalStateException("Unable to resolve Identifier for: " + value);
    }
}
