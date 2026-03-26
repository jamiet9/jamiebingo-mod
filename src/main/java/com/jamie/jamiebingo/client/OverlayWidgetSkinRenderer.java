package com.jamie.jamiebingo.client;

import net.minecraft.client.gui.GuiGraphics;

public final class OverlayWidgetSkinRenderer {
    private OverlayWidgetSkinRenderer() {}

    public static void draw(
            GuiGraphics graphics,
            ClientCardLayoutSettings.OverlaySkin skin,
            int x,
            int y,
            int width,
            int height
    ) {
        if (skin == null || skin == ClientCardLayoutSettings.OverlaySkin.DEFAULT) {
            return;
        }

        int pad = 4;
        int left = x - pad;
        int top = y - pad;
        int right = x + width + pad;
        int bottom = y + height + pad;

        switch (skin) {
            case PANEL -> {
                graphics.fill(left, top, right, bottom, 0xAA171A20);
                graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xCC252A33);
                graphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0x99212631);
                graphics.fill(left, top, right, top + 1, 0xFFE5E9F0);
                graphics.fill(left, top, left + 1, bottom, 0xFFE5E9F0);
                graphics.fill(left, bottom - 1, right, bottom, 0xFF3D4655);
                graphics.fill(right - 1, top, right, bottom, 0xFF3D4655);
            }
            case GLASS -> {
                graphics.fill(left, top, right, bottom, 0x66346086);
                graphics.fill(left + 2, top + 2, right - 2, bottom - 2, 0x664C88AF);
                graphics.fill(left + 3, top + 3, right - 3, bottom - 3, 0x44213750);
                graphics.fill(left, top, right, top + 1, 0xAAE0F6FF);
                graphics.fill(left, top, left + 1, bottom, 0x88E0F6FF);
                graphics.fill(left, bottom - 1, right, bottom, 0x66304A66);
                graphics.fill(right - 1, top, right, bottom, 0x66304A66);
            }
            case TERMINAL -> {
                graphics.fill(left, top, right, bottom, 0xCC061108);
                graphics.fill(left + 1, top + 1, right - 1, bottom - 1, 0xDD0A1C0E);
                graphics.fill(left + 3, top + 3, right - 3, bottom - 3, 0xAA031006);
                graphics.fill(left, top, right, top + 1, 0xFF85FFAA);
                graphics.fill(left, bottom - 1, right, bottom, 0xFF2C8C47);
                graphics.fill(left, top, left + 1, bottom, 0xFF85FFAA);
                graphics.fill(right - 1, top, right, bottom, 0xFF2C8C47);
            }
            default -> {}
        }
    }
}
