package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.menu.TeamSelectMenu;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
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
    protected void init() {
        super.init();
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // Vanilla container background is already handled; no custom rendering yet.
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (mouseButton == 0
                && slot != null
            && slotId >= 0
            && slotId < 16
            && this.minecraft != null) {
            com.jamie.jamiebingo.client.ClientTeamPreferenceSettings.setPreferredTeamColorId(slotId);
            var mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            var sound = com.jamie.jamiebingo.client.ClientMinecraftUtil.getSoundManager(mc);
            if (sound != null) {
                com.jamie.jamiebingo.util.SoundManagerUtil.play(
                        sound,
                        SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.2f, 1.0f)
                );
            }
        }
        super.slotClicked(slot, slotId, mouseButton, clickType);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isNew) {
        return super.mouseClicked(event, isNew);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }
}
