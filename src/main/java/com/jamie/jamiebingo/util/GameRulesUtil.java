package com.jamie.jamiebingo.util;

import net.minecraft.world.level.gamerules.GameRules;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class GameRulesUtil {
    private GameRulesUtil() {}

    public static GameRules getGameRules(Object level) {
        if (level == null) return null;
        try {
            Method m = level.getClass().getMethod("getGameRules");
            return (GameRules) m.invoke(level);
        } catch (Exception ignored) {
        }
        for (Method m : level.getClass().getMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != GameRules.class) continue;
            try {
                return (GameRules) m.invoke(level);
            } catch (Exception ignored) {
            }
        }
        for (Method m : level.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != GameRules.class) continue;
            try {
                m.setAccessible(true);
                return (GameRules) m.invoke(level);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public static boolean getBoolean(GameRules rules, Object key, boolean fallback) {
        if (rules == null || key == null) return fallback;
        try {
            Method m = rules.getClass().getMethod("getBoolean", key.getClass());
            Object out = m.invoke(rules, key);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        try {
            Method m = rules.getClass().getMethod("get", key.getClass());
            Object value = m.invoke(rules, key);
            if (value != null) {
                Method get = value.getClass().getMethod("get");
                Object out = get.invoke(value);
                if (out instanceof Boolean b) return b;
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    public static boolean getBooleanByName(GameRules rules, boolean fallback, String... names) {
        if (rules == null || names == null || names.length == 0) return fallback;
        Object key = findRuleKey(names);
        if (key == null) return fallback;
        return getBoolean(rules, key, fallback);
    }

    public static void setBooleanByName(GameRules rules, Object server, boolean value, String... names) {
        if (rules == null || names == null || names.length == 0) return;
        Object key = findRuleKey(names);
        if (key == null) return;
        try {
            Method set = rules.getClass().getMethod("set", key.getClass(), boolean.class, Class.forName("net.minecraft.server.MinecraftServer"));
            set.invoke(rules, key, value, server);
            return;
        } catch (Throwable ignored) {
        }
        try {
            Method get = rules.getClass().getMethod("get", key.getClass());
            Object ruleVal = get.invoke(rules, key);
            if (ruleVal != null) {
                Method set = ruleVal.getClass().getMethod("set", String.class);
                set.invoke(ruleVal, Boolean.toString(value));
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object findRuleKey(String... names) {
        try {
            for (Field f : GameRules.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                Class<?> type = f.getType();
                if (!type.getName().contains("Key")) continue;
                try {
                    f.setAccessible(true);
                    Object key = f.get(null);
                    if (key == null) continue;
                    String keyName = getRuleName(key);
                    if (keyName == null) continue;
                    for (String n : names) {
                        if (n == null) continue;
                        if (keyName.equalsIgnoreCase(n) || keyName.toLowerCase().contains(n.toLowerCase())) {
                            return key;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String getRuleName(Object key) {
        try {
            Method m = key.getClass().getMethod("getId");
            Object out = m.invoke(key);
            if (out != null) return out.toString();
        } catch (Throwable ignored) {
        }
        try {
            Method m = key.getClass().getMethod("getName");
            Object out = m.invoke(key);
            if (out != null) return out.toString();
        } catch (Throwable ignored) {
        }
        try {
            Method m = key.getClass().getMethod("getKey");
            Object out = m.invoke(key);
            if (out != null) return out.toString();
        } catch (Throwable ignored) {
        }
        return key.toString();
    }
}
