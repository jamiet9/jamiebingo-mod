package com.jamie.jamiebingo.client;


import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.bingo.BingoLineType;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.DyeColor;

public class ClientWinningLineData {

    /* =========================
       STATE
       ========================= */

    public static boolean active = false;
    public static BingoLineType type = null;
    public static int index = -1;
    public static DyeColor color = null;

    // Animation
    public static float progress = 0f;   // 0 → 1
    private static int cardSize = 5;

    // 🔊 Sound guard (important)
    private static boolean playedSound = false;

    private ClientWinningLineData() {}

    /* =========================
       LIFECYCLE
       ========================= */

    public static void set(BingoLineType t, int i, DyeColor c, int size) {
        active = true;
        type = t;
        index = i;
        color = c;
        cardSize = size;

        progress = 0f;
        playedSound = false; // reset sound lock
    }

    public static void clear() {
        active = false;
        type = null;
        index = -1;
        color = null;
        progress = 0f;
        cardSize = 5;
        playedSound = false;
    }

    /* =========================
       ANIMATION + SOUND
       ========================= */

    /**
     * Call once per frame from renderer.
     * @param speed Typical values: 0.05f – 0.12f
     */
    public static void tickAnimation(float speed) {
        if (!active) return;

        // 🔊 Play sound ONCE at start
        if (!playedSound && ClientMinecraftUtil.getPlayer() != null) {
            ClientMinecraftUtil.getPlayer().playSound(
                    SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    1.1f,
                    1.0f
            );
            playedSound = true;
        }

        progress += speed;
        if (progress > 1f) {
            progress = 1f;
        }
    }

    /* =========================
       VISUAL HELPERS
       ========================= */

    /**
     * Thickness scales with board density.
     * Smaller boards → thicker, more dramatic line.
     */
    public static int getLineThickness(int boxSize) {
        int base = Math.max(3, boxSize / 6);

        // Slight boost for small boards (2x2, 3x3)
        if (cardSize <= 3) {
            base += 2;
        }

        return base;
    }
}

