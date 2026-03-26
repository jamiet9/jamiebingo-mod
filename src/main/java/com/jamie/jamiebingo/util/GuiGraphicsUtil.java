package com.jamie.jamiebingo.util;

import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2fStack;

import java.lang.reflect.Method;

public final class GuiGraphicsUtil {
    private GuiGraphicsUtil() {
    }

    public static int getGuiWidth(GuiGraphics graphics) {
        if (graphics == null) return 0;
        Integer out = invokeInt(graphics, "guiWidth");
        if (out != null) return out;
        Object window = com.jamie.jamiebingo.client.ClientMinecraftUtil.getWindow(
                com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft()
        );
        int fallback = WindowUtil.getGuiScaledWidth(window);
        return fallback > 0 ? fallback : 0;
    }

    public static int getGuiHeight(GuiGraphics graphics) {
        if (graphics == null) return 0;
        Integer out = invokeInt(graphics, "guiHeight");
        if (out != null) return out;
        Object window = com.jamie.jamiebingo.client.ClientMinecraftUtil.getWindow(
                com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft()
        );
        int fallback = WindowUtil.getGuiScaledHeight(window);
        return fallback > 0 ? fallback : 0;
    }

    public static Matrix3x2fStack getPose(GuiGraphics graphics) {
        if (graphics == null) return new Matrix3x2fStack(4);
        try {
            for (Method m : graphics.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!Matrix3x2fStack.class.isAssignableFrom(m.getReturnType())) continue;
                Object out = m.invoke(graphics);
                if (out instanceof Matrix3x2fStack stack) return stack;
            }
        } catch (Throwable ignored) {
        }
        return new Matrix3x2fStack(4);
    }

    public static void renderItem(GuiGraphics graphics, net.minecraft.world.item.ItemStack stack, int x, int y) {
        if (graphics == null || stack == null) return;
        try {
            Method m = graphics.getClass().getMethod(
                    "renderItem",
                    net.minecraft.world.item.ItemStack.class,
                    int.class,
                    int.class
            );
            m.invoke(graphics, stack, x, y);
            return;
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : graphics.getClass().getMethods()) {
                if (m.getParameterCount() != 3) continue;
                Class<?>[] types = m.getParameterTypes();
                if (types[0] != net.minecraft.world.item.ItemStack.class
                        || types[1] != int.class
                        || types[2] != int.class) {
                    continue;
                }
                if (!m.getName().toLowerCase().contains("render")) continue;
                m.invoke(graphics, stack, x, y);
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    private static Integer invokeInt(GuiGraphics graphics, String nameContains) {
        try {
            for (Method m : graphics.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (m.getReturnType() != int.class && m.getReturnType() != Integer.class) continue;
                String name = m.getName().toLowerCase();
                if (!name.contains(nameContains.toLowerCase())) continue;
                Object out = m.invoke(graphics);
                if (out instanceof Number n) return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
