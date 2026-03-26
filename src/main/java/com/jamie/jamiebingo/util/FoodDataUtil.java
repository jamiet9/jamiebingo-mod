package com.jamie.jamiebingo.util;

import net.minecraft.world.food.FoodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class FoodDataUtil {
    private FoodDataUtil() {
    }

    public static int getFoodLevel(FoodData data) {
        if (data == null) return 0;
        try {
            Method m = data.getClass().getMethod("getFoodLevel");
            Object out = m.invoke(data);
            if (out instanceof Integer i) return i;
        } catch (Throwable ignored) {
        }
        Integer field = readIntFieldContaining(data, "food");
        return field != null ? field : 0;
    }

    public static void setFoodLevel(FoodData data, int level) {
        if (data == null) return;
        try {
            Method m = data.getClass().getMethod("setFoodLevel", int.class);
            m.invoke(data, level);
            return;
        } catch (Throwable ignored) {
        }
        writeIntFieldContaining(data, "food", level);
    }

    public static void setSaturation(FoodData data, float value) {
        if (data == null) return;
        try {
            Method m = data.getClass().getMethod("setSaturation", float.class);
            m.invoke(data, value);
            return;
        } catch (Throwable ignored) {
        }
        writeFloatFieldContaining(data, "saturation", value);
    }

    public static void addExhaustion(FoodData data, float value) {
        if (data == null) return;
        try {
            Method m = data.getClass().getMethod("addExhaustion", float.class);
            m.invoke(data, value);
            return;
        } catch (Throwable ignored) {
        }
        Float current = readFloatFieldContaining(data, "exhaust");
        if (current != null) {
            writeFloatFieldContaining(data, "exhaust", current + value);
        }
    }

    private static Integer readIntFieldContaining(FoodData data, String token) {
        for (Field f : data.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (f.getType() != int.class) continue;
            if (!f.getName().toLowerCase().contains(token)) continue;
            try {
                f.setAccessible(true);
                return f.getInt(data);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void writeIntFieldContaining(FoodData data, String token, int value) {
        for (Field f : data.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (f.getType() != int.class) continue;
            if (!f.getName().toLowerCase().contains(token)) continue;
            try {
                f.setAccessible(true);
                f.setInt(data, value);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private static Float readFloatFieldContaining(FoodData data, String token) {
        for (Field f : data.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (f.getType() != float.class) continue;
            if (!f.getName().toLowerCase().contains(token)) continue;
            try {
                f.setAccessible(true);
                return f.getFloat(data);
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    private static void writeFloatFieldContaining(FoodData data, String token, float value) {
        for (Field f : data.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            if (f.getType() != float.class) continue;
            if (!f.getName().toLowerCase().contains(token)) continue;
            try {
                f.setAccessible(true);
                f.setFloat(data, value);
                return;
            } catch (Throwable ignored) {
            }
        }
    }
}
