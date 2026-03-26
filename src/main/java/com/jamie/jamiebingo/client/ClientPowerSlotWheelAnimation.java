package com.jamie.jamiebingo.client;

import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;

public final class ClientPowerSlotWheelAnimation {
    private static final Identifier WHEEL_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/power_wheel.png");
    private static final int SPIN_TOTAL_TICKS = 60;
    private static final int MESSAGE_TOTAL_TICKS = 60;

    private static int spinTicks = 0;
    private static int messageTicks = 0;
    private static boolean buffResult = false;
    private static float startAngleDeg = 0.0f;
    private static float endAngleDeg = 0.0f;
    private static float currentAngleDeg = 0.0f;
    private static int spinSoundCooldown = 0;
    private static long lastGameTick = -1L;

    private ClientPowerSlotWheelAnimation() {
    }

    public static void start(boolean buff) {
        spinTicks = SPIN_TOTAL_TICKS;
        messageTicks = MESSAGE_TOTAL_TICKS;
        buffResult = buff;
        startAngleDeg = (float) (Math.random() * 360.0);
        float targetBase = buff ? 0.0f : 180.0f;
        float targetOffset = ((float) Math.random() * 80.0f) - 40.0f;
        int fullTurns = 5 + (int) (Math.random() * 3.0);
        endAngleDeg = targetBase + targetOffset + 360.0f * fullTurns;
        currentAngleDeg = startAngleDeg;
        spinSoundCooldown = 0;
        lastGameTick = -1L;
    }

    public static void clear() {
        spinTicks = 0;
        messageTicks = 0;
        startAngleDeg = 0.0f;
        endAngleDeg = 0.0f;
        currentAngleDeg = 0.0f;
        spinSoundCooldown = 0;
        lastGameTick = -1L;
    }

    public static void tick() {
        long now = currentGameTick();
        if (now < 0) return;
        int deltaTicks;
        if (lastGameTick < 0) {
            deltaTicks = 1;
        } else {
            deltaTicks = (int) Math.max(0L, Math.min(40L, now - lastGameTick));
        }
        lastGameTick = now;
        if (deltaTicks <= 0) return;

        if (spinTicks > 0) {
            int nextSpinTicks = Math.max(0, spinTicks - deltaTicks);
            int elapsed = SPIN_TOTAL_TICKS - nextSpinTicks;
            float t = Math.max(0.0f, Math.min(1.0f, elapsed / (float) SPIN_TOTAL_TICKS));
            float eased = 1.0f - (float) Math.pow(1.0f - t, 3.0f);
            currentAngleDeg = startAngleDeg + (endAngleDeg - startAngleDeg) * eased;

            spinSoundCooldown -= deltaTicks;
            if (spinSoundCooldown <= 0 && nextSpinTicks > 0) {
                var player = ClientMinecraftUtil.getPlayer();
                if (player != null) {
                    float pitch = 0.8f + 0.6f * t;
                    player.playSound(SoundEvents.NOTE_BLOCK_HAT.value(), 0.28f, pitch);
                }
                spinSoundCooldown = 5;
            }
            spinTicks = nextSpinTicks;
            if (spinTicks <= 0) {
                currentAngleDeg = endAngleDeg;
            }
        }
        if (messageTicks > 0) {
            messageTicks = Math.max(0, messageTicks - deltaTicks);
        }
    }

    public static void render(GuiGraphics graphics, int width, int topY) {
        if (graphics == null || messageTicks <= 0) return;
        var font = ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());
        if (font == null) return;

        int size = 96;
        int cx = width / 2;
        int wheelY = topY + 8;

        // Spin the wheel texture around its center.
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        pose.pushMatrix();
        pose.translate(cx, wheelY + size / 2.0f);
        pose.rotate((float) Math.toRadians(currentAngleDeg));
        pose.translate(-size / 2.0f, -size / 2.0f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, WHEEL_TEXTURE, 0, 0, 0, 0, size, size, size, size);
        pose.popMatrix();

        // Fixed pointer at top-center, with slight vibration while spinning.
        int jitterX = spinTicks > 0 ? (int) Math.round(Math.sin((SPIN_TOTAL_TICKS - spinTicks) * 1.2f) * 1.8f) : 0;
        int pointerColor = 0xFFFFFFFF;
        int tipY = wheelY + 14;
        for (int i = 0; i < 12; i++) {
            int y = tipY - i;
            int half = Math.max(0, i / 2);
            graphics.fill(cx - half + jitterX, y, cx + half + 1 + jitterX, y + 1, pointerColor);
        }

        String wheelText = spinTicks > 0 ? "power wheel spinning..." : (buffResult ? "triggered: buff" : "triggered: sabotage");
        int wheelColor = spinTicks > 0 ? 0xFFFFFFAA : (buffResult ? 0xFF66FF66 : 0xFFFF6666);
        drawScaledCentered(graphics, font, wheelText, cx, topY, wheelColor, 0.65f);

        if (spinTicks <= 0) {
            String resultText = buffResult ? "a buff was triggered!" : "a sabotage was triggered!";
            drawScaledCentered(graphics, font, resultText, cx, wheelY + size + 6, 0xFFFFFFFF, 0.65f);
        }
    }

    private static void drawScaledCentered(
            GuiGraphics graphics,
            net.minecraft.client.gui.Font font,
            String text,
            int centerX,
            int y,
            int color,
            float scale
    ) {
        if (graphics == null || font == null || text == null) return;
        float s = Math.max(0.4f, Math.min(1.0f, scale));
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        int textWidth = font.width(text);
        int drawX = Math.round((centerX - (textWidth * s) / 2.0f) / s);
        int drawY = Math.round(y / s);
        pose.pushMatrix();
        pose.scale(s, s);
        graphics.drawString(font, text, drawX, drawY, color, true);
        pose.popMatrix();
    }

    private static long currentGameTick() {
        var mc = ClientMinecraftUtil.getMinecraft();
        var level = ClientMinecraftUtil.getLevel(mc);
        if (level == null) return -1L;
        return level.getGameTime();
    }
}
