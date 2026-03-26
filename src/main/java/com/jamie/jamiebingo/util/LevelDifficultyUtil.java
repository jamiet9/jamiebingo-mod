package com.jamie.jamiebingo.util;

import net.minecraft.world.Difficulty;
import net.minecraft.world.level.Level;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class LevelDifficultyUtil {
    private LevelDifficultyUtil() {
    }

    public static Difficulty getDifficulty(Level level) {
        if (level == null) return null;
        try {
            Method m = level.getClass().getMethod("getDifficulty");
            Object out = m.invoke(level);
            if (out instanceof Difficulty d) return d;
        } catch (Throwable ignored) {
        }
        try {
            Method m = level.getClass().getMethod("difficulty");
            Object out = m.invoke(level);
            if (out instanceof Difficulty d) return d;
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : level.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                if (!Difficulty.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object out = f.get(level);
                if (out instanceof Difficulty d) return d;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
