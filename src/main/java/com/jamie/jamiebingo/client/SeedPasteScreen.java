package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketSeedPasteFinish;
import com.jamie.jamiebingo.network.packet.PacketSeedPastePart;
import com.jamie.jamiebingo.network.packet.PacketSeedPreviewRequest;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public class SeedPasteScreen extends Screen {

    private static final int CHUNK_SIZE = 800;
    private static final Identifier HIDDEN_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/slot_hidden.png");
    private String seed = "";

    public SeedPasteScreen() {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Paste Bingo Seed"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 2 - 60;

        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Paste from clipboard"),
                        b -> pasteFromClipboard()
                )
                .pos(cx - 80, y)
                .size(160, 20)
                .build());

        y += 26;
        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Start from seed"),
                        b -> sendSeed()
                )
                .pos(cx - 80, y)
                .size(160, 20)
                .build());

        y += 26;
        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Cancel"),
                        b -> onClose()
                )
                .pos(cx - 80, y)
                .size(160, 20)
                .build());
    }

    private void pasteFromClipboard() {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null) return;
        var keyboard = ClientMinecraftUtil.getKeyboardHandler(mc);
        if (keyboard == null) return;
        String raw = keyboard.getClipboard();
        if (raw == null) raw = "";
        raw = raw.trim();
        if (raw.startsWith("\"") && raw.endsWith("\"") && raw.length() > 1) {
            raw = raw.substring(1, raw.length() - 1);
        }
        seed = raw;
        if (!seed.isBlank()) {
            NetworkHandler.sendToServer(new PacketSeedPreviewRequest(seed));
        } else {
            SeedPreviewState.clear();
        }
    }

    private void sendSeed() {
        if (seed == null || seed.isBlank()) {
            return;
        }
        int total = (int) Math.ceil(seed.length() / (double) CHUNK_SIZE);
        for (int i = 0; i < total; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(seed.length(), start + CHUNK_SIZE);
            String chunk = seed.substring(start, end);
            NetworkHandler.sendToServer(
                    new PacketSeedPastePart(i + 1, total, chunk)
            );
        }
        NetworkHandler.sendToServer(new PacketSeedPasteFinish());
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBlurredBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);

        renderSettingsPreview(graphics);
        renderCardPreview(graphics);

        String preview = seed == null || seed.isBlank() ? "(No seed pasted)" : seed;
        int maxLen = Math.min(120, preview.length());
        String line = preview.substring(0, maxLen) + (preview.length() > maxLen ? "..." : "");

        int textY = height / 2 + 40;
        var font = ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());
        if (font != null) {
            graphics.drawCenteredString(font, "Preview: " + line, width / 2, textY, 0xFFCCCCCC);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Avoid double-blur from Screen.render calling renderBackground again.
    }

    private void renderSettingsPreview(GuiGraphics graphics) {
        if (!SeedPreviewState.hasPreview()) return;
        int x = 20;
        int y = height / 2 - 80;
        int lineHeight = minecraft.font.lineHeight + 2;
        for (String line : SeedPreviewState.getSettings()) {
            graphics.drawString(minecraft.font, line, x, y, 0xFFCCCCCC, true);
            y += lineHeight;
        }
        if (!SeedPreviewState.getError().isBlank()) {
            graphics.drawString(minecraft.font, SeedPreviewState.getError(), x, y + 6, 0xFFFF5555, true);
        }
    }

    private void renderCardPreview(GuiGraphics graphics) {
        if (!SeedPreviewState.hasPreview()) return;
        BingoCard card = SeedPreviewState.getCard();
        if (card == null) return;

        int size = card.getSize();
        int padding = 16;
        int maxWidth = width / 3;
        int maxHeight = height / 3;
        int spacing = Math.max(2, 8 - size);
        int boxSize = Math.max(12, Math.min(
                (maxWidth - spacing * (size - 1)) / size,
                (maxHeight - spacing * (size - 1)) / size
        ));

        int totalWidth = boxSize * size + spacing * (size - 1);
        int totalHeight = boxSize * size + spacing * (size - 1);

        int startX = width - padding - totalWidth;
        int startY = padding;

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int bx = startX + c * (boxSize + spacing);
                int by = startY + r * (boxSize + spacing);
                BingoSlot slot = card.getSlot(c, r);

                graphics.fill(bx, by, bx + boxSize, by + boxSize, 0xAA000000);
                graphics.fill(bx, by, bx + boxSize, by + 1, 0x66FFFFFF);
                graphics.fill(bx, by + boxSize - 1, bx + boxSize, by + boxSize, 0x66FFFFFF);
                graphics.fill(bx, by, bx + 1, by + boxSize, 0x66FFFFFF);
                graphics.fill(bx + boxSize - 1, by, bx + boxSize, by + boxSize, 0x66FFFFFF);

                if (slot == null) {
                    continue;
                }
                if (SeedPreviewState.shouldHidePreviewSlots()) {
                    graphics.blit(HIDDEN_TEXTURE, bx + 1, by + 1, boxSize - 2, boxSize - 2, 0, 0, 16, 16);
                    if (minecraft != null && minecraft.font != null) {
                        graphics.drawCenteredString(
                                minecraft.font,
                                "?",
                                bx + boxSize / 2,
                                by + (boxSize - minecraft.font.lineHeight) / 2,
                                0xFFFFFFFF
                        );
                    }
                    continue;
                }

                if (slot.getId().startsWith("quest.")) {
                    var questIcon = QuestIconProvider.iconFor(slot);
                    QuestIconRenderer.renderQuestIcon(
                            graphics,
                            bx,
                            by,
                            questIcon,
                            boxSize
                    );
                } else {
                    ItemStack stack = getStack(slot);
                    if (!stack.isEmpty()) {
                        graphics.renderItem(stack, bx + (boxSize - 16) / 2, by + (boxSize - 16) / 2);
                    } else {
                        graphics.blit(HIDDEN_TEXTURE, bx + 1, by + 1, boxSize - 2, boxSize - 2, 0, 0, 16, 16);
                    }
                }
            }
        }
    }

    private ItemStack getStack(BingoSlot slot) {
        try {
            Identifier rl = com.jamie.jamiebingo.util.IdUtil.id(slot.getId());
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null) return new ItemStack(item);
        } catch (Exception ignored) {}
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }
}


