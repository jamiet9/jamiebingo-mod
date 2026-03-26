package com.jamie.jamiebingo.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ClientKeybinds {
    public static final KeyMapping.Category CATEGORY =
            KeyCategoryUtil.getOrCreate("jamiebingo:jamiebingo");

    public static final KeyMapping TOGGLE_FULLSCREEN_CARD = new KeyMapping(
            "key.jamiebingo.fullscreen_card",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_CARD_OVERLAY = new KeyMapping(
            "key.jamiebingo.card_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_SETTINGS_OVERLAY = new KeyMapping(
            "key.jamiebingo.settings_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            CATEGORY
    );

    public static final KeyMapping TOGGLE_SCOREBOARD_OVERLAY = new KeyMapping(
            "key.jamiebingo.scoreboard_overlay",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    private ClientKeybinds() {
    }
}

