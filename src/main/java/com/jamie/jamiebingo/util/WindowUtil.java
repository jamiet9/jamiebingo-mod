package com.jamie.jamiebingo.util;

import java.lang.reflect.Method;

public final class WindowUtil {
    private WindowUtil() {
    }

    public static int getGuiScale(Object window) {
        if (window == null) return 1;
        try {
            Method m = window.getClass().getMethod("getGuiScale");
            Object out = m.invoke(window);
            if (out instanceof Number n) return Math.max(1, n.intValue());
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : window.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("guiscale")) continue;
                Object out = m.invoke(window);
                if (out instanceof Number n) return Math.max(1, n.intValue());
            }
        } catch (Throwable ignored) {
        }
        return 1;
    }

    public static int getGuiScaledWidth(Object window) {
        if (window == null) return 0;
        Integer out = invokeInt(window, "guiscaledwidth");
        return out != null ? out : 0;
    }

    public static int getGuiScaledHeight(Object window) {
        if (window == null) return 0;
        Integer out = invokeInt(window, "guiscaledheight");
        return out != null ? out : 0;
    }

    private static Integer invokeInt(Object window, String nameContains) {
        try {
            for (Method m : window.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (m.getReturnType() != int.class && m.getReturnType() != Integer.class) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains(nameContains)) continue;
                Object out = m.invoke(window);
                if (out instanceof Number n) return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
