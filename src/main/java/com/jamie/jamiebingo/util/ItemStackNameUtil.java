package com.jamie.jamiebingo.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;

public final class ItemStackNameUtil {
    private ItemStackNameUtil() {
    }

    public static String getHoverNameString(ItemStack stack) {
        Component name = getHoverName(stack);
        return name == null ? null : name.getString();
    }

    public static Component getHoverName(ItemStack stack) {
        if (stack == null) return null;
        try {
            Method m = stack.getClass().getMethod("getHoverName");
            Object out = m.invoke(stack);
            if (out instanceof Component c) return c;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!Component.class.isAssignableFrom(m.getReturnType())) continue;
                Object out = m.invoke(stack);
                if (out instanceof Component c) return c;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!Component.class.isAssignableFrom(m.getReturnType())) continue;
                m.setAccessible(true);
                Object out = m.invoke(stack);
                if (out instanceof Component c) return c;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
