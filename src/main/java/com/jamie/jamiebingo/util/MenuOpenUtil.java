package com.jamie.jamiebingo.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

import java.lang.reflect.Method;
import java.util.OptionalInt;

public final class MenuOpenUtil {
    private MenuOpenUtil() {}

    public static void open(ServerPlayer player, MenuProvider provider) {
        if (player == null || provider == null) return;
        // Try direct openMenu(MenuProvider)
        try {
            Method m = player.getClass().getMethod("openMenu", MenuProvider.class);
            Object out = m.invoke(player, provider);
            if (out instanceof OptionalInt) return;
        } catch (Throwable ignored) {
        }
        // Fallback: any method that takes MenuProvider and returns OptionalInt
        for (Method m : player.getClass().getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (!MenuProvider.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
            if (!OptionalInt.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                m.invoke(player, provider);
                return;
            } catch (Throwable ignored) {
            }
        }
    }
}
