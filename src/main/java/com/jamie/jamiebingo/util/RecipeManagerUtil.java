package com.jamie.jamiebingo.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Collection;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class RecipeManagerUtil {

    private RecipeManagerUtil() {
    }

    public static RecipeManager getRecipeManager(MinecraftServer server) {
        if (server == null) return null;
        // Try any no-arg method returning RecipeManager
        for (Method m : server.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (m.getReturnType() != RecipeManager.class) continue;
            try {
                return (RecipeManager) m.invoke(server);
            } catch (ReflectiveOperationException ignored) {
            }
        }

        // Try fields
        for (Field f : server.getClass().getDeclaredFields()) {
            if (!RecipeManager.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(server);
                if (v instanceof RecipeManager rm) return rm;
            } catch (IllegalAccessException ignored) {
            }
        }
        return null;
    }

    public static Collection<?> getAllRecipes(RecipeManager manager) {
        if (manager == null) return java.util.List.of();
        // Try any no-arg method returning Collection
        for (Method m : manager.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (!Collection.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object v = m.invoke(manager);
                if (v instanceof Collection<?> c) return c;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return java.util.List.of();
    }
}
