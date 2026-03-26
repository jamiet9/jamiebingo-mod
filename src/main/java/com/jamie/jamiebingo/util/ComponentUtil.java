package com.jamie.jamiebingo.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.ChatFormatting;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ComponentUtil {
    private ComponentUtil() {
    }

    public static MutableComponent literal(String text) {
        try {
            Method m = Component.class.getMethod("literal", String.class);
            Object out = m.invoke(null, text);
            if (out instanceof MutableComponent mc) return mc;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : Component.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 1 || m.getParameterTypes()[0] != String.class) continue;
                if (!MutableComponent.class.isAssignableFrom(m.getReturnType())) continue;
                m.setAccessible(true);
                Object out = m.invoke(null, text);
                if (out instanceof MutableComponent mc) return mc;
            }
        } catch (Throwable ignored) {
        }
        return empty();
    }

    public static MutableComponent empty() {
        try {
            Method m = Component.class.getMethod("empty");
            Object out = m.invoke(null);
            if (out instanceof MutableComponent mc) return mc;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : Component.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!MutableComponent.class.isAssignableFrom(m.getReturnType())) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("empty")) continue;
                m.setAccessible(true);
                Object out = m.invoke(null);
                if (out instanceof MutableComponent mc) return mc;
            }
        } catch (Throwable ignored) {
        }
        return literal("");
    }

    public static MutableComponent translatable(String key) {
        try {
            Method m = Component.class.getMethod("translatable", String.class);
            Object out = m.invoke(null, key);
            if (out instanceof MutableComponent mc) return mc;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : Component.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 1 || m.getParameterTypes()[0] != String.class) continue;
                if (!MutableComponent.class.isAssignableFrom(m.getReturnType())) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("trans")) continue;
                m.setAccessible(true);
                Object out = m.invoke(null, key);
                if (out instanceof MutableComponent mc) return mc;
            }
        } catch (Throwable ignored) {
        }
        return literal(key);
    }

    public static MutableComponent translatable(String key, Object... args) {
        try {
            Method m = Component.class.getMethod("translatable", String.class, Object[].class);
            Object out = m.invoke(null, key, args);
            if (out instanceof MutableComponent mc) return mc;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : Component.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                Class<?>[] params = m.getParameterTypes();
                if (params.length != 2) continue;
                if (params[0] != String.class || !params[1].isArray()) continue;
                if (!MutableComponent.class.isAssignableFrom(m.getReturnType())) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("trans")) continue;
                m.setAccessible(true);
                Object out = m.invoke(null, key, args);
                if (out instanceof MutableComponent mc) return mc;
            }
        } catch (Throwable ignored) {
        }
        return literal(key);
    }

    public static Style getStyle(Component component) {
        if (component == null) return Style.EMPTY;
        try {
            Method m = Component.class.getMethod("getStyle");
            Object out = m.invoke(component);
            if (out instanceof Style s) return s;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : Component.class.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!Style.class.isAssignableFrom(m.getReturnType())) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("style")) continue;
                m.setAccessible(true);
                Object out = m.invoke(component);
                if (out instanceof Style s) return s;
            }
        } catch (Throwable ignored) {
        }
        return Style.EMPTY;
    }

    public static MutableComponent withColor(MutableComponent component, ChatFormatting color) {
        if (component == null || color == null) return component;
        // Try MutableComponent.withStyle(ChatFormatting)
        try {
            Method m = component.getClass().getMethod("withStyle", ChatFormatting.class);
            Object out = m.invoke(component, color);
            if (out instanceof MutableComponent mc) return mc;
        } catch (Throwable ignored) {
        }
        // Try MutableComponent.withStyle(Style)
        try {
            Method m = component.getClass().getMethod("withStyle", Style.class);
            Style s = Style.EMPTY.withColor(color);
            Object out = m.invoke(component, s);
            if (out instanceof MutableComponent mc) return mc;
        } catch (Throwable ignored) {
        }
        // Fallback: try to mutate style directly
        try {
            Style s = getStyle(component);
            Style s2 = s.withColor(color);
            Method m = component.getClass().getMethod("setStyle", Style.class);
            Object out = m.invoke(component, s2);
            if (out instanceof MutableComponent mc) return mc;
        } catch (Throwable ignored) {
        }
        return component;
    }

    public static String getInsertion(Style style) {
        if (style == null) return null;
        try {
            Method m = Style.class.getMethod("getInsertion");
            Object out = m.invoke(style);
            return out instanceof String s ? s : null;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : Style.class.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (m.getReturnType() != String.class && m.getReturnType() != Object.class) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("insert")) continue;
                m.setAccessible(true);
                Object out = m.invoke(style);
                return out instanceof String s ? s : null;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static java.util.List<Component> getSiblings(Component component) {
        if (component == null) return java.util.Collections.emptyList();
        try {
            Method m = Component.class.getMethod("getSiblings");
            Object out = m.invoke(component);
            if (out instanceof java.util.List<?> list) {
                @SuppressWarnings("unchecked")
                java.util.List<Component> typed = (java.util.List<Component>) list;
                return typed;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : Component.class.getDeclaredMethods()) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 0) continue;
                if (!java.util.List.class.isAssignableFrom(m.getReturnType())) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("sibling") && !name.contains("children")) continue;
                m.setAccessible(true);
                Object out = m.invoke(component);
                if (out instanceof java.util.List<?> list) {
                    @SuppressWarnings("unchecked")
                    java.util.List<Component> typed = (java.util.List<Component>) list;
                    return typed;
                }
            }
        } catch (Throwable ignored) {
        }
        return java.util.Collections.emptyList();
    }

    public static String toJson(Component component) {
        if (component == null) return "{\"text\":\"\"}";
        try {
            Class<?> serializer = Class.forName("net.minecraft.network.chat.Component$Serializer");
            for (Method m : serializer.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 1) continue;
                if (!Component.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                if (m.getReturnType() != String.class) continue;
                m.setAccessible(true);
                Object out = m.invoke(null, component);
                if (out instanceof String s) return s;
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> serializer = Class.forName("net.minecraft.network.chat.ComponentSerializer");
            for (Method m : serializer.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) continue;
                if (m.getParameterCount() != 1) continue;
                if (!Component.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                if (m.getReturnType() != String.class) continue;
                m.setAccessible(true);
                Object out = m.invoke(null, component);
                if (out instanceof String s) return s;
            }
        } catch (Throwable ignored) {
        }
        String text = component.getString();
        String escaped = text.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"text\":\"" + escaped + "\"}";
    }
}
