package com.jamie.jamiebingo.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class PlayerFoodUtil {
    private PlayerFoodUtil() {
    }

    public static FoodData getFoodData(Player player) {
        if (player == null) return null;
        try {
            Method m = player.getClass().getMethod("getFoodData");
            Object out = m.invoke(player);
            if (out instanceof FoodData fd) return fd;
        } catch (Throwable ignored) {
        }
        try {
            Method m = player.getClass().getMethod("getFoodStats");
            Object out = m.invoke(player);
            if (out instanceof FoodData fd) return fd;
        } catch (Throwable ignored) {
        }
        try {
            for (Field f : player.getClass().getDeclaredFields()) {
                if (!FoodData.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object out = f.get(player);
                if (out instanceof FoodData fd) return fd;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
