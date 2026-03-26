package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.util.WindowUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;

import java.lang.reflect.Method;

final class FixedGuiScaleUtil {
    private static final int BASE_VIRTUAL_WIDTH = 640;
    private static final int BASE_VIRTUAL_HEIGHT = 360;

    private FixedGuiScaleUtil() {
    }

    static int virtualWidth(Minecraft mc, int fallbackWidth) {
        if (mc == null) return fallbackWidth;
        ViewportTransform t = transform(mc);
        if (!t.valid) return fallbackWidth;
        if (t.scale < 0.9999d) return BASE_VIRTUAL_WIDTH;
        return Math.max(1, t.scaledWidth);
    }

    static int virtualHeight(Minecraft mc, int fallbackHeight) {
        if (mc == null) return fallbackHeight;
        ViewportTransform t = transform(mc);
        if (!t.valid) return fallbackHeight;
        if (t.scale < 0.9999d) return BASE_VIRTUAL_HEIGHT;
        return Math.max(1, t.scaledHeight);
    }

    static float beginScaledRender(GuiGraphics graphics, Minecraft mc) {
        ViewportTransform t = transform(mc);
        float scale = (float) t.scale;
        if (Math.abs(scale - 1.0f) < 0.0001f) {
            return 1.0f;
        }
        var pose = graphics.pose();
        pose.pushMatrix();
        float safeScale = Math.max(0.0001f, scale);
        pose.translate((float) (t.offsetX / safeScale), (float) (t.offsetY / safeScale));
        pose.scale(scale, scale);
        return scale;
    }

    static void endScaledRender(GuiGraphics graphics, float appliedScale) {
        if (Math.abs(appliedScale - 1.0f) < 0.0001f) return;
        graphics.pose().popMatrix();
    }

    static int virtualMouseX(int mouseX, Minecraft mc) {
        ViewportTransform t = transform(mc);
        return (int) Math.floor((mouseX - t.offsetX) / t.scale);
    }

    static int virtualMouseY(int mouseY, Minecraft mc) {
        ViewportTransform t = transform(mc);
        return (int) Math.floor((mouseY - t.offsetY) / t.scale);
    }

    static double virtualMouseX(double mouseX, Minecraft mc) {
        ViewportTransform t = transform(mc);
        return (mouseX - t.offsetX) / t.scale;
    }

    static double virtualMouseY(double mouseY, Minecraft mc) {
        ViewportTransform t = transform(mc);
        return (mouseY - t.offsetY) / t.scale;
    }

    static double virtualDelta(double delta, Minecraft mc) {
        return delta / transform(mc).scale;
    }

    static MouseButtonEvent virtualEvent(MouseButtonEvent event, Minecraft mc) {
        if (event == null) return null;
        ViewportTransform t = transform(mc);
        if (Math.abs(t.scale - 1.0d) < 0.0001d && Math.abs(t.offsetX) < 0.0001d && Math.abs(t.offsetY) < 0.0001d) {
            return event;
        }
        return new MouseButtonEvent(
                (event.x() - t.offsetX) / t.scale,
                (event.y() - t.offsetY) / t.scale,
                new MouseButtonInfo(event.button(), extractModifiers(event))
        );
    }

    private static ViewportTransform transform(Minecraft mc) {
        if (mc == null) return ViewportTransform.invalid();
        Object window = ClientMinecraftUtil.getWindow(mc);
        int scaledWidth = WindowUtil.getGuiScaledWidth(window);
        int scaledHeight = WindowUtil.getGuiScaledHeight(window);
        if (scaledWidth <= 0 || scaledHeight <= 0) {
            return ViewportTransform.invalid();
        }

        double fitScale = Math.min(
                1.0d,
                Math.min(
                        scaledWidth / (double) BASE_VIRTUAL_WIDTH,
                        scaledHeight / (double) BASE_VIRTUAL_HEIGHT
                )
        );
        if (fitScale >= 0.9999d) {
            return new ViewportTransform(true, scaledWidth, scaledHeight, 1.0d, 0.0d, 0.0d);
        }
        double offsetX = (scaledWidth - (BASE_VIRTUAL_WIDTH * fitScale)) * 0.5d;
        double offsetY = (scaledHeight - (BASE_VIRTUAL_HEIGHT * fitScale)) * 0.5d;
        return new ViewportTransform(true, scaledWidth, scaledHeight, fitScale, offsetX, offsetY);
    }

    private static int extractModifiers(MouseButtonEvent event) {
        try {
            Method m = event.getClass().getMethod("modifiers");
            Object out = m.invoke(event);
            if (out instanceof Number n) {
                return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        try {
            Method m = event.getClass().getMethod("getModifiers");
            Object out = m.invoke(event);
            if (out instanceof Number n) {
                return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private record ViewportTransform(
            boolean valid,
            int scaledWidth,
            int scaledHeight,
            double scale,
            double offsetX,
            double offsetY
    ) {
        static ViewportTransform invalid() {
            return new ViewportTransform(false, 0, 0, 1.0d, 0.0d, 0.0d);
        }
    }
}
