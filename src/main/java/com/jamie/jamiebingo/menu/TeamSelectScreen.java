package com.jamie.jamiebingo.menu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Team selection screen.
 * Uses standard container screen rendering for this MC version.
 */
public class TeamSelectScreen extends AbstractContainerScreen<TeamSelectMenu> {

    public TeamSelectScreen(TeamSelectMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Vanilla container background is already handled; no custom rendering yet.
    }
}
