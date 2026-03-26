package com.jamie.jamiebingo.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.Locale;

public final class SettingsLineRenderer {
    private SettingsLineRenderer() {
    }

    public static void draw(GuiGraphics graphics, Font font, int x, int y, String line, boolean shadow) {
        if (font == null || line == null) return;
        int colon = line.indexOf(':');
        if (colon < 0) {
            graphics.drawString(font, line, x, y, 0xFFD0D0D0, shadow);
            return;
        }

        String key = line.substring(0, colon + 1);
        String value = line.substring(colon + 1).stripLeading();
        graphics.drawString(font, key, x, y, 0xFF8FD3FF, shadow);
        graphics.drawString(font, value, x + font.width(key + " "), y, valueColor(value), shadow);
    }

    public static int valueColor(String value) {
        if (value == null || value.isBlank()) return 0xFFE8E8E8;
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("on") || normalized.equals("enabled") || normalized.equals("true")) return 0xFF83FF9A;
        if (normalized.equals("off") || normalized.equals("disabled") || normalized.equals("false")) return 0xFFFF8D8D;
        if (normalized.contains("random")) return 0xFFFFE082;
        if (normalized.contains("full") || normalized.contains("lockout") || normalized.contains("line")) return 0xFFFFE082;
        return 0xFFF3F3F3;
    }
}
