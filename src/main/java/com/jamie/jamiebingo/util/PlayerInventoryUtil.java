package com.jamie.jamiebingo.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class PlayerInventoryUtil {
    private PlayerInventoryUtil() {
    }

    public static Inventory getInventory(Player player) {
        if (player == null) return null;

        // Try method "getInventory" if present.
        try {
            Method m = player.getClass().getMethod("getInventory");
            Object out = m.invoke(player);
            if (out instanceof Inventory inv) return inv;
        } catch (Throwable ignored) {
        }

        // Try any no-arg method returning Inventory.
        for (Method m : player.getClass().getMethods()) {
            if (m.getParameterCount() != 0) continue;
            if (!Inventory.class.isAssignableFrom(m.getReturnType())) continue;
            try {
                Object out = m.invoke(player);
                if (out instanceof Inventory inv) return inv;
            } catch (Throwable ignored) {
            }
        }

        // Try fields of type Inventory.
        for (Field f : player.getClass().getDeclaredFields()) {
            if (!Inventory.class.isAssignableFrom(f.getType())) continue;
            try {
                f.setAccessible(true);
                Object out = f.get(player);
                if (out instanceof Inventory inv) return inv;
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    public static boolean addItem(Player player, ItemStack stack) {
        if (player == null || stack == null) return false;

        if (invokeAdd(player, stack)) return true;

        Inventory inv = getInventory(player);
        return inv != null && invokeAdd(inv, stack);
    }

    private static boolean invokeAdd(Object target, ItemStack stack) {
        if (target == null || stack == null) return false;

        // Try common names first.
        try {
            Method m = target.getClass().getMethod("addItem", ItemStack.class);
            Object out = m.invoke(target, stack);
            return interpretAddResult(out);
        } catch (Throwable ignored) {
        }
        try {
            Method m = target.getClass().getMethod("add", ItemStack.class);
            Object out = m.invoke(target, stack);
            return interpretAddResult(out);
        } catch (Throwable ignored) {
        }

        // Try any 1-arg ItemStack method.
        for (Method m : target.getClass().getMethods()) {
            if (m.getParameterCount() != 1) continue;
            if (!ItemStack.class.isAssignableFrom(m.getParameterTypes()[0])) continue;
            try {
                Object out = m.invoke(target, stack);
                return interpretAddResult(out);
            } catch (Throwable ignored) {
            }
        }

        return false;
    }

    private static boolean interpretAddResult(Object out) {
        if (out == null) return true;
        if (out instanceof Boolean b) return b;
        if (out instanceof ItemStack s) return com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(s);
        return true;
    }
}
