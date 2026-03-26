package com.jamie.jamiebingo.util;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec2;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ClientInputUtil {
    private ClientInputUtil() {
    }

    public static Object getInput(LocalPlayer player) {
        if (player == null) return null;
        try {
            Field f = player.getClass().getField("input");
            Object out = f.get(player);
            if (out != null) return out;
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : player.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                String name = f.getName().toLowerCase();
                Class<?> type = f.getType();
                String typeName = type.getName().toLowerCase();
                if (!name.contains("input") && !typeName.contains("input")) continue;
                f.setAccessible(true);
                Object out = f.get(player);
                if (out != null) return out;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Vec2 getMoveVector(Object input) {
        if (input == null) return null;
        try {
            Method m = input.getClass().getMethod("getMoveVector");
            Object out = m.invoke(input);
            if (out instanceof Vec2 vec) return vec;
        } catch (Throwable ignored) {
        }
        try {
            Field f = input.getClass().getField("moveVector");
            Object out = f.get(input);
            if (out instanceof Vec2 vec) return vec;
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : input.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!Vec2.class.isAssignableFrom(f.getType())) continue;
                String name = f.getName().toLowerCase();
                if (!name.contains("move")) continue;
                f.setAccessible(true);
                Object out = f.get(input);
                if (out instanceof Vec2 vec) return vec;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static void setMoveVector(Object input, Vec2 value) {
        if (input == null || value == null) return;
        try {
            Field f = input.getClass().getField("moveVector");
            f.set(input, value);
            return;
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : input.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!Vec2.class.isAssignableFrom(f.getType())) continue;
                String name = f.getName().toLowerCase();
                if (!name.contains("move")) continue;
                f.setAccessible(true);
                f.set(input, value);
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : input.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!Vec2.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("move") && !name.contains("vector")) continue;
                m.invoke(input, value);
                return;
            }
        } catch (Throwable ignored) {
        }
    }
}
