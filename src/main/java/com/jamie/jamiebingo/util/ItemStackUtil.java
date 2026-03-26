package com.jamie.jamiebingo.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class ItemStackUtil {
    private static volatile ItemStack EMPTY;

    private ItemStackUtil() {
    }

    public static ItemStack empty() {
        ItemStack cached = EMPTY;
        if (cached != null) return cached;
        synchronized (ItemStackUtil.class) {
            if (EMPTY != null) return EMPTY;
            try {
                for (Field f : ItemStack.class.getDeclaredFields()) {
                    if (!Modifier.isStatic(f.getModifiers())) continue;
                    if (!ItemStack.class.isAssignableFrom(f.getType())) continue;
                    f.setAccessible(true);
                    Object out = f.get(null);
                    if (out instanceof ItemStack stack) {
                        EMPTY = stack;
                        return stack;
                    }
                }
            } catch (Throwable ignored) {
            }
            try {
                for (Method m : ItemStack.class.getDeclaredMethods()) {
                    if (!Modifier.isStatic(m.getModifiers())) continue;
                    if (m.getParameterCount() != 0) continue;
                    if (!ItemStack.class.isAssignableFrom(m.getReturnType())) continue;
                    m.setAccessible(true);
                    Object out = m.invoke(null);
                    if (out instanceof ItemStack stack) {
                        EMPTY = stack;
                        return stack;
                    }
                }
            } catch (Throwable ignored) {
            }
            Item air = findAirItem();
            if (air != null) {
                EMPTY = new ItemStack(air);
                return EMPTY;
            }
            return EMPTY;
        }
    }

    public static boolean isEmpty(ItemStack stack) {
        if (stack == null) return true;
        try {
            Method m = stack.getClass().getMethod("isEmpty");
            Object out = m.invoke(stack);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (m.getReturnType() != boolean.class && m.getReturnType() != Boolean.class) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains("empty")) continue;
                Object out = m.invoke(stack);
                if (out instanceof Boolean b) return b;
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = stack.getClass().getMethod("getCount");
            Object out = m.invoke(stack);
            if (out instanceof Integer i) return i <= 0;
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static Item getItem(ItemStack stack) {
        if (stack == null) return null;
        try {
            return stack.getItem();
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : stack.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!Item.class.isAssignableFrom(m.getReturnType())) continue;
                m.setAccessible(true);
                Object out = m.invoke(stack);
                if (out instanceof Item item) return item;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Item findAirItem() {
        Item fallback = null;
        try {
            for (Field f : Items.class.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) continue;
                if (!Item.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object out = f.get(null);
                if (out instanceof Item item) {
                    if (fallback == null) fallback = item;
                    String s = String.valueOf(item).toLowerCase();
                    if (s.contains("air")) return item;
                }
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }
}
