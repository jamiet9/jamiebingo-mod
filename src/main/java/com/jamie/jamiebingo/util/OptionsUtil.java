package com.jamie.jamiebingo.util;

import net.minecraft.client.Options;

import java.lang.reflect.Method;

public final class OptionsUtil {
    private OptionsUtil() {
    }

    public static double getChatScale(Options options) {
        return getOptionDouble(options, "chatScale", 1.0);
    }

    public static double getChatHeightFocused(Options options) {
        return getOptionDouble(options, "chatHeightFocused", 1.0);
    }

    public static double getChatHeightUnfocused(Options options) {
        return getOptionDouble(options, "chatHeightUnfocused", 0.443);
    }

    public static double getChatLineSpacing(Options options) {
        return getOptionDouble(options, "chatLineSpacing", 0.0);
    }

    public static double getChatOpacity(Options options) {
        return getOptionDouble(options, "chatOpacity", 1.0);
    }

    private static double getOptionDouble(Options options, String methodName, double def) {
        if (options == null) return def;
        try {
            Method m = options.getClass().getMethod(methodName);
            Object opt = m.invoke(options);
            return readOptionValue(opt, def);
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : options.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!m.getName().toLowerCase().contains(methodName.toLowerCase().replace("_", ""))) continue;
                Object opt = m.invoke(options);
                return readOptionValue(opt, def);
            }
        } catch (Throwable ignored) {
        }
        return def;
    }

    private static double readOptionValue(Object opt, double def) {
        if (opt == null) return def;
        try {
            Method get = opt.getClass().getMethod("get");
            Object out = get.invoke(opt);
            if (out instanceof Number n) return n.doubleValue();
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : opt.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                Class<?> rt = m.getReturnType();
                if (!(Number.class.isAssignableFrom(rt)
                        || rt == double.class
                        || rt == float.class
                        || rt == int.class
                        || rt == long.class)) {
                    continue;
                }
                Object out = m.invoke(opt);
                if (out instanceof Number n) return n.doubleValue();
            }
        } catch (Throwable ignored) {
        }
        return def;
    }
}
