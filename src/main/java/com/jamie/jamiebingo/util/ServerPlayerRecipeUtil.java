package com.jamie.jamiebingo.util;

import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.util.Collection;

public final class ServerPlayerRecipeUtil {

    private ServerPlayerRecipeUtil() {
    }

    public static int awardRecipes(ServerPlayer player, Collection<?> recipes) {
        if (player == null || recipes == null) return 0;
        // Use reflection only to avoid hard-linking to obfuscated methods.
        for (Method m : player.getClass().getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (!Collection.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
            if (!"awardRecipes".equals(m.getName()) && !"unlockRecipes".equals(m.getName())) continue;
            try {
                Object res = m.invoke(player, recipes);
                if (res instanceof Integer i) return i;
                if (res instanceof Number n) return n.intValue();
                return 0;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return 0;
    }
}
