package com.jamie.jamiebingo.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

public final class CardSkinDecorRenderer {
    private static final Identifier SLOT_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/gui/sprites/container/slot.png");

    private CardSkinDecorRenderer() {}

    public static boolean usesInventoryChestLayout(ClientCardLayoutSettings.CardSkin skin) {
        return false;
    }

    public static boolean usesCustomSlotFrame(ClientCardLayoutSettings.CardSkin skin) {
        return skin == ClientCardLayoutSettings.CardSkin.BLOOM
                || skin == ClientCardLayoutSettings.CardSkin.RUNIC
                || skin == ClientCardLayoutSettings.CardSkin.CELESTIAL;
    }

    public static void drawBoardBackdrop(
            GuiGraphics graphics,
            ClientCardLayoutSettings.CardSkin skin,
            int x,
            int y,
            int width,
            int height,
            boolean fullscreen
    ) {
        switch (skin) {
            case BLOOM -> drawBloomBoard(graphics, x, y, width, height, fullscreen);
            case RUNIC -> drawRunicBoard(graphics, x, y, width, height, fullscreen);
            case CELESTIAL -> drawCelestialBoard(graphics, x, y, width, height, fullscreen);
            default -> {
                // Intentionally no-op for flat-color skins.
            }
        }
    }

    public static void drawSlotDecoration(
            GuiGraphics graphics,
            ClientCardLayoutSettings.CardSkin skin,
            int x,
            int y,
            int box,
            int col,
            int row,
            int borderThickness,
            int borderColor,
            int slotBg,
            boolean fullscreen
        ) {
        switch (skin) {
            case BLOOM -> drawBloomSlot(graphics, x, y, box, col, row, borderColor, slotBg);
            case RUNIC -> drawRunicSlot(graphics, x, y, box, col, row, borderColor, slotBg);
            case CELESTIAL -> drawCelestialSlot(graphics, x, y, box, col, row, borderColor, slotBg);
            case GLASS -> {
                graphics.fill(x + 1, y + 1, x + 4, y + 2, borderColor);
                graphics.fill(x + 1, y + 1, x + 2, y + 4, borderColor);
                graphics.fill(x + box - 4, y + box - 2, x + box - 1, y + box - 1, borderColor);
                graphics.fill(x + box - 2, y + box - 4, x + box - 1, y + box - 1, borderColor);
            }
            case MINIMAL -> {
                graphics.fill(x + 2, y + 2, x + box - 2, y + 3, borderColor);
                graphics.fill(x + 2, y + box - 3, x + box - 2, y + box - 2, borderColor);
                graphics.fill(x + 2, y + 2, x + 3, y + box - 2, borderColor);
                graphics.fill(x + box - 3, y + 2, x + box - 2, y + box - 2, borderColor);
            }
            case NEON -> {
                int p = Math.max(1, box / 7);
                graphics.fill(x + 1, y + 1, x + 1 + p, y + 1 + p, borderColor);
                graphics.fill(x + box - 1 - p, y + 1, x + box - 1, y + 1 + p, borderColor);
                graphics.fill(x + 1, y + box - 1 - p, x + 1 + p, y + box - 1, borderColor);
                graphics.fill(x + box - 1 - p, y + box - 1 - p, x + box - 1, y + box - 1, borderColor);
            }
            case LAVA -> {
                int n = Math.max(2, box / 6);
                graphics.fill(x + 1, y + 1, x + 1 + n, y + 1 + n, borderColor);
                graphics.fill(x + box - 1 - n, y + 1, x + box - 1, y + 1 + n, borderColor);
                graphics.fill(x + 1, y + box - 1 - n, x + 1 + n, y + box - 1, borderColor);
                graphics.fill(x + box - 1 - n, y + box - 1 - n, x + box - 1, y + box - 1, borderColor);
                graphics.fill(x + box / 2 - 1, y + 1, x + box / 2 + 1, y + 3, borderColor);
                graphics.fill(x + box / 2 - 1, y + box - 3, x + box / 2 + 1, y + box - 1, borderColor);
            }
            case CANDY -> {
                int step = Math.max(3, box / 5);
                for (int i = 0; i < box; i += step) {
                    graphics.fill(x + i, y + 1, x + Math.min(box, i + 1), y + 3, borderColor);
                    graphics.fill(x + i, y + box - 3, x + Math.min(box, i + 1), y + box - 1, borderColor);
                }
            }
            case TERMINAL -> {
                int step = Math.max(3, box / 4);
                for (int yy = y + 2; yy < y + box - 2; yy += step) {
                    graphics.fill(x + 1, yy, x + 3, yy + 1, borderColor);
                    graphics.fill(x + box - 3, yy, x + box - 1, yy + 1, borderColor);
                }
                graphics.fill(x + box / 2, y + 1, x + box / 2 + 1, y + 3, borderColor);
                graphics.fill(x + box / 2, y + box - 3, x + box / 2 + 1, y + box - 1, borderColor);
            }
            case ROYAL -> {
                graphics.fill(x + 2, y + 2, x + box - 2, y + 3, borderColor);
                graphics.fill(x + 2, y + box - 3, x + box - 2, y + box - 2, borderColor);
                int cx = x + box / 2;
                int cy = y + box / 2;
                graphics.fill(cx - 1, cy - 1, cx + 1, cy + 1, borderColor);
                graphics.fill(x + 1, y + 1, x + 3, y + 3, borderColor);
                graphics.fill(x + box - 3, y + 1, x + box - 1, y + 3, borderColor);
                graphics.fill(x + 1, y + box - 3, x + 3, y + box - 1, borderColor);
                graphics.fill(x + box - 3, y + box - 3, x + box - 1, y + box - 1, borderColor);
            }
            case VOID -> {
                graphics.fill(x + 1, y + 1, x + box - 1, y + 2, borderColor);
                int cx = x + box / 2;
                int cy = y + box / 2;
                graphics.fill(cx - 2, cy - 2, cx + 2, cy + 2, borderColor);
            }
            case PRISM -> {
                int mid = y + box / 2;
                graphics.fill(x + 2, mid - 2, x + box - 2, mid - 1, borderColor);
                graphics.fill(x + 2, mid + 1, x + box - 2, mid + 2, borderColor);
                graphics.fill(x + box / 2 - 1, y + 2, x + box / 2 + 1, y + box - 2, borderColor);
            }
            case TOXIC -> {
                graphics.fill(x + 2, y + box - 3, x + box - 2, y + box - 2, borderColor);
                graphics.fill(x + 2, y + 2, x + 3, y + 3, borderColor);
                graphics.fill(x + box - 3, y + 3, x + box - 2, y + 4, borderColor);
                graphics.fill(x + box / 2 - 1, y + box - 5, x + box / 2 + 1, y + box - 2, borderColor);
            }
            case ICE -> {
                graphics.fill(x + 1, y + 1, x + 4, y + 2, borderColor);
                graphics.fill(x + 1, y + 1, x + 2, y + 4, borderColor);
                graphics.fill(x + box - 4, y + box - 2, x + box - 1, y + box - 1, borderColor);
                graphics.fill(x + box - 2, y + box - 4, x + box - 1, y + box - 1, borderColor);
            }
            case SUNSET -> {
                int horizon = y + (box * 2) / 3;
                graphics.fill(x + 2, horizon, x + box - 2, horizon + 1, borderColor);
                graphics.fill(x + box / 2 - 2, horizon - 3, x + box / 2 + 2, horizon - 1, borderColor);
            }
            case GLITCH -> {
                graphics.fill(x + 2, y + 3, x + box - 4, y + 4, borderColor);
                graphics.fill(x + 4, y + box - 5, x + box - 2, y + box - 4, borderColor);
                graphics.fill(x + 1, y + box / 2, x + box - 1, y + box / 2 + 1, borderColor);
            }
            case CHROME -> {
                int sheenX = x + box / 2 - 1;
                graphics.fill(sheenX, y + 2, sheenX + 2, y + box - 2, borderColor);
                graphics.fill(x + 2, y + box - 3, x + box - 2, y + box - 2, borderColor);
                graphics.fill(x + 2, y + 2, x + box - 2, y + 3, borderColor);
            }
            default -> {}
        }
    }

    public static void renderScaledItem(GuiGraphics graphics, net.minecraft.world.item.ItemStack stack, int x, int y, int boxSize) {
        if (stack == null || stack.isEmpty()) return;
        int integerScale = Math.max(1, boxSize / 16);
        float scale = (float) integerScale;
        int pixelSize = 16 * integerScale;
        int offsetX = x + Math.max(0, (boxSize - pixelSize) / 2);
        int offsetY = y + Math.max(0, (boxSize - pixelSize) / 2);
        int drawX = Math.round(offsetX / scale);
        int drawY = Math.round(offsetY / scale);
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        pose.pushMatrix();
        pose.scale(scale, scale);
        graphics.renderItem(stack, drawX, drawY);
        pose.popMatrix();
    }

    private static void drawBloomBoard(GuiGraphics graphics, int x, int y, int width, int height, boolean fullscreen) {
        int panelX = x - 10;
        int panelY = y - 16;
        int panelW = width + 20;
        int panelH = height + (fullscreen ? 24 : 20);

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF2A1E26);
        graphics.fill(panelX + 3, panelY + 3, panelX + panelW - 3, panelY + panelH - 3, 0xFF4A3141);
        graphics.fill(panelX + 7, panelY + 7, panelX + panelW - 7, panelY + panelH - 7, 0xFF6A465C);
        graphics.fill(panelX + 10, panelY + 10, panelX + panelW - 10, panelY + panelH - 10, 0xFF7A5268);
        drawVineEdge(graphics, panelX, panelY, panelW, panelH, 0xFFB4F2C8, 0xFF5D9F78);
    }

    private static void drawRunicBoard(GuiGraphics graphics, int x, int y, int width, int height, boolean fullscreen) {
        int panelX = x - 10;
        int panelY = y - 14;
        int panelW = width + 20;
        int panelH = height + (fullscreen ? 22 : 18);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF1E2331);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + panelH - 2, 0xFF394056);
        graphics.fill(panelX + 5, panelY + 5, panelX + panelW - 5, panelY + panelH - 5, 0xFF202638);
        graphics.fill(panelX + 9, panelY + 9, panelX + panelW - 9, panelY + panelH - 9, 0xFF2B3246);
        drawRunicEdge(graphics, panelX, panelY, panelW, panelH, 0xFF99D8FF, 0xFF5279B8);
    }

    private static void drawCelestialBoard(GuiGraphics graphics, int x, int y, int width, int height, boolean fullscreen) {
        int panelX = x - 12;
        int panelY = y - 16;
        int panelW = width + 24;
        int panelH = height + (fullscreen ? 24 : 20);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFF201729);
        graphics.fill(panelX + 3, panelY + 3, panelX + panelW - 3, panelY + panelH - 3, 0xFF4A3758);
        graphics.fill(panelX + 6, panelY + 6, panelX + panelW - 6, panelY + panelH - 6, 0xFF2A2237);
        graphics.fill(panelX + 10, panelY + 10, panelX + panelW - 10, panelY + panelH - 10, 0xFF372B46);
        drawRunicEdge(graphics, panelX, panelY, panelW, panelH, 0xFFE8D6FF, 0xFF8B6BBC);
    }

    private static void drawVineEdge(GuiGraphics graphics, int x, int y, int w, int h, int c1, int c2) {
        for (int i = 0; i < 6; i++) {
            int offset = i * 2;
            int color = (i % 2 == 0) ? c1 : c2;
            graphics.fill(x + offset, y + offset, x + w - offset, y + offset + 1, color);
            graphics.fill(x + offset, y + h - offset - 1, x + w - offset, y + h - offset, color);
            graphics.fill(x + offset, y + offset, x + offset + 1, y + h - offset, color);
            graphics.fill(x + w - offset - 1, y + offset, x + w - offset, y + h - offset, color);
        }
        for (int yy = y + 24; yy < y + h - 24; yy += 18) {
            graphics.fill(x + 2, yy, x + 7, yy + 3, c1);
            graphics.fill(x + w - 7, yy, x + w - 2, yy + 3, c1);
            graphics.fill(x + 4, yy + 4, x + 9, yy + 7, c2);
            graphics.fill(x + w - 9, yy + 4, x + w - 4, yy + 7, c2);
        }
    }

    private static void drawRunicEdge(GuiGraphics graphics, int x, int y, int w, int h, int c1, int c2) {
        graphics.fill(x + 8, y + 2, x + w - 8, y + 4, c1);
        graphics.fill(x + 8, y + h - 4, x + w - 8, y + h - 2, c1);
        graphics.fill(x + 2, y + 8, x + 4, y + h - 8, c1);
        graphics.fill(x + w - 4, y + 8, x + w - 2, y + h - 8, c1);
        for (int i = 0; i < 5; i++) {
            int inset = 4 + i * 2;
            int color = (i % 2 == 0) ? c2 : c1;
            graphics.fill(x + inset, y + inset, x + w - inset, y + inset + 1, color);
            graphics.fill(x + inset, y + h - inset - 1, x + w - inset, y + h - inset, color);
        }
        int midY = y + h / 2;
        graphics.fill(x + 3, midY - 1, x + 8, midY + 1, c2);
        graphics.fill(x + w - 8, midY - 1, x + w - 3, midY + 1, c2);
    }

    private static void drawBloomSlot(GuiGraphics graphics, int x, int y, int box, int col, int row, int borderColor, int slotBg) {
        int outer = ((col + row) & 1) == 0 ? 0xFFE8B8D4 : 0xFFD89FBE;
        int inner = ((col + row) & 1) == 0 ? 0xFF5E4151 : 0xFF503747;
        graphics.fill(x, y, x + box, y + box, outer);
        graphics.fill(x, y, x + box, y + 2, borderColor);
        graphics.fill(x, y + box - 2, x + box, y + box, borderColor);
        graphics.fill(x, y, x + 2, y + box, borderColor);
        graphics.fill(x + box - 2, y, x + box, y + box, borderColor);
        graphics.fill(x + 2, y + 2, x + box - 2, y + box - 2, 0xFF2C1F28);
        graphics.fill(x + 3, y + 3, x + box - 3, y + box - 3, slotBg);
        graphics.fill(x + 4, y + 4, x + box - 4, y + box - 4, inner & 0x66FFFFFF);
        graphics.fill(x + 4, y + 4, x + box - 4, y + 6, 0x77FFD9EB);
        graphics.fill(x + 4, y + box - 6, x + box - 4, y + box - 4, 0x77533544);
    }

    private static void drawRunicSlot(GuiGraphics graphics, int x, int y, int box, int col, int row, int borderColor, int slotBg) {
        int outer = ((col + row) & 1) == 0 ? 0xFFB7D6F9 : 0xFF94B8E8;
        int inner = ((col + row) & 1) == 0 ? 0xFF283145 : 0xFF1F273A;
        graphics.fill(x, y, x + box, y + box, outer);
        graphics.fill(x, y, x + box, y + 2, borderColor);
        graphics.fill(x, y + box - 2, x + box, y + box, borderColor);
        graphics.fill(x, y, x + 2, y + box, borderColor);
        graphics.fill(x + box - 2, y, x + box, y + box, borderColor);
        graphics.fill(x + 2, y + 2, x + box - 2, y + box - 2, 0xFF101722);
        graphics.fill(x + 3, y + 3, x + box - 3, y + box - 3, slotBg);
        graphics.fill(x + 4, y + 4, x + box - 4, y + box - 4, inner & 0x66FFFFFF);
        int midX = x + box / 2;
        graphics.fill(midX - 1, y + 4, midX + 1, y + box - 4, 0x77DDF1FF);
        graphics.fill(x + 4, y + box / 2 - 1, x + box - 4, y + box / 2 + 1, 0x77517DC2);
    }

    private static void drawCelestialSlot(GuiGraphics graphics, int x, int y, int box, int col, int row, int borderColor, int slotBg) {
        int outer = ((col + row) & 1) == 0 ? 0xFFE1CAFF : 0xFFC6A8F1;
        int inner = ((col + row) & 1) == 0 ? 0xFF392B49 : 0xFF30213F;
        graphics.fill(x, y, x + box, y + box, outer);
        graphics.fill(x, y, x + box, y + 2, borderColor);
        graphics.fill(x, y + box - 2, x + box, y + box, borderColor);
        graphics.fill(x, y, x + 2, y + box, borderColor);
        graphics.fill(x + box - 2, y, x + box, y + box, borderColor);
        graphics.fill(x + 2, y + 2, x + box - 2, y + box - 2, 0xFF201827);
        graphics.fill(x + 3, y + 3, x + box - 3, y + box - 3, slotBg);
        graphics.fill(x + 4, y + 4, x + box - 4, y + box - 4, inner & 0x66FFFFFF);
        graphics.fill(x + 4, y + 4, x + box - 4, y + 5, 0x88FFF7D1);
        graphics.fill(x + 4, y + box - 5, x + box - 4, y + box - 4, 0x88614B7B);
        graphics.fill(x + box / 2 - 1, y + box / 2 - 1, x + box / 2 + 1, y + box / 2 + 1, 0xAAFFF1A8);
    }
}
