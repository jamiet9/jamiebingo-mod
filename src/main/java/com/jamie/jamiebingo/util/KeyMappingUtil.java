package com.jamie.jamiebingo.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class KeyMappingUtil {
    private KeyMappingUtil() {}

    public static boolean consumeClick(Object keyMapping) {
        if (keyMapping == null) return false;

        Method bestConsume = null;
        Method bestDown = null;

        for (Method m : keyMapping.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class) continue;

            String name = m.getName().toLowerCase();
            if (name.contains("consume")) {
                bestConsume = m;
                break;
            }
            if (name.equals("isdown") || name.contains("down") || name.contains("pressed")) {
                if (bestDown == null) bestDown = m;
            }
        }

        if (invokeBool(keyMapping, bestConsume)) return true;
        return invokeBool(keyMapping, bestDown);
    }

    private static boolean invokeBool(Object target, Method m) {
        if (m == null) return false;
        try {
            Object out = m.invoke(target);
            return out instanceof Boolean b ? b : (out != null && (boolean) out);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
