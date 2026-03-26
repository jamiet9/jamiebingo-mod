package com.jamie.jamiebingo.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class ScreenTooltipUtil {
    private ScreenTooltipUtil() {
    }

    public static void drawTextTooltip(
            GuiGraphics graphics,
            Font font,
            String text,
            int mouseX,
            int mouseY,
            int screenWidth,
            int screenHeight,
            int maxTextWidth
    ) {
        if (text == null || text.isBlank()) return;
        drawComponentTooltip(
                graphics,
                font,
                List.of(com.jamie.jamiebingo.util.ComponentUtil.literal(text)),
                mouseX,
                mouseY,
                screenWidth,
                screenHeight,
                maxTextWidth
        );
    }

    public static void drawComponentTooltip(
            GuiGraphics graphics,
            Font font,
            List<Component> lines,
            int mouseX,
            int mouseY,
            int screenWidth,
            int screenHeight,
            int maxTextWidth
    ) {
        if (graphics == null || font == null || lines == null || lines.isEmpty()) return;

        int wrapWidth = Math.max(80, maxTextWidth);
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) {
            if (line == null) continue;
            wrapped.addAll(font.split(line, wrapWidth));
        }
        if (wrapped.isEmpty()) return;

        int textWidth = 0;
        for (FormattedCharSequence seq : wrapped) {
            textWidth = Math.max(textWidth, font.width(seq));
        }
        int boxWidth = textWidth + 8;
        int lineHeight = font.lineHeight + 1;
        int boxHeight = wrapped.size() * lineHeight + 6;

        int x = mouseX + 10;
        int y = mouseY - boxHeight - 4;
        if (x + boxWidth > screenWidth - 4) x = screenWidth - boxWidth - 4;
        if (x < 4) x = 4;
        if (y < 4) y = mouseY + 10;
        if (y + boxHeight > screenHeight - 4) y = screenHeight - boxHeight - 4;
        if (y < 4) y = 4;

        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xE0101010);
        graphics.fill(x, y, x + boxWidth, y + 1, 0x90FFFFFF);
        graphics.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, 0x90000000);
        graphics.fill(x, y, x + 1, y + boxHeight, 0x90FFFFFF);
        graphics.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, 0x90000000);

        int drawY = y + 3;
        for (FormattedCharSequence seq : wrapped) {
            graphics.drawString(font, seq, x + 4, drawY, 0xFFFFFFFF, false);
            drawY += lineHeight;
        }
    }
}
