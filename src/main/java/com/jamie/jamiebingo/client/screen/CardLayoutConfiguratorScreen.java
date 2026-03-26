package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.client.ClientCardLayoutSettings;
import com.jamie.jamiebingo.client.ClientCardData;
import com.jamie.jamiebingo.client.ClientSettingsOverlay;
import com.jamie.jamiebingo.client.ClientTeamScoreData;
import com.jamie.jamiebingo.client.CardSkinDecorRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntConsumer;

public class CardLayoutConfiguratorScreen extends Screen {
    private static final int FALLBACK_CARD_SIZE = 5;
    private static final int CONTROL_HEIGHT = 14;
    private static final int CONTROL_GAP = 3;
    private static final List<String> PRESET_ITEM_POOL = List.of(
            "minecraft:oak_log", "minecraft:stone", "minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond",
            "minecraft:redstone", "minecraft:lapis_lazuli", "minecraft:obsidian", "minecraft:ender_pearl", "minecraft:blaze_rod",
            "minecraft:string", "minecraft:gunpowder", "minecraft:glowstone_dust", "minecraft:slime_ball", "minecraft:honeycomb",
            "minecraft:leather", "minecraft:feather", "minecraft:bone", "minecraft:carrot", "minecraft:bread",
            "minecraft:bucket", "minecraft:clock", "minecraft:compass", "minecraft:book", "minecraft:map"
    );

    private enum Target {
        OVERLAY_CARD("Overlay Card"),
        FULLSCREEN_CARD("Fullscreen Card"),
        SCOREBOARD("Scoreboard"),
        SETTINGS_OVERLAY("Settings Overlay");

        private final String label;

        Target(String label) {
            this.label = label;
        }
    }

    private static final class ScoreLine {
        private final String text;
        private final int color;

        private ScoreLine(String text, int color) {
            this.text = text;
            this.color = color;
        }
    }

    private Target selectedTarget = Target.OVERLAY_CARD;
    private int previewCardSize = 5;
    private boolean previewSizeInitializedFromLive = false;
    private boolean settingsHidden = false;

    private Button skinPrevButton;
    private Button skinButton;
    private Button skinNextButton;
    private PercentSlider cardSizeSlider;
    private Button resetScaleButton;
    private Button resetPositionButton;
    private Button hideSettingsButton;
    private Button closeButton;
    private PercentSlider scaleSlider;

    private Target dragTarget = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public CardLayoutConfiguratorScreen() {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Card Layout Configurator"));
    }

    @Override
    protected void init() {
        ClientCardLayoutSettings.load();
        if (!previewSizeInitializedFromLive) {
            var liveCard = ClientCardData.getCard();
            if (liveCard != null) {
                previewCardSize = clamp(liveCard.getSize(), 1, 10);
            }
            previewSizeInitializedFromLive = true;
        }
        clearWidgets();

        int left = 12;
        int panelWidth = Math.min(220, Math.max(176, width / 3));
        int y = 12;
        int half = (panelWidth - 4) / 2;

        skinPrevButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("<"),
                        b -> cycleSkin(false))
                .pos(left, y)
                .size(18, CONTROL_HEIGHT)
                .build());
        skinButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Skin"),
                        b -> cycleSkin(true))
                .pos(left + 22, y)
                .size(panelWidth - 44, CONTROL_HEIGHT)
                .build());
        skinNextButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(">"),
                        b -> cycleSkin(true))
                .pos(left + panelWidth - 18, y)
                .size(18, CONTROL_HEIGHT)
                .build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        cardSizeSlider = addRenderableWidget(new PercentSlider(
                left, y, panelWidth, CONTROL_HEIGHT,
                1, 10, "Card Size: ", "", this::setPreviewCardSize
        ));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        scaleSlider = addRenderableWidget(new PercentSlider(
                left, y, panelWidth, CONTROL_HEIGHT,
                10, 220, "Scale: ", "%", this::setScalePercent
        ));
        y += CONTROL_HEIGHT + CONTROL_GAP;

        resetScaleButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Reset Scale"),
                        b -> {
                            resetScale();
                            refreshControls();
                        })
                .pos(left, y)
                .size(half, CONTROL_HEIGHT)
                .build());
        resetPositionButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Reset Pos"),
                        b -> {
                            resetPosition();
                            refreshControls();
                        })
                .pos(left + half + 4, y)
                .size(panelWidth - half - 4, CONTROL_HEIGHT)
                .build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        hideSettingsButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Hide UI"),
                        b -> {
                            settingsHidden = !settingsHidden;
                            applySettingsVisibility();
                            refreshControls();
                        })
                .pos(width - 66, height - 20)
                .size(58, 12)
                .build());
        y += CONTROL_HEIGHT + CONTROL_GAP;

        closeButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Done"),
                        b -> onClose())
                .pos(left, y)
                .size(panelWidth, CONTROL_HEIGHT)
                .build());

        refreshControls();
        applySettingsVisibility();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        syncScreenSizeFromWindow();
        if (this.children().isEmpty()) {
            this.init(this.width, this.height);
        }
        graphics.fill(0, 0, width, height, 0xAA0E1116);
        renderPreviews(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, "Click any preview to select and drag it", 12, height - 24, 0xFFBBBBBB, false);
        graphics.drawString(font, "ESC also closes and saves", 12, height - 13, 0xFF888888, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isNew) {
        if (event.button() == 0) {
            Target clicked = findTargetAt((int) event.x(), (int) event.y());
            if (clicked != null) {
                selectedTarget = clicked;
                ClientCardLayoutSettings.LayoutResult bounds = computeBoundsForTarget(clicked);
                dragTarget = clicked;
                dragOffsetX = (int) event.x() - bounds.startX;
                dragOffsetY = (int) event.y() - bounds.startY;
                refreshControls();
                return true;
            }
        }
        return super.mouseClicked(event, isNew);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragTarget == null) {
            return super.mouseDragged(event, dragX, dragY);
        }
        updateTargetPosition(dragTarget, (int) event.x() - dragOffsetX, (int) event.y() - dragOffsetY);
        refreshControls();
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragTarget = null;
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        ClientCardLayoutSettings.save();
        super.onClose();
    }

    private void renderPreviews(GuiGraphics graphics) {
        renderCardPreview(graphics, Target.OVERLAY_CARD);
        renderCardPreview(graphics, Target.FULLSCREEN_CARD);
        renderScoreboardPreview(graphics);
        renderSettingsPreview(graphics);

        ClientCardLayoutSettings.LayoutResult selected = computeBoundsForTarget(selectedTarget);
        int color = settingsHidden ? 0xAA55AA55 : 0xAAFFFF66;
        graphics.fill(selected.startX - 1, selected.startY - 1, selected.startX + selected.totalWidth + 1, selected.startY, color);
        graphics.fill(selected.startX - 1, selected.startY + selected.totalHeight, selected.startX + selected.totalWidth + 1, selected.startY + selected.totalHeight + 1, color);
        graphics.fill(selected.startX - 1, selected.startY - 1, selected.startX, selected.startY + selected.totalHeight + 1, color);
        graphics.fill(selected.startX + selected.totalWidth, selected.startY - 1, selected.startX + selected.totalWidth + 1, selected.startY + selected.totalHeight + 1, color);
    }

    private void renderCardPreview(GuiGraphics graphics, Target target) {
        int cardSize = activeCardSize();
        ClientCardLayoutSettings.CardSkin cardSkin = target == Target.OVERLAY_CARD
                ? ClientCardLayoutSettings.overlay.getSkin()
                : ClientCardLayoutSettings.fullscreen.getSkin();
        ClientCardLayoutSettings.LayoutResult layout = computeBoundsForTarget(target);
        int box = layout.boxSize;
        int spacing = layout.spacing;
        int slotCount = cardSize * cardSize;
        int visualHeight = layout.totalHeight + Math.max(target == Target.FULLSCREEN_CARD ? 10 : 8, box / 2);

        graphics.fill(layout.startX - 2, layout.startY - 2, layout.startX + layout.totalWidth + 2, layout.startY + visualHeight + 2, 0x44111111);
        com.jamie.jamiebingo.client.CardSkinDecorRenderer.drawBoardBackdrop(
                graphics, cardSkin, layout.startX, layout.startY, layout.totalWidth, visualHeight, target == Target.FULLSCREEN_CARD
        );
        for (int idx = 0; idx < slotCount; idx++) {
                int boardRow = idx / cardSize;
                int boardCol = idx % cardSize;
                int col = boardCol;
                int row = boardRow;
                int x = layout.startX + col * (box + spacing);
                int y = layout.startY + row * (box + spacing);

                int bg = previewSlotBackground(cardSkin, col, row, target);
                int border = previewSlotBorder(cardSkin, col, row, target);
                int borderThickness = cardSkin == ClientCardLayoutSettings.CardSkin.MINIMAL ? 1 : 2;
                boolean customSlotFrame = com.jamie.jamiebingo.client.CardSkinDecorRenderer.usesCustomSlotFrame(cardSkin);
                int accent = slotAccentColor(cardSkin, col, row);

                if (!customSlotFrame) {
                    graphics.fill(x, y, x + box, y + box, bg);
                    if (accent != 0) {
                        graphics.fill(x + borderThickness, y + borderThickness, x + box - borderThickness, y + borderThickness + 1, accent);
                        if ((col + row) % 2 == 0) {
                            graphics.fill(x + borderThickness, y + box - borderThickness - 1, x + box - borderThickness, y + box - borderThickness, accent);
                        }
                    }
                    graphics.fill(x, y, x + box, y + borderThickness, border);
                    graphics.fill(x, y + box - borderThickness, x + box, y + box, border);
                    graphics.fill(x, y, x + borderThickness, y + box, border);
                    graphics.fill(x + box - borderThickness, y, x + box, y + box, border);
                }
                com.jamie.jamiebingo.client.CardSkinDecorRenderer.drawSlotDecoration(
                        graphics, cardSkin, x, y, box, col, row, borderThickness, border, bg, target == Target.FULLSCREEN_CARD
                );
                if (box >= 16) {
                    ItemStack stack = presetStackFor(idx);
                    CardSkinDecorRenderer.renderScaledItem(graphics, stack, x, y, box);
                }
                drawRarityLabelExact(graphics, target, cardSkin, x, y, box, border, presetRarityFor(idx));
        }
        String header = target == Target.OVERLAY_CARD ? "Overlay Card Preview" : "Fullscreen Card Preview";
        graphics.drawString(font, header + " " + cardSize + "x" + cardSize, layout.startX, Math.max(2, layout.startY - 10), 0xFFFFFFFF, true);
    }

    private void renderScoreboardPreview(GuiGraphics graphics) {
        List<ScoreLine> lines = scoreboardLines();
        if (lines.isEmpty()) return;
        ClientCardLayoutSettings.LayoutResult bounds = computeBoundsForTarget(Target.SCOREBOARD);
        float scale = Math.max(ClientCardLayoutSettings.MIN_SCALE, ClientCardLayoutSettings.scoreboard.scale);
        int lineHeight = Math.max(8, font.lineHeight + 1);
        int drawX = Math.round(bounds.startX / scale);
        int drawY = Math.round(bounds.startY / scale);

        graphics.fill(bounds.startX - 2, bounds.startY - 2, bounds.startX + bounds.totalWidth + 2, bounds.startY + bounds.totalHeight + 2, 0x44000000);
        com.jamie.jamiebingo.client.OverlayWidgetSkinRenderer.draw(
                graphics,
                ClientCardLayoutSettings.scoreboard.getSkin(),
                bounds.startX,
                bounds.startY,
                bounds.totalWidth,
                bounds.totalHeight
        );
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        pose.pushMatrix();
        pose.scale(scale, scale);
        for (ScoreLine line : lines) {
            if (!line.text.isEmpty()) {
                graphics.drawString(font, line.text, drawX, drawY, line.color, true);
            }
            drawY += lineHeight;
        }
        pose.popMatrix();
    }

    private void renderSettingsPreview(GuiGraphics graphics) {
        List<String> lines = settingsLines();
        ClientCardLayoutSettings.LayoutResult bounds = computeBoundsForTarget(Target.SETTINGS_OVERLAY);
        float scale = Math.max(ClientCardLayoutSettings.MIN_SCALE, ClientCardLayoutSettings.settingsOverlay.scale);
        int lineHeight = font.lineHeight + 2;
        int drawX = Math.round(bounds.startX / scale);
        int drawY = Math.round(bounds.startY / scale);

        graphics.fill(bounds.startX - 2, bounds.startY - 2, bounds.startX + bounds.totalWidth + 2, bounds.startY + bounds.totalHeight + 2, 0x33000000);
        com.jamie.jamiebingo.client.OverlayWidgetSkinRenderer.draw(
                graphics,
                ClientCardLayoutSettings.settingsOverlay.getSkin(),
                bounds.startX,
                bounds.startY,
                bounds.totalWidth,
                bounds.totalHeight
        );
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        pose.pushMatrix();
        pose.scale(scale, scale);
        for (String line : lines) {
            drawSettingsLine(graphics, drawX, drawY, line);
            drawY += lineHeight;
        }
        pose.popMatrix();
    }

    private void cycleSkin(boolean forward) {
        Target target = selectedTarget;
        if (target == Target.OVERLAY_CARD) {
            ClientCardLayoutSettings.overlay.setSkin(forward
                    ? ClientCardLayoutSettings.overlay.getSkin().next()
                    : ClientCardLayoutSettings.overlay.getSkin().previous());
        } else if (target == Target.FULLSCREEN_CARD) {
            ClientCardLayoutSettings.fullscreen.setSkin(forward
                    ? ClientCardLayoutSettings.fullscreen.getSkin().next()
                    : ClientCardLayoutSettings.fullscreen.getSkin().previous());
        } else if (target == Target.SCOREBOARD) {
            ClientCardLayoutSettings.scoreboard.setSkin(forward
                    ? ClientCardLayoutSettings.scoreboard.getSkin().next()
                    : ClientCardLayoutSettings.scoreboard.getSkin().previous());
        } else if (target == Target.SETTINGS_OVERLAY) {
            ClientCardLayoutSettings.settingsOverlay.setSkin(forward
                    ? ClientCardLayoutSettings.settingsOverlay.getSkin().next()
                    : ClientCardLayoutSettings.settingsOverlay.getSkin().previous());
        }
        ClientCardLayoutSettings.save();
        refreshControls();
    }

    private void setScalePercent(int percent) {
        float scale = clamp(percent / 100.0f, ClientCardLayoutSettings.MIN_SCALE, 2.2f);
        switch (selectedTarget) {
            case OVERLAY_CARD, FULLSCREEN_CARD -> selectedCardLayout().scale = scale;
            case SCOREBOARD -> ClientCardLayoutSettings.scoreboard.scale = scale;
            case SETTINGS_OVERLAY -> ClientCardLayoutSettings.settingsOverlay.scale = scale;
        }
        ClientCardLayoutSettings.save();
    }

    private void resetScale() {
        switch (selectedTarget) {
            case OVERLAY_CARD, FULLSCREEN_CARD -> selectedCardLayout().scale = 1.0f;
            case SCOREBOARD -> ClientCardLayoutSettings.scoreboard.resetScale();
            case SETTINGS_OVERLAY -> ClientCardLayoutSettings.settingsOverlay.resetScale();
        }
        ClientCardLayoutSettings.save();
    }

    private void resetPosition() {
        switch (selectedTarget) {
            case OVERLAY_CARD, FULLSCREEN_CARD -> {
                ClientCardLayoutSettings.LayoutConfig layout = selectedCardLayout();
                layout.customPosition = false;
                layout.x = 0;
                layout.y = 0;
            }
            case SCOREBOARD -> ClientCardLayoutSettings.scoreboard.resetPosition();
            case SETTINGS_OVERLAY -> ClientCardLayoutSettings.settingsOverlay.resetPosition();
        }
        ClientCardLayoutSettings.save();
    }

    private void updateTargetPosition(Target target, int x, int y) {
        ClientCardLayoutSettings.LayoutResult bounds = computeBoundsForTarget(target);
        int maxX = Math.max(0, width - bounds.totalWidth);
        int maxY = Math.max(0, height - bounds.totalHeight);
        int clampedX = clamp(x, 0, maxX);
        int clampedY = clamp(y, 0, maxY);

        switch (target) {
            case OVERLAY_CARD -> {
                ClientCardLayoutSettings.LayoutConfig layout = ClientCardLayoutSettings.overlay.mutableLayoutForSize(previewCardSize);
                layout.customPosition = true;
                layout.x = clampedX;
                layout.y = clampedY;
            }
            case FULLSCREEN_CARD -> {
                ClientCardLayoutSettings.LayoutConfig layout = ClientCardLayoutSettings.fullscreen.mutableLayoutForSize(previewCardSize);
                layout.customPosition = true;
                layout.x = clampedX;
                layout.y = clampedY;
            }
            case SCOREBOARD -> {
                ClientCardLayoutSettings.scoreboard.customPosition = true;
                ClientCardLayoutSettings.scoreboard.x = clampedX;
                ClientCardLayoutSettings.scoreboard.y = clampedY;
            }
            case SETTINGS_OVERLAY -> {
                ClientCardLayoutSettings.settingsOverlay.customPosition = true;
                ClientCardLayoutSettings.settingsOverlay.x = clampedX;
                ClientCardLayoutSettings.settingsOverlay.y = clampedY;
            }
        }
        ClientCardLayoutSettings.save();
    }

    private void refreshControls() {
        Target target = selectedTarget;
        hideSettingsButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(settingsHidden ? "Show UI" : "Hide UI"));

        boolean cardTarget = target == Target.OVERLAY_CARD || target == Target.FULLSCREEN_CARD;
        String skinName = target.label + " | " + (target == Target.OVERLAY_CARD
                ? ClientCardLayoutSettings.overlay.getSkin().displayName()
                : target == Target.FULLSCREEN_CARD
                ? ClientCardLayoutSettings.fullscreen.getSkin().displayName()
                : target == Target.SCOREBOARD
                ? ClientCardLayoutSettings.scoreboard.getSkin().displayName()
                : ClientCardLayoutSettings.settingsOverlay.getSkin().displayName());
        skinButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Skin: " + skinName));
        skinButton.active = !settingsHidden;
        skinPrevButton.active = !settingsHidden;
        skinNextButton.active = !settingsHidden;
        cardSizeSlider.active = cardTarget && !settingsHidden;
        cardSizeSlider.setIntValue(previewCardSize);

        int scalePercent = Math.round(activeScale() * 100f);
        scaleSlider.setIntValue(scalePercent);
    }

    private void applySettingsVisibility() {
        boolean visible = !settingsHidden;
        for (AbstractWidget widget : List.of(
                skinButton,
                skinPrevButton,
                skinNextButton,
                cardSizeSlider,
                scaleSlider,
                resetScaleButton,
                resetPositionButton,
                closeButton
        )) {
            widget.visible = visible;
            widget.active = visible;
        }
        hideSettingsButton.visible = true;
        hideSettingsButton.active = true;
    }

    private float activeScale() {
        return switch (selectedTarget) {
            case OVERLAY_CARD, FULLSCREEN_CARD -> selectedCardLayout().scale;
            case SCOREBOARD -> ClientCardLayoutSettings.scoreboard.scale;
            case SETTINGS_OVERLAY -> ClientCardLayoutSettings.settingsOverlay.scale;
        };
    }

    private int activeCardSize() {
        return clamp(previewCardSize, 1, 10);
    }

    private ClientCardLayoutSettings.LayoutResult computeBoundsForTarget(Target target) {
        boolean hangman = com.jamie.jamiebingo.client.ClientGameState.winCondition
                == com.jamie.jamiebingo.bingo.WinCondition.HANGMAN;
        if (target == Target.OVERLAY_CARD) {
            return ClientCardLayoutSettings.computeOverlayLayout(
                    width, height, activeCardSize(), hangman, ClientCardLayoutSettings.overlay.resolveLayout(activeCardSize())
            );
        }
        if (target == Target.FULLSCREEN_CARD) {
            return ClientCardLayoutSettings.computeFullscreenLayout(
                    width, height, activeCardSize(), hangman, ClientCardLayoutSettings.fullscreen.resolveLayout(activeCardSize())
            );
        }

        if (target == Target.SCOREBOARD) {
            List<ScoreLine> lines = scoreboardLines();
            int maxWidth = 0;
            for (ScoreLine line : lines) {
                maxWidth = Math.max(maxWidth, font.width(line.text));
            }
            int lineHeight = Math.max(8, font.lineHeight + 1);
            int contentHeight = lineHeight * lines.size();
            float scale = Math.max(ClientCardLayoutSettings.MIN_SCALE, ClientCardLayoutSettings.scoreboard.scale);
            int defaultX = width - Math.round(maxWidth * scale) - 12;
            int defaultY = height - Math.round(contentHeight * scale) - 22;
            return ClientCardLayoutSettings.resolveWidgetBounds(
                    width, height, maxWidth, contentHeight, defaultX, defaultY, ClientCardLayoutSettings.scoreboard
            );
        }

        List<String> lines = settingsLines();
        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        int lineHeight = font.lineHeight + 2;
        int contentHeight = lineHeight * lines.size();
        float scale = Math.max(ClientCardLayoutSettings.MIN_SCALE, ClientCardLayoutSettings.settingsOverlay.scale);
        int defaultX = 8;
        int defaultY = height / 2 - Math.round(contentHeight * scale / 2f);
        return ClientCardLayoutSettings.resolveWidgetBounds(
                width, height, maxWidth, contentHeight, defaultX, defaultY, ClientCardLayoutSettings.settingsOverlay
        );
    }

    private List<ScoreLine> scoreboardLines() {
        if (!ClientTeamScoreData.hasData()) {
            return List.of(
                    new ScoreLine("Red Team (84) | Lines: 3", 0xFFFF5555),
                    new ScoreLine("  Alice - 41", 0xFFFFFFFF),
                    new ScoreLine("  Bob - 43", 0xFFFFFFFF),
                    new ScoreLine("", 0),
                    new ScoreLine("Blue Team (77) | Lines: 2", 0xFF55AAFF),
                    new ScoreLine("  Jamie - 38", 0xFFFFFFFF),
                    new ScoreLine("  Alex - 39", 0xFFFFFFFF),
                    new ScoreLine("", 0),
                    new ScoreLine("Green Team (62) | Lines: 1", 0xFF55FF55),
                    new ScoreLine("  Sam - 30", 0xFFFFFFFF),
                    new ScoreLine("  Riley - 32", 0xFFFFFFFF)
            );
        }

        List<ScoreLine> lines = new ArrayList<>();
        for (ClientTeamScoreData.TeamEntry team : ClientTeamScoreData.getTeamsSorted()) {
            String header = team.color.getName() + " Team (" + team.totalScore + ")";
            if (team.completedLines >= 0) {
                header = header + " | Lines: " + team.completedLines;
            }
            lines.add(new ScoreLine(header, 0xFF000000 | team.color.getTextColor()));
            for (Map.Entry<UUID, Integer> entry : team.memberScores.entrySet()) {
                String name = team.memberNames.getOrDefault(entry.getKey(), "Player");
                lines.add(new ScoreLine("  " + name + " - " + entry.getValue(), 0xFFFFFFFF));
            }
            lines.add(new ScoreLine("", 0));
        }
        return lines.isEmpty() ? List.of(new ScoreLine("Scoreboard empty", 0xFFFFFFFF)) : lines;
    }

    private List<String> settingsLines() {
        List<String> live = ClientSettingsOverlay.getLines();
        if (!live.isEmpty()) return live;
        return List.of(
                "Mode: Lockout",
                "Card: 5x5 (Hybrid 50%)",
                "Difficulty: Normal",
                "Rerolls: 3",
                "Countdown: 30m",
                "Settings preview preset"
        );
    }

    private Target findTargetAt(int x, int y) {
        Target[] order = new Target[] {
                Target.SETTINGS_OVERLAY,
                Target.SCOREBOARD,
                Target.FULLSCREEN_CARD,
                Target.OVERLAY_CARD
        };
        for (Target target : order) {
            if (contains(computeBoundsForTarget(target), x, y)) {
                return target;
            }
        }
        return null;
    }

    private ItemStack presetStackFor(int index) {
        int poolIndex = Math.floorMod(index, PRESET_ITEM_POOL.size());
        String itemId = PRESET_ITEM_POOL.get(poolIndex);
        ItemStack stack = com.jamie.jamiebingo.util.ItemLookupUtil.stack(itemId);
        if (com.jamie.jamiebingo.util.ItemStackUtil.isEmpty(stack)) {
            return com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier");
        }
        return stack;
    }

    private ClientCardLayoutSettings.LayoutConfig selectedCardLayout() {
        return selectedTarget == Target.FULLSCREEN_CARD
                ? ClientCardLayoutSettings.fullscreen.mutableLayoutForSize(activeCardSize())
                : ClientCardLayoutSettings.overlay.mutableLayoutForSize(activeCardSize());
    }

    private ClientCardLayoutSettings.LayoutConfig cardLayoutForTarget(Target target) {
        return target == Target.FULLSCREEN_CARD
                ? ClientCardLayoutSettings.fullscreen.resolveLayout(activeCardSize())
                : ClientCardLayoutSettings.overlay.resolveLayout(activeCardSize());
    }

    private void setPreviewCardSize(int size) {
        previewCardSize = clamp(size, 1, 10);
        refreshControls();
    }

    private int previewSlotBackground(ClientCardLayoutSettings.CardSkin cardSkin, int c, int r, Target target) {
        boolean fullscreen = target == Target.FULLSCREEN_CARD;
        return switch (cardSkin) {
            case BLOOM -> fullscreen ? 0xCC4E3144 : 0xAA4E3144;
            case RUNIC -> fullscreen ? 0xCC1D2940 : 0xAA1D2940;
            case CELESTIAL -> fullscreen ? 0xCC2E223C : 0xAA2E223C;
            case GLASS -> fullscreen ? 0x88456F9B : 0x66456F9B;
            case MINIMAL -> 0xDD0D0D0D;
            case NEON -> fullscreen ? 0xCC0A1530 : 0xAA0A1530;
            case LAVA -> fullscreen ? 0xCC2A0E00 : 0xAA2A0E00;
            case CANDY -> fullscreen ? 0xCC2D1231 : 0xAA2D1231;
            case TERMINAL -> fullscreen ? 0xCC001A08 : 0xAA001A08;
            case ROYAL -> fullscreen ? 0xCC241900 : 0xAA241900;
            case VOID -> fullscreen ? 0xCC060018 : 0xAA060018;
            case PRISM -> fullscreen ? 0xCC10183A : 0xAA10183A;
            case TOXIC -> fullscreen ? 0xCC121D00 : 0xAA121D00;
            case ICE -> fullscreen ? 0xCC051B2B : 0xAA051B2B;
            case SUNSET -> fullscreen ? 0xCC2D1018 : 0xAA2D1018;
            case GLITCH -> fullscreen ? 0xCC120B1A : 0xAA120B1A;
            case CHROME -> fullscreen ? 0xCC1A1E24 : 0xAA1A1E24;
            default -> fullscreen
                    ? (((c + r) & 1) == 0 ? 0xAA2F3640 : 0xAA3A4450)
                    : (((c + r) & 1) == 0 ? 0x88000000 : 0x88080808);
        };
    }

    private int previewSlotBorder(ClientCardLayoutSettings.CardSkin cardSkin, int c, int r, Target target) {
        boolean fullscreen = target == Target.FULLSCREEN_CARD;
        return switch (cardSkin) {
            case BLOOM -> 0xFFDFA1C4;
            case RUNIC -> 0xFF89C4FF;
            case CELESTIAL -> 0xFFD9B9FF;
            case GLASS -> fullscreen ? 0xFFD4E8FF : 0xAAC4E4FF;
            case MINIMAL -> 0xFFE2E2E2;
            case NEON -> 0xFF72FBFF;
            case LAVA -> 0xFFFFB54D;
            case CANDY -> 0xFFFFD8F0;
            case TERMINAL -> 0xFF8AFFAE;
            case ROYAL -> 0xFFFFEAA0;
            case VOID -> 0xFFE3CFFF;
            case PRISM -> 0xFFFFF8C9;
            case TOXIC -> 0xFFD5FF7F;
            case ICE -> 0xFFC6F5FF;
            case SUNSET -> 0xFFFFC28D;
            case GLITCH -> 0xFFFFA2EA;
            case CHROME -> 0xFFFFFFFF;
            default -> fullscreen ? 0x66FFFFFF : 0x88FFFFFF;
        };
    }

    private int slotAccentColor(ClientCardLayoutSettings.CardSkin skin, int col, int row) {
        return switch (skin) {
            case BLOOM -> ((col + row) % 2 == 0) ? 0x66F4B8D8 : 0x336E485B;
            case RUNIC -> ((col + row) % 2 == 0) ? 0x668FD8FF : 0x33456DA2;
            case CELESTIAL -> ((col + row) % 2 == 0) ? 0x66E4D4FF : 0x33634A8D;
            case NEON -> ((col + row) % 2 == 0) ? 0x663CFAFF : 0x3321A2FF;
            case LAVA -> ((col + row) % 2 == 0) ? 0x66FF7A1F : 0x33FFCA50;
            case CANDY -> ((col + row) % 2 == 0) ? 0x66FFB2DF : 0x33FF77C9;
            case TERMINAL -> ((col + row) % 2 == 0) ? 0x662BFF74 : 0x3334B95D;
            case ROYAL -> ((col + row) % 2 == 0) ? 0x66FFE49B : 0x33B8881F;
            case VOID -> ((col + row) % 2 == 0) ? 0x668A42E8 : 0x334C1A94;
            case PRISM -> ((col + row) % 2 == 0) ? 0x66A8F6FF : 0x66FF7DE5;
            case TOXIC -> ((col + row) % 2 == 0) ? 0x669DFF3E : 0x3367C500;
            case ICE -> ((col + row) % 2 == 0) ? 0x6682E9FF : 0x3352A9D9;
            case SUNSET -> ((col + row) % 2 == 0) ? 0x66FF9566 : 0x33FF4DA8;
            case GLITCH -> ((col + row) % 2 == 0) ? 0x6600FFE0 : 0x33FF42B5;
            case CHROME -> ((col + row) % 2 == 0) ? 0x66D5DEE8 : 0x33808FA6;
            default -> 0;
        };
    }


    private String presetRarityFor(int index) {
        return switch (Math.floorMod(index, 6)) {
            case 0 -> "common";
            case 1 -> "uncommon";
            case 2 -> "rare";
            case 3 -> "epic";
            case 4 -> "legendary";
            default -> "mythic";
        };
    }

    private int rarityColor(String rarity) {
        return switch (rarity) {
            case "common" -> 0xFFAAAAAA;
            case "uncommon" -> 0xFF55FF55;
            case "rare" -> 0xFF55AAFF;
            case "epic" -> 0xFFAA00FF;
            case "legendary" -> 0xFFFFAA00;
            case "mythic" -> 0xFFFF55FF;
            default -> 0xFFFFFFFF;
        };
    }

    private void drawRarityLabelExact(
            GuiGraphics graphics,
            Target target,
            ClientCardLayoutSettings.CardSkin cardSkin,
            int bx,
            int by,
            int boxSize,
            int border,
            String rarity
    ) {
        String text = rarity == null || rarity.length() < 3
                ? ""
                : rarity.substring(0, 3).toUpperCase();
        boolean fullscreen = target == Target.FULLSCREEN_CARD;
        float rarityScale = fullscreen ? 0.6f : 0.55f;
        int textWidth = Math.round(font.width(text) * rarityScale);
        int labelH = fullscreen
                ? Math.max(8, Math.round(font.lineHeight * rarityScale) + 4)
                : Math.max(7, Math.round(font.lineHeight * rarityScale) + 4);
        int labelW = Math.min(boxSize - 4, Math.max(fullscreen ? 14 : 12, textWidth + 8));
        int labelX1 = bx + (boxSize - labelW) / 2;
        int labelY1 = by + boxSize + 1;
        int labelX2 = labelX1 + labelW;
        int labelY2 = labelY1 + labelH;
        int labelBg = switch (cardSkin) {
            case BLOOM -> 0xCC5C3A4E;
            case RUNIC -> 0xCC22324A;
            case CELESTIAL -> 0xCC3B2D4A;
            case GLASS -> fullscreen ? 0xC0253D5A : 0xB0253D5A;
            case MINIMAL -> 0xEE000000;
            case NEON -> 0xCC001727;
            case LAVA -> 0xCC341200;
            case CANDY -> 0xCC3D0E2F;
            case TERMINAL -> 0xCC001F0A;
            case ROYAL -> 0xCC2A1E00;
            case VOID -> 0xCC110022;
            case PRISM -> 0xCC1A1F43;
            case TOXIC -> 0xCC1A2200;
            case ICE -> 0xCC06202F;
            case SUNSET -> 0xCC3B1726;
            case GLITCH -> 0xCC1E1226;
            case CHROME -> 0xCC20252E;
            default -> 0xCC101010;
        };

        graphics.fill(labelX1, labelY1, labelX2, labelY2, labelBg);
        graphics.fill(labelX1, labelY1, labelX2, labelY1 + 1, border);
        graphics.fill(labelX1, labelY2 - 1, labelX2, labelY2, border);
        graphics.fill(labelX1, labelY1, labelX1 + 1, labelY2, border);
        graphics.fill(labelX2 - 1, labelY1, labelX2, labelY2, border);

        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        pose.pushMatrix();
        pose.scale(rarityScale, rarityScale);
        int rarityTextX = Math.round((labelX1 + (labelW - textWidth) / 2f) / rarityScale);
        int rarityTextY = Math.round((labelY1 + (labelH - Math.round(font.lineHeight * rarityScale)) / 2f) / rarityScale);
        graphics.drawString(font, text, rarityTextX, rarityTextY, rarityColor(rarity), true);
        pose.popMatrix();
    }

    private static boolean contains(ClientCardLayoutSettings.LayoutResult bounds, int x, int y) {
        return x >= bounds.startX
                && x <= bounds.startX + bounds.totalWidth
                && y >= bounds.startY
                && y <= bounds.startY + bounds.totalHeight;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String onOff(boolean value) {
        return value ? "On" : "Off";
    }

    private void drawSettingsLine(GuiGraphics graphics, int x, int y, String line) {
        if (line == null) return;
        int colon = line.indexOf(':');
        if (colon < 0) {
            graphics.drawString(font, line, x, y, 0xFFD8D8D8, true);
            return;
        }
        String key = line.substring(0, colon + 1);
        String value = line.substring(colon + 1).stripLeading();
        graphics.drawString(font, key, x, y, 0xFF8FD3FF, true);
        graphics.drawString(font, value, x + font.width(key + " "), y, settingValueColor(value), true);
    }

    private int settingValueColor(String value) {
        if (value == null || value.isBlank()) return 0xFFEDEDED;
        String normalized = value.trim().toLowerCase();
        if (normalized.equals("on") || normalized.equals("enabled") || normalized.equals("true")) return 0xFF83FF9A;
        if (normalized.equals("off") || normalized.equals("disabled") || normalized.equals("false")) return 0xFFFF8D8D;
        if (normalized.contains("random")) return 0xFFFFE082;
        if (normalized.contains("full") || normalized.contains("lockout") || normalized.contains("line")) return 0xFFFFE082;
        return 0xFFF3F3F3;
    }

    private static final class PercentSlider extends AbstractWidget {
        private final int min;
        private final int max;
        private final String label;
        private final String suffix;
        private final IntConsumer onChange;
        private int value;
        private boolean dragging;

        private PercentSlider(
                int x, int y, int width, int height,
                int min, int max, String label, String suffix, IntConsumer onChange
        ) {
            super(x, y, width, height, Component.empty());
            this.min = min;
            this.max = Math.max(min, max);
            this.label = label;
            this.suffix = suffix;
            this.onChange = onChange;
            this.value = min;
            refreshMessage();
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            int bg = active ? 0xFF2A2E35 : 0xFF1A1D22;
            graphics.fill(x, y, x + w, y + h, bg);
            graphics.fill(x, y, x + w, y + 1, 0x66FFFFFF);
            graphics.fill(x, y + h - 1, x + w, y + h, 0x44000000);

            int pad = 6;
            int trackX = x + pad;
            int trackW = Math.max(1, w - pad * 2);
            int trackY = y + h - 5;
            graphics.fill(trackX, trackY, trackX + trackW, trackY + 2, 0xFF101318);

            int fillW = (int) Math.round(trackW * normalized());
            graphics.fill(trackX, trackY, trackX + fillW, trackY + 2, 0xFF66C6FF);
            int knobX = trackX + fillW;
            graphics.fill(knobX - 1, trackY - 2, knobX + 1, trackY + 4, 0xFFE8EEF6);

            graphics.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + 3, 0xFFFFFFFF);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean isNew) {
            super.onClick(event, isNew);
            if (event.button() != 0) return;
            dragging = true;
            setFromMouse(event.x());
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
            super.onDrag(event, dragX, dragY);
            if (!dragging || event.button() != 0) return;
            setFromMouse(event.x());
        }

        @Override
        public void onRelease(MouseButtonEvent event) {
            super.onRelease(event);
            dragging = false;
        }

        private void setFromMouse(double mouseX) {
            int pad = 6;
            int trackX = getX() + pad;
            int trackW = Math.max(1, getWidth() - pad * 2);
            double t = (mouseX - trackX) / (double) trackW;
            t = Math.max(0.0d, Math.min(1.0d, t));
            setIntValue(min + (int) Math.round((max - min) * t));
        }

        private double normalized() {
            if (max <= min) return 0.0d;
            return (value - min) / (double) (max - min);
        }

        private void refreshMessage() {
            setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(label + value + suffix));
        }

        private void setIntValue(int next) {
            int clamped = Math.max(min, Math.min(max, next));
            if (clamped != this.value) {
                this.value = clamped;
                onChange.accept(clamped);
            }
            refreshMessage();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    private void syncScreenSizeFromWindow() {
        if (this.minecraft == null) return;
        Object window = com.jamie.jamiebingo.client.ClientMinecraftUtil.getWindow(this.minecraft);
        int w = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledWidth(window);
        int h = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledHeight(window);
        if (w > 0) this.width = w;
        if (h > 0) this.height = h;
    }
}
