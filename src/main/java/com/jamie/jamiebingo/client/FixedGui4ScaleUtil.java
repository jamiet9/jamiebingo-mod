package com.jamie.jamiebingo.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;

final class FixedGui4ScaleUtil {
    private FixedGui4ScaleUtil() {
    }

    static int virtualWidth(int currentGuiWidth) {
        return currentGuiWidth;
    }

    static int virtualHeight(int currentGuiHeight) {
        return currentGuiHeight;
    }

    static float beginScaledRender(GuiGraphics graphics, Minecraft mc) {
        return 1.0f;
    }

    static void endScaledRender(GuiGraphics graphics, float appliedScale) {
        // no-op
    }

    static int virtualMouseX(int mouseX, Minecraft mc) {
        return mouseX;
    }

    static int virtualMouseY(int mouseY, Minecraft mc) {
        return mouseY;
    }

    static MouseButtonEvent virtualEvent(MouseButtonEvent event, Minecraft mc) {
        return event;
    }
}
