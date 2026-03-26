package com.jamie.jamiebingo.client;


import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.bingo.*;
import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import com.jamie.jamiebingo.client.ClientMineState;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.client.screen.ScreenTooltipUtil;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketToggleHighlight;
import com.jamie.jamiebingo.quest.QuestDefinition;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;

public class BingoCardScreen extends Screen {

    private static final Identifier HIDDEN_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/slot_hidden.png");
    private static final int HIDDEN_TEXTURE_SIZE = 1024;
    private static final Map<Identifier, List<Component>> ITEM_RECIPE_TOOLTIP_CACHE = new HashMap<>();
    private static final Map<Identifier, RecipePreview> ITEM_RECIPE_PREVIEW_CACHE = new HashMap<>();
    private static final Identifier SLOT_TEXTURE = com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/gui/sprites/container/slot.png");
    private static final Identifier ARROW_TEXTURE = com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/gui/sprites/container/villager/trade_arrow.png");
    private static final Identifier FURNACE_BURN_TEXTURE = com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/gui/sprites/container/furnace/burn_progress.png");
    private static final Identifier FURNACE_LIT_TEXTURE = com.jamie.jamiebingo.util.IdUtil.id("minecraft:textures/gui/sprites/container/furnace/lit_progress.png");

    private final BingoCard card;
    private EditBox chatInput;
    private boolean chatInputOpen = false;
    private boolean suppressNextChatChar = false;
    private static boolean overlayHiddenForFullscreen = false;
    private static boolean overlayStateBeforeFullscreen = true;

    public BingoCardScreen(BingoCard card) {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Bingo Card"));
        this.card = card;
    }

    public static void openFullscreen(BingoCard card) {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null || card == null) return;
        if (!overlayHiddenForFullscreen) {
            overlayStateBeforeFullscreen = ClientEvents.cardOverlayEnabled;
            overlayHiddenForFullscreen = true;
        }
        ClientEvents.cardOverlayEnabled = false;
        ClientMinecraftUtil.setScreen(mc, new BingoCardScreen(card));
    }

    public static void closeFullscreen() {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null) return;
        restoreOverlayAfterFullscreen();
        ClientMinecraftUtil.setScreen(mc, null);
    }

    private static void restoreOverlayAfterFullscreen() {
        if (!overlayHiddenForFullscreen) return;
        ClientEvents.cardOverlayEnabled = overlayStateBeforeFullscreen;
        overlayHiddenForFullscreen = false;
    }

    public static boolean isFullscreenCardScreen(Screen screen) {
        return screen instanceof BingoCardScreen;
    }

    public static void onFullscreenScreenRemoved() {
        restoreOverlayAfterFullscreen();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !ClientStartCountdown.isActive();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (chatInputOpen && chatInput != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                closeChatInput();
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                submitChatInput();
                return true;
            }
            if (chatInput.keyPressed(event)) {
                return true;
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_T) {
            openChatInput("");
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SLASH) {
            openChatInput("/");
            return true;
        }

        boolean countdownActive = ClientStartCountdown.isActive();
        if (countdownActive) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
                return true;
            }
            if (ClientKeybinds.TOGGLE_FULLSCREEN_CARD.isActiveAndMatches(InputConstants.getKey(event))) {
                return true;
            }
        } else {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_E) {
                closeFullscreen();
                return true;
            }
            if (ClientKeybinds.TOGGLE_FULLSCREEN_CARD.isActiveAndMatches(InputConstants.getKey(event))) {
                closeFullscreen();
                return true;
            }
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (suppressNextChatChar) {
            suppressNextChatChar = false;
            return true;
        }
        if (chatInputOpen && chatInput != null && chatInput.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void removed() {
        super.removed();
        onFullscreenScreenRemoved();
    }

    /* ===============================
       ITEM HELPERS
       =============================== */

    private ItemStack getStack(BingoSlot slot) {
        try {
            String id = slot.getId();
            Identifier rl = com.jamie.jamiebingo.util.IdUtil.id(id.contains(":") ? id : "minecraft:" + id);
            Item item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null) return new ItemStack(item);
        } catch (Exception ignored) {}
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    /* ===============================
       TEAM COLOUR
       =============================== */

    private DyeColor getLocalTeamColor() {
        if (ClientMinecraftUtil.getPlayer() == null) return null;

        UUID self = com.jamie.jamiebingo.util.EntityUtil.getUUID(ClientMinecraftUtil.getPlayer());
        for (ClientTeamScoreData.TeamEntry team : ClientTeamScoreData.getTeamsSorted()) {
            if (team.memberScores.containsKey(self)) {
                return team.color;
            }
        }
        return null;
    }

    private int localTeamColorARGB(int alpha) {
        DyeColor c = getLocalTeamColor();
        if (c == null) return (alpha << 24) | 0x00CC00;
        return (alpha << 24) | c.getTextColor();
    }

    private int highlightColorARGB(int alpha) {
        DyeColor c = getLocalTeamColor();
        int rgb = c != null ? c.getTextColor() : 0xFFFF00;
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private int flashColorARGB(int alpha, DyeColor ownerColor) {
        int rgb = ownerColor != null
                ? ownerColor.getTextColor()
                : (getLocalTeamColor() != null ? getLocalTeamColor().getTextColor() : 0xFFFF00);
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private void renderQuestProgress(GuiGraphics graphics, int x, int y, int size, float ratio, int rgb) {
        if (ratio <= 0f || ratio >= 1f) return;

        int fillHeight = Math.max(1, Math.round(size * ratio));
        int startY = y + size - fillHeight;
        int stripeHeight = 3;
        boolean dark = false;

        for (int yy = startY; yy < y + size; yy += stripeHeight) {
            int h = Math.min(stripeHeight, y + size - yy);
            int alpha = dark ? 0x22 : 0x44;
            int color = (alpha << 24) | (rgb & 0xFFFFFF);
            graphics.fill(x, yy, x + size, yy + h, color);
            dark = !dark;
        }
    }

    private int rarityColor(String rarity) {
        return switch (rarity.toLowerCase()) {
            case "common" -> 0xFFAAAAAA;
            case "uncommon" -> 0xFF55FF55;
            case "rare" -> 0xFF55AAFF;
            case "epic" -> 0xFFAA00FF;
            case "legendary" -> 0xFFFFAA00;
            case "mythic" -> 0xFFFF55FF;
            default -> 0xFFFFFFFF;
        };
    }

    /* ===============================
       DOUBLED SLOTS (RARITY)
       =============================== */

    private Set<String> computeDoubledSlots(BingoCard card, Set<String> completed) {
        Set<String> doubled = new HashSet<>();
        int size = card.getSize();

        var lines = ClientCompletedLines.getCompletedLines(card, completed);
        for (var line : lines) {
            switch (line.type()) {
                case ROW -> {
                    for (int x = 0; x < size; x++)
                        doubled.add(card.getSlot(x, line.index()).getId());
                }
                case COLUMN -> {
                    for (int y = 0; y < size; y++)
                        doubled.add(card.getSlot(line.index(), y).getId());
                }
                case DIAGONAL_MAIN -> {
                    for (int i = 0; i < size; i++)
                        doubled.add(card.getSlot(i, i).getId());
                }
                case DIAGONAL_ANTI -> {
                    for (int i = 0; i < size; i++)
                        doubled.add(card.getSlot(size - 1 - i, i).getId());
                }
            }
        }
        return doubled;
    }

    private Set<String> computeAllDoubledSlots(
            BingoCard card,
            Map<DyeColor, Set<String>> completedByTeam
    ) {
        Set<String> doubled = new HashSet<>();

        for (Set<String> teamCompleted : completedByTeam.values()) {
            doubled.addAll(computeDoubledSlots(card, teamCompleted));
        }

        return doubled;
    }

    /* ===============================
       RENDER
       =============================== */

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        applyFixedScreenSize();
        if (chatInput != null) {
            chatInput.setWidth(Math.max(120, this.width - 20));
            chatInput.setPosition(10, this.height - 26);
        }
        float appliedScale = FixedGui4ScaleUtil.beginScaledRender(graphics, ClientMinecraftUtil.getMinecraft());
        try {
        mouseX = FixedGui4ScaleUtil.virtualMouseX(mouseX, ClientMinecraftUtil.getMinecraft());
        mouseY = FixedGui4ScaleUtil.virtualMouseY(mouseY, ClientMinecraftUtil.getMinecraft());

        if (ClientCasinoState.isActive()) {
            return;
        }
        ClientPowerSlotState.tickVisual();
        ClientPowerSlotWheelAnimation.tick();

        // No blur in fullscreen mode.

        int countdownSeconds = ClientStartCountdown.getSecondsRemaining();
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (countdownSeconds > 0) {
            String text = "Game starts in " + countdownSeconds + "s";
            var font = ClientMinecraftUtil.getFont(mc);
            if (font == null) return;
            int textX = (width - font.width(text)) / 2;
            int textY = 10;
            graphics.drawString(font, text, textX, textY, 0xFFFFFF, true);
        }
        int wheelTop = countdownSeconds > 0 ? 42 : 34;
        ClientPowerSlotWheelAnimation.render(graphics, this.width, wheelTop);

        int size = card.getSize();
        ClientCardLayoutSettings.CardSkin cardSkin = ClientCardLayoutSettings.fullscreen.getSkin();
        ClientCardLayoutSettings.LayoutResult layout = ClientCardLayoutSettings.computeFullscreenLayout(
                width,
                height,
                size,
                ClientGameState.winCondition == WinCondition.HANGMAN
        );

        int spacing = layout.spacing;
        int boxSize = layout.boxSize;
        int totalWidth = layout.totalWidth;
        int totalHeight = layout.totalHeight;
        int visualHeight = totalHeight + Math.max(10, boxSize / 2);
        int startX = layout.startX;
        int startY = layout.startY;
        int slotCount = size * size;

        var itemRenderer = ClientMinecraftUtil.getItemRenderer(mc);
        var font = ClientMinecraftUtil.getFont(mc);
        if (itemRenderer == null || font == null) return;

        DyeColor myColor = getLocalTeamColor();

        boolean showAllTeamsLines =
                ClientGameState.winCondition == WinCondition.LOCKOUT
                        || ClientGameState.winCondition == WinCondition.RARITY;

        BingoSlot hovered = null;

        Map<DyeColor, Set<String>> completedByTeam = new HashMap<>();

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot s = card.getSlot(x, y);
                DyeColor owner = ClientSlotOwnership.getOwner(s.getId());
                if (owner != null) {
                    completedByTeam
                            .computeIfAbsent(owner, c -> new HashSet<>())
                            .add(s.getId());
                }
            }
        }

        Set<String> myCompleted =
                myColor != null
                        ? completedByTeam.getOrDefault(myColor, Set.of())
                        : Set.of();

        Set<String> doubled =
                ClientGameState.winCondition == WinCondition.RARITY
                        ? computeAllDoubledSlots(card, completedByTeam)
                        : Set.of();
        CardSkinDecorRenderer.drawBoardBackdrop(
                graphics, cardSkin, startX, startY, totalWidth, visualHeight, true
        );

        for (int index = 0; index < slotCount; index++) {
                int c = index % size;
                int r = index / size;
                int displayC = c;
                int displayR = r;

                int bx = startX + displayC * (boxSize + spacing);
                int by = startY + displayR * (boxSize + spacing);

                BingoSlot slot = card.getSlot(c, r);

                boolean blindHidden =
                        ClientGameState.winCondition == WinCondition.BLIND
                                && !(r == 0 && c == 0)
                                && !ClientRevealedSlots.isRevealed(slot.getId());

                boolean hangmanHidden =
                        ClientGameState.winCondition == WinCondition.HANGMAN
                                && !ClientHangmanState.slotRevealed;
                if (!blindHidden && !hangmanHidden) {
                    bx += ClientFlashSlots.shakeOffsetX(slot.getId());
                    by += ClientFlashSlots.shakeOffsetY(slot.getId());
                }

                DyeColor ownerColor = ClientSlotOwnership.getOwner(slot.getId());

                boolean lockedOut =
                        (ClientGameState.winCondition == WinCondition.LOCKOUT
                                || ClientGameState.winCondition == WinCondition.RARITY)
                                && ownerColor != null
                                && ownerColor != myColor;

                int bg = ownerColor != null
                        ? switch (cardSkin) {
                    case BLOOM -> (0x58 << 24) | ownerColor.getTextColor();
                    case RUNIC -> (0x58 << 24) | ownerColor.getTextColor();
                    case CELESTIAL -> (0x58 << 24) | ownerColor.getTextColor();
                    case GLASS -> (0x55 << 24) | ownerColor.getTextColor();
                    case MINIMAL -> (0x3A << 24) | ownerColor.getTextColor();
                    case NEON -> (0x60 << 24) | ownerColor.getTextColor();
                    case LAVA -> (0x66 << 24) | ownerColor.getTextColor();
                    case CANDY -> (0x5A << 24) | ownerColor.getTextColor();
                    case TERMINAL -> (0x58 << 24) | ownerColor.getTextColor();
                    case ROYAL -> (0x5C << 24) | ownerColor.getTextColor();
                    case VOID -> (0x70 << 24) | ownerColor.getTextColor();
                    case PRISM -> (0x62 << 24) | ownerColor.getTextColor();
                    case TOXIC -> (0x64 << 24) | ownerColor.getTextColor();
                    case ICE -> (0x60 << 24) | ownerColor.getTextColor();
                    case SUNSET -> (0x64 << 24) | ownerColor.getTextColor();
                    case GLITCH -> (0x6B << 24) | ownerColor.getTextColor();
                    case CHROME -> (0x67 << 24) | ownerColor.getTextColor();
                    default -> (0x44 << 24) | ownerColor.getTextColor();
                }
                        : switch (cardSkin) {
                    case BLOOM -> 0xCC4E3144;
                    case RUNIC -> 0xCC1D2940;
                    case CELESTIAL -> 0xCC2E223C;
                    case GLASS -> 0x88456F9B;
                    case MINIMAL -> 0xDD0D0D0D;
                    case NEON -> 0xCC0A1530;
                    case LAVA -> 0xCC2A0E00;
                    case CANDY -> 0xCC2D1231;
                    case TERMINAL -> 0xCC001A08;
                    case ROYAL -> 0xCC241900;
                    case VOID -> 0xCC060018;
                    case PRISM -> 0xCC10183A;
                    case TOXIC -> 0xCC121D00;
                    case ICE -> 0xCC051B2B;
                    case SUNSET -> 0xCC2D1018;
                    case GLITCH -> 0xCC120B1A;
                    case CHROME -> 0xCC1A1E24;
                    default -> 0xAA000000;
                };

                int border = ownerColor != null
                        ? switch (cardSkin) {
                    case BLOOM -> (0xEE << 24) | ownerColor.getTextColor();
                    case RUNIC -> (0xEE << 24) | ownerColor.getTextColor();
                    case CELESTIAL -> (0xEE << 24) | ownerColor.getTextColor();
                    case GLASS -> (0xCC << 24) | ownerColor.getTextColor();
                    case MINIMAL -> (0xCC << 24) | ownerColor.getTextColor();
                    case NEON -> (0xEE << 24) | ownerColor.getTextColor();
                    case LAVA -> (0xEE << 24) | ownerColor.getTextColor();
                    case CANDY -> (0xEE << 24) | ownerColor.getTextColor();
                    case TERMINAL -> (0xEE << 24) | ownerColor.getTextColor();
                    case ROYAL -> (0xEE << 24) | ownerColor.getTextColor();
                    case VOID -> (0xEE << 24) | ownerColor.getTextColor();
                    case PRISM -> (0xEE << 24) | ownerColor.getTextColor();
                    case TOXIC -> (0xEE << 24) | ownerColor.getTextColor();
                    case ICE -> (0xEE << 24) | ownerColor.getTextColor();
                    case SUNSET -> (0xEE << 24) | ownerColor.getTextColor();
                    case GLITCH -> (0xEE << 24) | ownerColor.getTextColor();
                    case CHROME -> (0xEE << 24) | ownerColor.getTextColor();
                    default -> (0xAA << 24) | ownerColor.getTextColor();
                }
                        : switch (cardSkin) {
                    case BLOOM -> 0xFFDFA1C4;
                    case RUNIC -> 0xFF89C4FF;
                    case CELESTIAL -> 0xFFD9B9FF;
                    case GLASS -> 0xFFD4E8FF;
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
                    default -> 0x66FFFFFF;
                };
                int borderThickness = cardSkin == ClientCardLayoutSettings.CardSkin.MINIMAL ? 1 : 2;
                boolean customSlotFrame = CardSkinDecorRenderer.usesCustomSlotFrame(cardSkin);

                if (!customSlotFrame) {
                    graphics.fill(bx, by, bx + boxSize, by + boxSize, bg);
                    int accent = slotAccentColor(cardSkin, displayC, displayR);
                    if (accent != 0) {
                        graphics.fill(bx + borderThickness, by + borderThickness, bx + boxSize - borderThickness, by + borderThickness + 1, accent);
                        if ((displayC + displayR) % 2 == 0) {
                            graphics.fill(bx + borderThickness, by + boxSize - borderThickness - 1, bx + boxSize - borderThickness, by + boxSize - borderThickness, accent);
                        }
                    }
                    graphics.fill(bx, by, bx + boxSize, by + borderThickness, border);
                    graphics.fill(bx, by + boxSize - borderThickness, bx + boxSize, by + boxSize, border);
                    graphics.fill(bx, by, bx + borderThickness, by + boxSize, border);
                    graphics.fill(bx + boxSize - borderThickness, by, bx + boxSize, by + boxSize, border);
                }
                CardSkinDecorRenderer.drawSlotDecoration(
                        graphics, cardSkin, bx, by, boxSize, displayC, displayR, borderThickness, border, bg, true
                );

                int iconX = bx + (boxSize - 16) / 2;
                int iconY = by + (boxSize - 16) / 2;

                boolean flashing =
                        ClientFlashSlots.isFlashing(slot.getId())
                                && !blindHidden
                                && !hangmanHidden;

                if (flashing) {
                    float pulse = ClientFlashSlots.pulse(partialTicks);
                    int alpha = 60 + (int) (140 * pulse);
                    int flashFill = flashColorARGB(alpha, ownerColor);
                    graphics.fill(bx, by, bx + boxSize, by + boxSize, flashFill);

                    int t = Math.max(2, boxSize / 8);
                    int borderAlpha = 140 + (int) (80 * pulse);
                    int flashBorder = flashColorARGB(borderAlpha, ownerColor);
                    graphics.fill(bx - t, by - t, bx + boxSize + t, by, flashBorder);
                    graphics.fill(bx - t, by + boxSize, bx + boxSize + t, by + boxSize + t, flashBorder);
                    graphics.fill(bx - t, by, bx, by + boxSize, flashBorder);
                    graphics.fill(bx + boxSize, by, bx + boxSize + t, by + boxSize, flashBorder);
                }

                boolean rerollAnimating =
                        ClientRerollAnimation.isAnimating(slot.getId())
                                && !blindHidden
                                && !hangmanHidden;
                if (rerollAnimating) {
                    float pulse = ClientRerollAnimation.pulse(partialTicks);
                    int alpha = 35 + (int) (90 * pulse);
                    int shimmer = (alpha << 24) | 0x00FFE08A;
                    graphics.fill(bx, by, bx + boxSize, by + boxSize, shimmer);
                    int t = Math.max(1, boxSize / 12);
                    int edgeAlpha = 120 + (int) (100 * pulse);
                    int edge = (edgeAlpha << 24) | 0x00FFD86B;
                    graphics.fill(bx - t, by - t, bx + boxSize + t, by, edge);
                    graphics.fill(bx - t, by + boxSize, bx + boxSize + t, by + boxSize + t, edge);
                    graphics.fill(bx - t, by, bx, by + boxSize, edge);
                    graphics.fill(bx + boxSize, by, bx + boxSize + t, by + boxSize, edge);
                }

                boolean highlighted =
                        ClientHighlightedSlots.isHighlighted(slot.getId())
                                && !blindHidden
                                && !hangmanHidden;

                if (highlighted) {
                    int hCol = highlightColorARGB(220);
                    int t = Math.max(2, boxSize / 10);
                    graphics.fill(bx - t, by - t, bx + boxSize + t, by, hCol);
                    graphics.fill(bx - t, by + boxSize, bx + boxSize + t, by + boxSize + t, hCol);
                    graphics.fill(bx - t, by, bx, by + boxSize, hCol);
                    graphics.fill(bx + boxSize, by, bx + boxSize + t, by + boxSize, hCol);
                }

                if (ownerColor != null) {
                    int completeShade = 0x7A000000;
                    graphics.fill(
                            bx + borderThickness,
                            by + borderThickness,
                            bx + boxSize - borderThickness,
                            by + boxSize - borderThickness,
                            completeShade
                    );
                    int completeBorder = (0xE0 << 24) | (ownerColor.getTextColor() & 0xFFFFFF);
                    int ct = Math.max(2, borderThickness + 1);
                    graphics.fill(bx - ct, by - ct, bx + boxSize + ct, by, completeBorder);
                    graphics.fill(bx - ct, by + boxSize, bx + boxSize + ct, by + boxSize + ct, completeBorder);
                    graphics.fill(bx - ct, by, bx, by + boxSize, completeBorder);
                    graphics.fill(bx + boxSize, by, bx + boxSize + ct, by + boxSize, completeBorder);
                }

                if (blindHidden || hangmanHidden) {
                    String q = "?";
                    int qx = bx + (boxSize - font.width(q)) / 2;
                    int qy = by + (boxSize - font.lineHeight) / 2;
                    graphics.drawString(font, q, qx, qy, 0xFFFFFFFF, true);
                    continue;
                }

                if (slot.getId().startsWith("quest.")) {
                    int max = ClientQuestProgressData.getMax(slot.getId());
                    int progress = ClientQuestProgressData.getProgress(slot.getId());
                    boolean completed = ClientProgressData.isCompleted(slot.getId());
                    if (max > 0 && progress > 0 && !completed) {
                        DyeColor teamColor = getLocalTeamColor();
                        int rgb = teamColor != null ? teamColor.getTextColor() : 0x00CC00;
                        float ratio = Math.min(1f, progress / (float) max);
                        renderQuestProgress(graphics, bx, by, boxSize, ratio, rgb);
                    }

                    var questIcon = QuestIconProvider.iconFor(slot);
                    renderQuestIconSharp(graphics, bx, by, boxSize, questIcon);
                } else {
                    ItemStack stack = getStack(slot);
                    if (!stack.isEmpty()) {
                        renderItemSharp(graphics, stack, iconX, iconY);
                    }
                }

                if (lockedOut) {
                    renderItemSharp(graphics, com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"), iconX, iconY);
                }

                {
                    String text = slot.getRarity().substring(0, Math.min(3, slot.getRarity().length())).toUpperCase();

                    if (ClientGameState.winCondition == WinCondition.RARITY) {
                        int pts = RarityScoreCalculator.base(slot);
                        if (doubled.contains(slot.getId())) pts *= 2;
                        text += " " + pts;
                    }

                    float rarityScale = 0.6f;
                    int textWidth = Math.round(font.width(text) * rarityScale);
                    int labelW = Math.min(boxSize - 4, Math.max(14, textWidth + 8));
                    int labelH = Math.min(boxSize / 2 - 2, Math.max(8, Math.round(font.lineHeight * rarityScale) + 4));
                    int labelX1 = bx + (boxSize - labelW) / 2;
                    int labelY1 = by + boxSize + 1;
                    int labelX2 = labelX1 + labelW;
                    int labelY2 = labelY1 + labelH;
                    int labelBg = switch (cardSkin) {
                        case BLOOM -> 0xCC5C3A4E;
                        case RUNIC -> 0xCC22324A;
                        case CELESTIAL -> 0xCC3B2D4A;
                        case GLASS -> 0xC0253D5A;
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
                    graphics.drawString(font, text, rarityTextX, rarityTextY, rarityColor(slot.getRarity()), true);
                    pose.popMatrix();
                }

                if (mouseX >= bx && mouseX <= bx + boxSize &&
                        mouseY >= by && mouseY <= by + boxSize) {
                    hovered = slot;
                }
            }
        renderMineSlot(graphics, mouseX, mouseY, startX, startY, boxSize);
        renderPowerSlot(graphics, mouseX, mouseY, startX, startY, boxSize);

        int thickness = Math.max(3, boxSize / 5);

        for (var entry : completedByTeam.entrySet()) {

            DyeColor teamColor = entry.getKey();

            if (!showAllTeamsLines && teamColor != myColor)
                continue;

            int lineColor = (0x08 << 24) | teamColor.getTextColor();
            var lines = ClientCompletedLines.getCompletedLines(card, entry.getValue());

            for (var line : lines) {
                switch (line.type()) {
                    case ROW -> {
                        int y = startY + line.index() * (boxSize + spacing) + boxSize / 2;
                        graphics.fill(
                                startX, y - thickness / 2,
                                startX + totalWidth, y + thickness / 2,
                                lineColor);
                    }
                    case COLUMN -> {
                        int x = startX + line.index() * (boxSize + spacing) + boxSize / 2;
                        graphics.fill(
                                x - thickness / 2, startY,
                                x + thickness / 2, startY + totalHeight,
                                lineColor);
                    }
                    case DIAGONAL_MAIN ->
                            drawDiagonal(graphics,
                                    startX, startY,
                                    startX + totalWidth, startY + totalHeight,
                                    thickness, lineColor);
                    case DIAGONAL_ANTI ->
                            drawDiagonal(graphics,
                                    startX + totalWidth, startY,
                                    startX, startY + totalHeight,
                                    thickness, lineColor);
                }
            }
        }

        renderFakeMarkers(graphics, font, slotCount, startX, startY, boxSize, spacing, size);

        if (hovered != null) {
            if (ClientGameState.winCondition == WinCondition.BLIND
                    && !ClientRevealedSlots.isRevealed(hovered.getId())) {

                BingoSlot topLeft = card.getSlot(0, 0);
                String topLeftId = topLeft != null ? topLeft.getId() : null;

                if (topLeftId == null || !topLeftId.equals(hovered.getId())) {
                    super.render(graphics, mouseX, mouseY, partialTicks);
                    return;
                }
            }

            if (ClientGameState.winCondition == WinCondition.HANGMAN
                    && !ClientHangmanState.slotRevealed) {
                super.render(graphics, mouseX, mouseY, partialTicks);
                return;
            }

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(hovered.getName()));
            RecipePreview hoveredRecipePreview = null;

            ItemStack stack = getStack(hovered);
            Identifier key = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (key != null && key.getPath().endsWith("_banner_pattern")) {
                String path = key.getPath().replace("_banner_pattern", "");
                String pretty = Arrays.stream(path.split("_"))
                        .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                        .reduce((a, b) -> a + " " + b)
                        .orElse(path);
                tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(pretty));
            }

            if (hovered.getId().startsWith("quest.")) {
                final String hoveredId = hovered.getId();
                QuestDefinition def = QuestDatabase.getQuests().stream()
                        .filter(q -> q.id.equals(hoveredId))
                        .findFirst()
                        .orElse(null);
                if (def != null && def.description != null && !def.description.isBlank()) {
                    tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(def.description));
                }

                int max = ClientQuestProgressData.getMax(hoveredId);
                if (max > 0) {
                    int progress = ClientQuestProgressData.getProgress(hoveredId);
                    tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(formatQuestProgress(hoveredId, progress, max))
                            .withStyle(ChatFormatting.BLUE));
                }
            } else {
                hoveredRecipePreview = getRecipePreview(stack);
            }

            if (hoveredRecipePreview != null) {
                renderRecipePreviewPanel(graphics, hoveredRecipePreview, mouseX, mouseY);
            }
            ScreenTooltipUtil.drawComponentTooltip(
                    graphics,
                    font,
                    tooltip,
                    mouseX,
                    mouseY,
                    this.width,
                    this.height,
                    Math.max(180, this.width - 24)
            );
        }

        if (chatInputOpen) {
            graphics.drawString(font, "Chat", 12, this.height - 39, 0xFFFFFFFF, true);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
        } finally {
            FixedGui4ScaleUtil.endScaledRender(graphics, appliedScale);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Avoid double-blur from Screen.render calling renderBackground again.
    }

    private static String formatQuestProgress(String questId, int progress, int max) {
        if ("quest.wear_a_carved_pumpkin_for_5_minutes".equals(questId)) {
            double minutes = progress / 1200.0;
            return String.format(java.util.Locale.ROOT, "%.1f/5", minutes);
        }
        if ("quest.ride_a_happy_ghast_for_200_meters".equals(questId)) {
            double meters = progress / 100.0;
            return String.format(java.util.Locale.ROOT, "%.1f/200", meters);
        }
        if ("quest.ride_a_minecart_for_25_meters".equals(questId)) {
            double meters = progress / 100.0;
            return String.format(java.util.Locale.ROOT, "%.1f/25", meters);
        }
        if ("quest.use_a_ladder_to_climb_64_meters_in_hieght".equals(questId)) {
            double meters = progress / 100.0;
            return String.format(java.util.Locale.ROOT, "%.1f/64", meters);
        }
        return progress + "/" + max;
    }

    private void appendItemRecipeTooltip(List<Component> tooltip, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        Identifier key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return;

        List<Component> cached = ITEM_RECIPE_TOOLTIP_CACHE.get(key);
        if (cached != null) {
            tooltip.addAll(cached);
            return;
        }

        List<Component> built = buildItemRecipeTooltip(stack);
        // Null means recipe data is not ready yet; retry on future hovers.
        if (built == null) return;
        // Don't cache empty hits; avoids stale misses if recipe graph isn't ready yet.
        if (!built.isEmpty()) {
            ITEM_RECIPE_TOOLTIP_CACHE.put(key, built);
        }
        tooltip.addAll(built);
    }

    private List<Component> buildItemRecipeTooltip(ItemStack target) {
        List<Component> out = new ArrayList<>();
        List<Component> fromRecipeBook = buildFromClientRecipeBook(target);
        if (fromRecipeBook != null && !fromRecipeBook.isEmpty()) {
            return fromRecipeBook;
        }

        var level = ClientMinecraftUtil.getLevel();
        if (level == null) return null;

        Object manager = findClientRecipeManager(level);
        if (manager == null) return null;
        List<Object> allRecipes = findRecipeObjects(manager);
        if (allRecipes.isEmpty()) return null;

        List<Component> lines = new ArrayList<>();
        int shown = 0;

        for (Object recipeLike : allRecipes) {
            Object recipe = unwrapRecipe(recipeLike);
            if (recipe == null) continue;
            ItemStack result = resultStackForRecipe(recipe, level);
            if (result == null || result.isEmpty() || result.getItem() != target.getItem()) continue;

            String kind = recipeKind(recipe);
            String ingredients = ingredientSummary(recipe);
            lines.add(com.jamie.jamiebingo.util.ComponentUtil.literal(kind + ": " + ingredients).withStyle(ChatFormatting.GRAY));

            shown++;
            if (shown >= 3) break;
        }

        if (!lines.isEmpty()) {
            out.add(com.jamie.jamiebingo.util.ComponentUtil.literal("How to obtain").withStyle(ChatFormatting.DARK_AQUA));
            out.addAll(lines);
        }
        return out;
    }

    private List<Component> buildFromClientRecipeBook(ItemStack target) {
        var player = ClientMinecraftUtil.getPlayer();
        if (player == null || target == null || target.isEmpty()) return null;

        Object recipeBook = findClientRecipeBook(player);
        if (recipeBook == null) return null;

        List<Object> entries = findRecipeDisplayEntries(recipeBook);
        if (entries.isEmpty()) return null;

        Object ctx = createEmptyContextMap();
        List<Component> lines = new ArrayList<>();
        int shown = 0;

        for (Object entry : entries) {
            List<ItemStack> results = entryResultStacks(entry, ctx);
            if (results.isEmpty()) continue;

            boolean matches = false;
            for (ItemStack result : results) {
                if (result != null && !result.isEmpty() && result.getItem() == target.getItem()) {
                    matches = true;
                    break;
                }
            }
            if (!matches) continue;

            String kind = displayKind(entry);
            String ingredients = entryIngredientSummary(entry);
            lines.add(com.jamie.jamiebingo.util.ComponentUtil.literal(kind + ": " + ingredients).withStyle(ChatFormatting.GRAY));

            shown++;
            if (shown >= 3) break;
        }

        if (lines.isEmpty()) return List.of();
        List<Component> out = new ArrayList<>();
        out.add(com.jamie.jamiebingo.util.ComponentUtil.literal("How to obtain").withStyle(ChatFormatting.DARK_AQUA));
        out.addAll(lines);
        return out;
    }

    private Object findClientRecipeBook(Object player) {
        if (player == null) return null;
        try {
            for (Method m : player.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                if (!rt.contains("clientrecipebook")) continue;
                Object out = m.invoke(player);
                if (out != null) return out;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private List<Object> findRecipeDisplayEntries(Object recipeBook) {
        List<Object> out = new ArrayList<>();
        if (recipeBook == null) return out;

        // 1.21.11: ClientRecipeBook stores all entries in a private map.
        try {
            for (java.lang.reflect.Field f : recipeBook.getClass().getDeclaredFields()) {
                if (!Map.class.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                Object raw = f.get(recipeBook);
                if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) continue;
                Object sample = map.values().iterator().next();
                if (sample != null && sample.getClass().getName().toLowerCase(Locale.ROOT).contains("recipedisplayentry")) {
                    out.addAll((Collection<? extends Object>) map.values());
                    if (!out.isEmpty()) return out;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            for (Method m : recipeBook.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!List.class.isAssignableFrom(m.getReturnType())) continue;
                Object listObj = m.invoke(recipeBook);
                if (!(listObj instanceof List<?> collections) || collections.isEmpty()) continue;
                Object firstCollection = collections.get(0);
                if (firstCollection == null) continue;

                for (Method cm : firstCollection.getClass().getMethods()) {
                    if (cm.getParameterCount() != 0) continue;
                    if (!List.class.isAssignableFrom(cm.getReturnType())) continue;
                    Object entriesObj = cm.invoke(firstCollection);
                    if (!(entriesObj instanceof List<?> entries) || entries.isEmpty()) continue;
                    Object firstEntry = entries.get(0);
                    if (firstEntry != null && firstEntry.getClass().getName().toLowerCase(Locale.ROOT).contains("recipedisplayentry")) {
                        out.addAll((List<Object>) entries);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private Object createEmptyContextMap() {
        try {
            Class<?> cls = Class.forName("net.minecraft.util.context.ContextMap");
            java.lang.reflect.Constructor<?> c = cls.getDeclaredConstructor(Map.class);
            c.setAccessible(true);
            return c.newInstance(Collections.emptyMap());
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object createSlotDisplayContextFromLevel() {
        try {
            Object level = ClientMinecraftUtil.getLevel();
            if (level == null) return null;
            Class<?> cls = Class.forName("net.minecraft.world.item.crafting.display.SlotDisplayContext");
            for (Method m : cls.getMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
                if (!"fromLevel".equals(m.getName())) continue;
                if (m.getParameterCount() != 1) continue;
                if (!m.getParameterTypes()[0].isAssignableFrom(level.getClass())) continue;
                return m.invoke(null, level);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private List<ItemStack> entryResultStacks(Object entry, Object contextMap) {
        List<ItemStack> out = new ArrayList<>();
        if (entry == null) return out;
        try {
            for (Method m : entry.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!List.class.isAssignableFrom(m.getReturnType())) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (contextMap == null || !p.isAssignableFrom(contextMap.getClass())) continue;
                Object v = m.invoke(entry, contextMap);
                if (v instanceof List<?> list) {
                    for (Object obj : list) {
                        if (obj instanceof ItemStack s) out.add(s);
                    }
                    if (!out.isEmpty()) return out;
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private String entryIngredientSummary(Object entry) {
        if (entry == null) return "Unknown ingredients";
        try {
            for (Method m : entry.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!Optional.class.isAssignableFrom(m.getReturnType())) continue;
                Object ov = m.invoke(entry);
                if (!(ov instanceof Optional<?> opt) || opt.isEmpty()) continue;
                Object inner = opt.get();
                if (!(inner instanceof List<?> list) || list.isEmpty()) continue;

                List<String> names = new ArrayList<>();
                for (Object ingredient : list) {
                    ItemStack sample = ingredientExampleStack(ingredient);
                    if (sample != null && !sample.isEmpty()) {
                        names.add(sample.getHoverName().getString());
                    }
                }
                if (!names.isEmpty()) return String.join(" + ", names);
            }
        } catch (Throwable ignored) {
        }
        return "Unknown ingredients";
    }

    private String displayKind(Object entry) {
        if (entry == null) return "Craft";
        try {
            for (Method m : entry.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                if (!rt.contains("recipedisplay")) continue;
                Object display = m.invoke(entry);
                if (display == null) break;
                String name = display.getClass().getSimpleName().toLowerCase(Locale.ROOT);
                if (name.contains("smelting")) return "Smelt";
                if (name.contains("blasting")) return "Blast";
                if (name.contains("smoking")) return "Smoke";
                if (name.contains("campfire")) return "Campfire";
                if (name.contains("stonecut")) return "Stonecut";
                if (name.contains("smith")) return "Smith";
                return "Craft";
            }
        } catch (Throwable ignored) {
        }
        return "Craft";
    }

    private Object findClientRecipeManager(net.minecraft.client.multiplayer.ClientLevel level) {
        if (level == null) return null;
        try {
            for (Method m : level.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                if (!(rt.contains("recipemanager") || rt.contains("recipeaccess"))) continue;
                Object out = m.invoke(level);
                if (out != null) return out;
            }
        } catch (Throwable ignored) {
        }

        try {
            Minecraft mc = ClientMinecraftUtil.getMinecraft();
            if (mc != null) {
                for (Method m : mc.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                    if (!rt.contains("clientpacketlistener")) continue;
                    Object listener = m.invoke(mc);
                    if (listener == null) continue;
                    for (Method lm : listener.getClass().getMethods()) {
                        if (lm.getParameterCount() != 0) continue;
                        String lrt = lm.getReturnType().getName().toLowerCase(Locale.ROOT);
                        if (!(lrt.contains("recipemanager") || lrt.contains("recipeaccess"))) continue;
                        Object out = lm.invoke(listener);
                        if (out != null) return out;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private List<Object> findRecipeObjects(Object manager) {
        if (manager == null) return List.of();
        List<Object> out = new ArrayList<>();
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        collectRecipeObjects(manager, out, visited, 0);
        return out;
    }

    private void collectRecipeObjects(Object root, List<Object> out, Set<Object> visited, int depth) {
        if (root == null || out == null || visited == null || depth > 6) return;
        if (!visited.add(root)) return;

        if (root instanceof Collection<?> collection) {
            for (Object element : collection) collectRecipeObjects(element, out, visited, depth + 1);
            return;
        }
        if (root instanceof Map<?, ?> map) {
            for (Object value : map.values()) collectRecipeObjects(value, out, visited, depth + 1);
            return;
        }
        if (root instanceof Iterable<?> iterable) {
            for (Object element : iterable) collectRecipeObjects(element, out, visited, depth + 1);
            return;
        }
        if (root.getClass().isArray()) {
            int len = Array.getLength(root);
            for (int i = 0; i < len; i++) collectRecipeObjects(Array.get(root, i), out, visited, depth + 1);
            return;
        }

        String className = root.getClass().getName().toLowerCase(Locale.ROOT);
        if (className.contains(".recipe") || className.contains("recipeholder")) {
            out.add(root);
        }

        try {
            for (Method m : root.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String name = m.getName().toLowerCase(Locale.ROOT);
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                boolean maybeContainer = name.contains("recipe")
                        || name.contains("values")
                        || name.contains("entries")
                        || name.contains("all")
                        || name.contains("bytype")
                        || name.contains("map")
                        || rt.contains("recipe")
                        || Collection.class.isAssignableFrom(m.getReturnType())
                        || Map.class.isAssignableFrom(m.getReturnType());
                if (!maybeContainer) continue;

                Object value = m.invoke(root);
                if (value != null) collectRecipeObjects(value, out, visited, depth + 1);
            }
        } catch (Throwable ignored) {
        }
    }

    private Object unwrapRecipe(Object holder) {
        if (holder == null) return null;
        String cls = holder.getClass().getName().toLowerCase(Locale.ROOT);
        if (cls.contains("recipe") && !cls.contains("holder")) return holder;

        // Fast path for RecipeHolder on 1.21: prefer the value accessor.
        if (cls.contains("recipeholder")) {
            try {
                for (Method m : holder.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    Class<?> rt = m.getReturnType();
                    String rtn = rt.getName().toLowerCase(Locale.ROOT);
                    if (rtn.contains("resourcekey")) continue;
                    if (!rtn.contains("recipe")) continue;
                    Object out = m.invoke(holder);
                    if (isRecipeLike(out)) return out;
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            for (Method m : holder.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                if (!rt.contains("recipe")) continue;
                Object out = m.invoke(holder);
                if (isRecipeLike(out)) return out;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private boolean isRecipeLike(Object value) {
        if (value == null) return false;
        String cn = value.getClass().getName().toLowerCase(Locale.ROOT);
        if (cn.contains("resourcekey") || cn.contains("optional")) return false;
        if (!cn.contains("recipe")) return false;
        return true;
    }

    private ItemStack resultStackForRecipe(Object recipe, net.minecraft.client.multiplayer.ClientLevel level) {
        if (recipe == null || level == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        try {
            for (Method m : recipe.getClass().getMethods()) {
                if (m.getReturnType() != ItemStack.class) continue;
                if (m.getParameterCount() == 1) {
                    Object arg = findSingleArgumentForMethod(m, level);
                    if (arg != null) {
                        Object out = m.invoke(recipe, arg);
                        if (out instanceof ItemStack stack) return stack;
                    }
                } else if (m.getParameterCount() == 0) {
                    Object out = m.invoke(recipe);
                    if (out instanceof ItemStack stack) return stack;
                }
            }
        } catch (Throwable ignored) {
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private Object findSingleArgumentForMethod(Method m, net.minecraft.client.multiplayer.ClientLevel level) {
        if (m == null || level == null || m.getParameterCount() != 1) return null;
        Class<?> p = m.getParameterTypes()[0];
        try {
            Object registryAccess = level.registryAccess();
            if (registryAccess != null && p.isAssignableFrom(registryAccess.getClass())) {
                return registryAccess;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String ingredientSummary(Object recipe) {
        if (recipe == null) return "Unknown";
        List<String> names = new ArrayList<>();
        for (Object ingredient : extractIngredientObjects(recipe)) {
            ItemStack sample = ingredientExampleStack(ingredient);
            if (sample != null && !sample.isEmpty()) {
                names.add(sample.getHoverName().getString());
            }
        }
        if (names.isEmpty()) return "Unknown ingredients";
        return String.join(" + ", names);
    }

    private List<Object> extractIngredientObjects(Object recipe) {
        List<Object> out = new ArrayList<>();
        if (recipe == null) return out;
        try {
            for (Method m : recipe.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                if (!rt.contains("placementinfo")) continue;
                Object placement = m.invoke(recipe);
                if (placement == null) continue;
                for (Method pm : placement.getClass().getMethods()) {
                    if (pm.getParameterCount() != 0) continue;
                    Class<?> prt = pm.getReturnType();
                    if (!(Collection.class.isAssignableFrom(prt) || List.class.isAssignableFrom(prt))) continue;
                    Object v = pm.invoke(placement);
                    collectObjects(v, out);
                    if (!out.isEmpty()) return out;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : recipe.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String name = m.getName().toLowerCase(Locale.ROOT);
                if (!name.contains("ingredient")) continue;
                Object v = m.invoke(recipe);
                collectObjects(v, out);
                if (!out.isEmpty()) return out;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : recipe.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                if (!rt.contains("placement")) continue;
                Object placement = m.invoke(recipe);
                if (placement == null) continue;
                for (Method pm : placement.getClass().getMethods()) {
                    if (pm.getParameterCount() != 0) continue;
                    String pn = pm.getName().toLowerCase(Locale.ROOT);
                    if (!pn.contains("ingredient")) continue;
                    Object v = pm.invoke(placement);
                    collectObjects(v, out);
                    if (!out.isEmpty()) return out;
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private void collectObjects(Object v, List<Object> out) {
        if (v == null || out == null) return;
        if (v instanceof Collection<?> c) {
            out.addAll(c);
            return;
        }
        if (v.getClass().isArray()) {
            int len = Array.getLength(v);
            for (int i = 0; i < len; i++) {
                out.add(Array.get(v, i));
            }
            return;
        }
        if (v instanceof Iterable<?> it) {
            for (Object obj : it) out.add(obj);
            return;
        }
        out.add(v);
    }

    private ItemStack ingredientExampleStack(Object ingredient) {
        if (ingredient == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        try {
            for (Method m : ingredient.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (m.getReturnType() == ItemStack.class) {
                    Object out = m.invoke(ingredient);
                    if (out instanceof ItemStack stack && !stack.isEmpty()) return stack;
                }
                if (m.getReturnType().isArray() && m.getReturnType().getComponentType() == ItemStack.class) {
                    Object out = m.invoke(ingredient);
                    if (out != null && Array.getLength(out) > 0) {
                        Object first = Array.get(out, 0);
                        if (first instanceof ItemStack stack && !stack.isEmpty()) return stack;
                    }
                }
                if (java.util.stream.Stream.class.isAssignableFrom(m.getReturnType())) {
                    Object out = m.invoke(ingredient);
                    if (out instanceof java.util.stream.Stream<?> stream) {
                        Object first = stream.findFirst().orElse(null);
                        if (first != null) {
                            // Holder<Item> path used by 1.21 Ingredient
                            for (Method hm : first.getClass().getMethods()) {
                                if (hm.getParameterCount() != 0) continue;
                                Object hv = hm.invoke(first);
                                if (hv instanceof Item item) {
                                    ItemStack stack = new ItemStack(item);
                                    if (!stack.isEmpty()) return stack;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private String recipeKind(Object recipe) {
        if (recipe == null) return "Recipe";
        String name = recipe.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (name.contains("smelting")) return "Smelt";
        if (name.contains("blasting")) return "Blast";
        if (name.contains("smoking")) return "Smoke";
        if (name.contains("campfire")) return "Campfire";
        if (name.contains("stonecut")) return "Stonecut";
        if (name.contains("smith")) return "Smith";
        return "Craft";
    }

    private RecipePreview getRecipePreview(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        Identifier key = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (key == null) return null;

        RecipePreview cached = ITEM_RECIPE_PREVIEW_CACHE.get(key);
        if (cached != null) return cached;

        RecipePreview built = buildRecipePreviewFromClientRecipeBook(stack);
        if (built == null) return null;
        if (!allInputsEmpty(built.inputs)) {
            ITEM_RECIPE_PREVIEW_CACHE.put(key, built);
        }
        return built;
    }

    private RecipePreview buildRecipePreviewFromClientRecipeBook(ItemStack target) {
        var player = ClientMinecraftUtil.getPlayer();
        if (player == null || target == null || target.isEmpty()) return null;
        Object recipeBook = findClientRecipeBook(player);
        if (recipeBook == null) return null;

        List<Object> entries = findRecipeDisplayEntries(recipeBook);
        if (entries.isEmpty()) return null;

        Object ctx = createSlotDisplayContextFromLevel();
        if (ctx == null) {
            ctx = createEmptyContextMap();
        }
        for (Object entry : entries) {
            List<ItemStack> results = entryResultStacks(entry, ctx);
            if (results.isEmpty()) continue;

            ItemStack out = null;
            for (ItemStack result : results) {
                if (result != null && !result.isEmpty() && result.getItem() == target.getItem()) {
                    out = result.copy();
                    break;
                }
            }
            if (out == null) continue;

            Object display = extractDisplay(entry);
            if (display == null) {
                continue;
            }
            List<ItemStack> fallbackIngredients = extractEntryIngredientStacks(entry);
            String displayClass = display.getClass().getName().toLowerCase(Locale.ROOT);

            List<Object> listSlots = readSlotDisplayList(display);
            List<Object> scalarSlots = readSlotDisplayScalars(display);
            List<Object> declaredScalarSlots = readSlotDisplayScalarsDeclared(display);
            int[] wh = readDisplayWidthHeight(display);
            boolean shapedDisplay = displayClass.contains("shapedcraftingrecipedisplay");
            boolean shapelessDisplay = displayClass.contains("shapelesscraftingrecipedisplay");
            boolean furnaceDisplay = displayClass.contains("furnacerecipedisplay");
            boolean smithingDisplay = displayClass.contains("smithingrecipedisplay");
            boolean stonecutDisplay = displayClass.contains("stonecutterrecipedisplay");
            boolean hasShape = shapedDisplay || (wh[0] > 1 && wh[1] > 1);
            boolean hasFloat = hasNoArgFloat(display);

            if (furnaceDisplay || hasFloat) {
                List<ItemStack> inputs = new ArrayList<>();
                Object ingredientSlot = callNoArg(display, "ingredient");
                if (ingredientSlot == null) {
                    List<Object> src = declaredScalarSlots.isEmpty() ? scalarSlots : declaredScalarSlots;
                    ingredientSlot = src.isEmpty() ? null : src.get(0);
                }
                ItemStack first = slotDisplayToStack(ingredientSlot, ctx);
                inputs.add(first == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : first);
                if (allInputsEmpty(inputs) && !fallbackIngredients.isEmpty()) {
                    inputs.set(0, fallbackIngredients.get(0));
                }
                return new RecipePreview("furnace", inputs, out, false);
            }

            if (smithingDisplay || scalarSlots.size() >= 5) {
                List<ItemStack> inputs = new ArrayList<>();
                List<Object> src = new ArrayList<>();
                Object template = callNoArg(display, "template");
                Object base = callNoArg(display, "base");
                Object addition = callNoArg(display, "addition");
                if (template != null || base != null || addition != null) {
                    src.add(template);
                    src.add(base);
                    src.add(addition);
                } else {
                    src = declaredScalarSlots.isEmpty() ? scalarSlots : declaredScalarSlots;
                }
                for (int i = 0; i < Math.min(3, src.size()); i++) {
                    inputs.add(slotDisplayToStack(src.get(i), ctx));
                }
                if (allInputsEmpty(inputs) && !fallbackIngredients.isEmpty()) {
                    for (int i = 0; i < Math.min(3, fallbackIngredients.size()); i++) {
                        inputs.set(i, fallbackIngredients.get(i));
                    }
                }
                return new RecipePreview("smith", inputs, out, false);
            }

            if (stonecutDisplay && !scalarSlots.isEmpty()) {
                List<ItemStack> inputs = new ArrayList<>();
                Object inputSlot = callNoArg(display, "input");
                if (inputSlot == null) {
                    List<Object> src = declaredScalarSlots.isEmpty() ? scalarSlots : declaredScalarSlots;
                    inputSlot = src.isEmpty() ? null : src.get(0);
                }
                inputs.add(slotDisplayToStack(inputSlot, ctx));
                if (allInputsEmpty(inputs) && !fallbackIngredients.isEmpty()) {
                    inputs.set(0, fallbackIngredients.get(0));
                }
                return new RecipePreview("stonecut", inputs, out, false);
            }

            if (hasShape) {
                int explicitW = callNoArgInt(display, "width", -1);
                int explicitH = callNoArgInt(display, "height", -1);
                if (explicitW > 0) wh[0] = explicitW;
                if (explicitH > 0) wh[1] = explicitH;
                Object explicitIngredients = callNoArg(display, "ingredients");
                if (explicitIngredients instanceof List<?> explicitList && !explicitList.isEmpty()) {
                    listSlots = new ArrayList<>(explicitList);
                }

                int w = Math.max(1, Math.min(3, wh[0]));
                int h = Math.max(1, Math.min(3, wh[1]));
                int offX = (3 - w) / 2;
                int offY = (3 - h) / 2;

                List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, com.jamie.jamiebingo.util.ItemStackUtil.empty()));
                int limit = Math.min(listSlots.size(), w * h);
                for (int i = 0; i < limit; i++) {
                    int row = i / w;
                    int col = i % w;
                    int idx = (row + offY) * 3 + (col + offX);
                    if (idx >= 0 && idx < 9) {
                        grid.set(idx, slotDisplayToStack(listSlots.get(i), ctx));
                    }
                }
                if (allInputsEmpty(grid) && !fallbackIngredients.isEmpty()) {
                    int fbLimit = Math.min(fallbackIngredients.size(), w * h);
                    for (int i = 0; i < fbLimit; i++) {
                        int row = i / w;
                        int col = i % w;
                        int idx = (row + offY) * 3 + (col + offX);
                        if (idx >= 0 && idx < 9) {
                            grid.set(idx, fallbackIngredients.get(i));
                        }
                    }
                }
                return new RecipePreview("craft", grid, out, false);
            }

            if (shapelessDisplay || !listSlots.isEmpty()) {
                Object explicitIngredients = callNoArg(display, "ingredients");
                if (explicitIngredients instanceof List<?> explicitList && !explicitList.isEmpty()) {
                    listSlots = new ArrayList<>(explicitList);
                }
                List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, com.jamie.jamiebingo.util.ItemStackUtil.empty()));
                int limit = Math.min(9, listSlots.size());
                for (int i = 0; i < limit; i++) {
                    grid.set(i, slotDisplayToStack(listSlots.get(i), ctx));
                }
                if (allInputsEmpty(grid) && !fallbackIngredients.isEmpty()) {
                    int fbLimit = Math.min(9, fallbackIngredients.size());
                    for (int i = 0; i < fbLimit; i++) {
                        grid.set(i, fallbackIngredients.get(i));
                    }
                }
                return new RecipePreview("craft", grid, out, true);
            }

            if (!scalarSlots.isEmpty()) {
                List<ItemStack> inputs = new ArrayList<>();
                if (!scalarSlots.isEmpty()) {
                    inputs.add(slotDisplayToStack(scalarSlots.get(0), ctx));
                } else {
                    inputs.add(com.jamie.jamiebingo.util.ItemStackUtil.empty());
                }
                if (allInputsEmpty(inputs) && !fallbackIngredients.isEmpty()) {
                    inputs.set(0, fallbackIngredients.get(0));
                }
                return new RecipePreview("stonecut", inputs, out, false);
            }

            List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, com.jamie.jamiebingo.util.ItemStackUtil.empty()));
            for (int i = 0; i < Math.min(9, fallbackIngredients.size()); i++) {
                grid.set(i, fallbackIngredients.get(i));
            }
            return new RecipePreview("craft", grid, out, false);
        }
        return null;
    }

    private Object callNoArg(Object instance, String methodName) {
        if (instance == null || methodName == null || methodName.isEmpty()) return null;
        try {
            Method m = instance.getClass().getMethod(methodName);
            if (m.getParameterCount() != 0) return null;
            return m.invoke(instance);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private int callNoArgInt(Object instance, String methodName, int fallback) {
        Object out = callNoArg(instance, methodName);
        if (out instanceof Integer i) return i;
        return fallback;
    }

    private List<ItemStack> extractEntryIngredientStacks(Object entry) {
        List<ItemStack> out = new ArrayList<>();
        if (entry == null) return out;
        try {
            for (Method m : entry.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!Optional.class.isAssignableFrom(m.getReturnType())) continue;
                Object ov = m.invoke(entry);
                if (!(ov instanceof Optional<?> opt) || opt.isEmpty()) continue;
                Object inner = opt.get();
                if (!(inner instanceof List<?> list)) continue;
                for (Object ingredient : list) {
                    ItemStack sample = ingredientExampleStack(ingredient);
                    out.add(sample == null ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : sample);
                }
                return out;
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private boolean allInputsEmpty(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) return true;
        for (ItemStack s : stacks) {
            if (s != null && !s.isEmpty()) return false;
        }
        return true;
    }

    private boolean hasNoArgFloat(Object display) {
        if (display == null) return false;
        try {
            for (Method m : display.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == float.class) return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private Object extractDisplay(Object entry) {
        if (entry == null) return null;
        try {
            for (Method m : entry.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                if (!rt.contains("recipedisplay")) continue;
                if (rt.contains("recipedisplayid")) continue;
                Object out = m.invoke(entry);
                if (out == null) continue;
                String outName = out.getClass().getName().toLowerCase(Locale.ROOT);
                if (!outName.contains("recipedisplay")) continue;
                if (outName.contains("recipedisplayid")) continue;
                return out;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private int[] readDisplayWidthHeight(Object display) {
        int[] out = new int[]{1, 1};
        if (display == null) return out;
        try {
            List<Integer> ints = new ArrayList<>();
            for (java.lang.reflect.Field f : display.getClass().getDeclaredFields()) {
                if (f.getType() != int.class) continue;
                f.setAccessible(true);
                ints.add(f.getInt(display));
            }
            if (ints.size() >= 2) {
                out[0] = ints.get(0);
                out[1] = ints.get(1);
                return out;
            }
        } catch (Throwable ignored) {
        }
        try {
            List<Integer> ints = new ArrayList<>();
            for (Method m : display.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 0 || m.getReturnType() != int.class) continue;
                if ("hashCode".equals(m.getName())) continue;
                m.setAccessible(true);
                Object v = m.invoke(display);
                if (v instanceof Integer i) ints.add(i);
            }
            if (ints.size() >= 2) {
                out[0] = ints.get(0);
                out[1] = ints.get(1);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private List<Object> readSlotDisplayScalarsDeclared(Object display) {
        List<Object> out = new ArrayList<>();
        if (display == null) return out;
        try {
            for (Method m : display.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() != 0) continue;
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                if (!rt.contains("slotdisplay")) continue;
                m.setAccessible(true);
                Object v = m.invoke(display);
                if (v != null) out.add(v);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private List<Object> readSlotDisplayList(Object display) {
        List<Object> out = new ArrayList<>();
        if (display == null) return out;
        try {
            for (Method m : display.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                if (!List.class.isAssignableFrom(m.getReturnType())) continue;
                Object v = m.invoke(display);
                if (!(v instanceof List<?> list) || list.isEmpty()) continue;
                List<Object> converted = new ArrayList<>();
                boolean hasSlotish = false;
                for (Object element : list) {
                    if (element == null) {
                        converted.add(null);
                        continue;
                    }
                    if (element instanceof Optional<?> opt) {
                        Object unwrapped = opt.orElse(null);
                        converted.add(unwrapped);
                        if (unwrapped != null && unwrapped.getClass().getName().toLowerCase(Locale.ROOT).contains("slotdisplay")) {
                            hasSlotish = true;
                        }
                        continue;
                    }
                    converted.add(element);
                    if (element.getClass().getName().toLowerCase(Locale.ROOT).contains("slotdisplay")) {
                        hasSlotish = true;
                    }
                }
                if (!hasSlotish) continue;
                out.addAll(converted);
                return out;
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private List<Object> readSlotDisplayScalars(Object display) {
        List<Object> out = new ArrayList<>();
        if (display == null) return out;
        try {
            for (Method m : display.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                if (!rt.contains("slotdisplay")) continue;
                Object v = m.invoke(display);
                if (v != null) out.add(v);
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private ItemStack slotDisplayToStack(Object slotDisplay, Object contextMap) {
        if (slotDisplay == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        String className = slotDisplay.getClass().getName().toLowerCase(Locale.ROOT);

        try {
            for (Method m : slotDisplay.getClass().getMethods()) {
                if (m.getParameterCount() != 1 || contextMap == null) continue;
                if (!"resolveForFirstStack".equals(m.getName())) continue;
                if (!m.getParameterTypes()[0].isAssignableFrom(contextMap.getClass())) continue;
                Object out = m.invoke(slotDisplay, contextMap);
                if (out instanceof ItemStack s && !s.isEmpty()) return s;
            }
        } catch (Throwable ignored) {
        }

        try {
            for (Method m : slotDisplay.getClass().getMethods()) {
                if (m.getParameterCount() != 1 || contextMap == null) continue;
                if (!"resolveForStacks".equals(m.getName())) continue;
                if (!m.getParameterTypes()[0].isAssignableFrom(contextMap.getClass())) continue;
                Object out = m.invoke(slotDisplay, contextMap);
                if (out instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof ItemStack s && !s.isEmpty()) {
                    return s;
                }
            }
        } catch (Throwable ignored) {
        }

        if (className.contains("slotdisplay$composite")) {
            try {
                for (Method m : slotDisplay.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    if (!List.class.isAssignableFrom(m.getReturnType())) continue;
                    Object v = m.invoke(slotDisplay);
                    if (!(v instanceof List<?> list)) continue;
                    for (Object child : list) {
                        ItemStack out = slotDisplayToStack(child, contextMap);
                        if (out != null && !out.isEmpty()) return out;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (className.contains("slotdisplay$withremainder")) {
            try {
                for (Method m : slotDisplay.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                    if (!rt.contains("slotdisplay")) continue;
                    Object inner = m.invoke(slotDisplay);
                    ItemStack out = slotDisplayToStack(inner, contextMap);
                    if (out != null && !out.isEmpty()) return out;
                }
            } catch (Throwable ignored) {
            }
        }

        if (className.contains("slotdisplay$itemslotdisplay")) {
            try {
                for (Method m : slotDisplay.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                    if (!rt.contains("holder")) continue;
                    Object holder = m.invoke(slotDisplay);
                    if (holder == null) continue;
                    for (Method hm : holder.getClass().getMethods()) {
                        if (hm.getParameterCount() != 0) continue;
                        Object hv = hm.invoke(holder);
                        if (hv instanceof Item item) {
                            return new ItemStack(item);
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (className.contains("slotdisplay$tagslotdisplay")) {
            try {
                Object tagKey = null;
                for (Method m : slotDisplay.getClass().getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    String rt = m.getReturnType().getName().toLowerCase(Locale.ROOT);
                    if (!rt.contains("tagkey")) continue;
                    tagKey = m.invoke(slotDisplay);
                    if (tagKey != null) break;
                }
                if (tagKey != null) {
                    for (Method rm : BuiltInRegistries.ITEM.getClass().getMethods()) {
                        if (rm.getParameterCount() != 1) continue;
                        if (!rm.getParameterTypes()[0].isAssignableFrom(tagKey.getClass())) continue;
                        Object out = rm.invoke(BuiltInRegistries.ITEM, tagKey);
                        if (out instanceof Optional<?> opt && opt.isPresent()) {
                            Object named = opt.get();
                            if (named instanceof Iterable<?> iterable) {
                                for (Object holder : iterable) {
                                    if (holder == null) continue;
                                    for (Method hm : holder.getClass().getMethods()) {
                                        if (hm.getParameterCount() != 0) continue;
                                        Object hv = hm.invoke(holder);
                                        if (hv instanceof Item item) {
                                            return new ItemStack(item);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        try {
            for (Method m : slotDisplay.getClass().getMethods()) {
                if (m.getReturnType() != ItemStack.class) continue;
                if (m.getParameterCount() == 1 && contextMap != null && m.getParameterTypes()[0].isAssignableFrom(contextMap.getClass())) {
                    Object v = m.invoke(slotDisplay, contextMap);
                    if (v instanceof ItemStack s) return s;
                }
                if (m.getParameterCount() == 0) {
                    Object v = m.invoke(slotDisplay);
                    if (v instanceof ItemStack s) return s;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Method m : slotDisplay.getClass().getMethods()) {
                if (!List.class.isAssignableFrom(m.getReturnType())) continue;
                if (m.getParameterCount() == 1 && contextMap != null && m.getParameterTypes()[0].isAssignableFrom(contextMap.getClass())) {
                    Object v = m.invoke(slotDisplay, contextMap);
                    if (v instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof ItemStack s) return s;
                }
                if (m.getParameterCount() == 0) {
                    Object v = m.invoke(slotDisplay);
                    if (v instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof ItemStack s) return s;
                }
            }
        } catch (Throwable ignored) {
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private void renderRecipePreviewPanel(GuiGraphics graphics, RecipePreview preview, int mouseX, int mouseY) {
        var fontRef = this.font != null ? this.font : ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());
        if (preview == null || preview.output == null || preview.output.isEmpty() || fontRef == null) return;

        int panelW = switch (preview.mode) {
            case "craft" -> 126;
            case "smith" -> 120;
            case "stonecut" -> 92;
            case "furnace" -> 100;
            default -> 126;
        };
        int panelH = switch (preview.mode) {
            case "craft" -> 78;
            case "smith" -> 56;
            case "stonecut" -> 56;
            case "furnace" -> 62;
            default -> 78;
        };

        int x = mouseX + 12;
        int y = mouseY + 12;
        if (x + panelW > this.width) x = this.width - panelW - 4;
        if (y + panelH > this.height) y = this.height - panelH - 4;
        if (x < 4) x = 4;
        if (y < 4) y = 4;

        graphics.fill(x, y, x + panelW, y + panelH, 0xF0100010);
        graphics.fill(x, y, x + panelW, y + 1, 0x90FFFFFF);
        graphics.fill(x, y + panelH - 1, x + panelW, y + panelH, 0x90000000);
        graphics.fill(x, y, x + 1, y + panelH, 0x90FFFFFF);
        graphics.fill(x + panelW - 1, y, x + panelW, y + panelH, 0x90000000);

        graphics.drawString(fontRef, switch (preview.mode) {
            case "furnace" -> "Smelting";
            case "smith" -> "Smithing";
            case "stonecut" -> "Stonecutting";
            default -> "Crafting";
        }, x + 6, y + 4, 0xFFE0E0E0, false);

        if ("craft".equals(preview.mode)) {
            int gridX = x + 8;
            int gridY = y + 18;
            for (int i = 0; i < 9; i++) {
                int sx = gridX + (i % 3) * 18;
                int sy = gridY + (i / 3) * 18;
                drawRecipeSlot(graphics, sx, sy, i < preview.inputs.size() ? preview.inputs.get(i) : com.jamie.jamiebingo.util.ItemStackUtil.empty());
            }
            drawGuiSprite(graphics, ARROW_TEXTURE, x + 66, y + 34, 22, 15);
                drawRecipeSlot(graphics, x + 92, y + 32, preview.output);
                if (preview.shapeless) {
                    graphics.fill(x + panelW - 24, y + 4, x + panelW - 6, y + 14, 0xAA303030);
                    graphics.drawString(fontRef, "S", x + panelW - 19, y + 6, 0xFFFFFFFF, false);
                }
            } else if ("furnace".equals(preview.mode)) {
            ItemStack input = preview.inputs.isEmpty() ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : preview.inputs.get(0);
            drawRecipeSlot(graphics, x + 10, y + 24, input);
            drawGuiSprite(graphics, FURNACE_LIT_TEXTURE, x + 14, y + 43, 14, 14);
            drawGuiSprite(graphics, FURNACE_BURN_TEXTURE, x + 33, y + 28, 24, 16);
            drawRecipeSlot(graphics, x + 66, y + 24, preview.output);
        } else if ("smith".equals(preview.mode)) {
            for (int i = 0; i < 3; i++) {
                ItemStack in = i < preview.inputs.size() ? preview.inputs.get(i) : com.jamie.jamiebingo.util.ItemStackUtil.empty();
                drawRecipeSlot(graphics, x + 8 + i * 18, y + 24, in);
            }
            drawGuiSprite(graphics, ARROW_TEXTURE, x + 63, y + 27, 22, 15);
            drawRecipeSlot(graphics, x + 90, y + 24, preview.output);
        } else {
            ItemStack input = preview.inputs.isEmpty() ? com.jamie.jamiebingo.util.ItemStackUtil.empty() : preview.inputs.get(0);
            drawRecipeSlot(graphics, x + 8, y + 24, input);
            drawGuiSprite(graphics, ARROW_TEXTURE, x + 31, y + 27, 22, 15);
            drawRecipeSlot(graphics, x + 58, y + 24, preview.output);
        }
    }

    private void drawRecipeSlot(GuiGraphics graphics, int x, int y, ItemStack stack) {
        drawGuiSprite(graphics, SLOT_TEXTURE, x, y, 18, 18);
        if (stack != null && !stack.isEmpty()) {
            graphics.renderItem(stack, x + 1, y + 1);
        }
    }

    private void drawGuiSprite(GuiGraphics graphics, Identifier spriteId, int x, int y, int w, int h) {
        try {
            graphics.blit(RenderPipelines.GUI_TEXTURED, spriteId, x, y, 0, 0, w, h, w, h);
            return;
        } catch (Throwable ignored) {
        }
        graphics.fill(x, y, x + w, y + h, 0xAA000000);
    }

    private record RecipePreview(String mode, List<ItemStack> inputs, ItemStack output, boolean shapeless) {}

    public static Object buildSharedRecipePreview(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        BingoCardScreen screen = new BingoCardScreen(new BingoCard(1));
        return screen.getRecipePreview(stack);
    }

    public static void renderSharedRecipePreviewPanel(
            GuiGraphics graphics,
            Object preview,
            int mouseX,
            int mouseY,
            int screenWidth,
            int screenHeight
    ) {
        if (!(preview instanceof RecipePreview recipePreview)) return;
        BingoCardScreen screen = new BingoCardScreen(new BingoCard(1));
        screen.width = Math.max(0, screenWidth);
        screen.height = Math.max(0, screenHeight);
        screen.renderRecipePreviewPanel(graphics, recipePreview, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isNew) {
        MouseButtonEvent fixedEvent = FixedGui4ScaleUtil.virtualEvent(event, ClientMinecraftUtil.getMinecraft());
        if (super.mouseClicked(fixedEvent, isNew)) {
            return true;
        }
        int button = fixedEvent.button();
        double mouseX = fixedEvent.x();
        double mouseY = fixedEvent.y();
        if (button != 0) return false;

        SlotHit hit = findSlotHit(mouseX, mouseY);
        if (hit == null || hit.slot == null) {
            return super.mouseClicked(fixedEvent, isNew);
        }

        boolean blindHidden =
                ClientGameState.winCondition == WinCondition.BLIND
                        && !(hit.row == 0 && hit.col == 0)
                        && !ClientRevealedSlots.isRevealed(hit.slot.getId());

        boolean hangmanHidden =
                ClientGameState.winCondition == WinCondition.HANGMAN
                        && !ClientHangmanState.slotRevealed;

        if (blindHidden || hangmanHidden) {
            return true;
        }

        Set<String> local = new HashSet<>(ClientHighlightedSlots.getAll());
        if (!local.add(hit.slot.getId())) {
            local.remove(hit.slot.getId());
        }
        ClientHighlightedSlots.set(local);

        NetworkHandler.sendToServer(
                new PacketToggleHighlight(hit.slot.getId())
        );
        return true;
    }

    @Override
    protected void init() {
        applyFixedScreenSize();
        super.init();
        chatInput = addRenderableWidget(new EditBox(
                this.font,
                10,
                this.height - 26,
                Math.max(120, this.width - 20),
                16,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Chat")
        ));
        chatInput.setMaxLength(256);
        chatInput.setCanLoseFocus(false);
        chatInput.setVisible(false);
        chatInput.setFocused(false);
    }

    private void openChatInput(String initial) {
        if (chatInput == null) return;
        if (chatInputOpen) return;
        chatInputOpen = true;
        suppressNextChatChar = true;
        chatInput.setVisible(true);
        chatInput.setFocused(true);
        setFocused(chatInput);
        chatInput.setValue(initial == null ? "" : initial);
        chatInput.moveCursorToEnd(false);
    }

    private void closeChatInput() {
        if (chatInput == null) return;
        chatInputOpen = false;
        suppressNextChatChar = false;
        chatInput.setValue("");
        chatInput.setFocused(false);
        chatInput.setVisible(false);
        setFocused(null);
    }

    private void submitChatInput() {
        if (chatInput == null) return;
        String message = chatInput.getValue();
        if (message == null) {
            closeChatInput();
            return;
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            closeChatInput();
            return;
        }

        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        var player = ClientMinecraftUtil.getPlayer(mc);
        if (player == null || player.connection == null) {
            closeChatInput();
            return;
        }

        if (trimmed.startsWith("/")) {
            String command = trimmed.substring(1).trim();
            if (!command.isEmpty()) {
                player.connection.sendCommand(command);
            }
        } else {
            player.connection.sendChat(trimmed);
        }
        closeChatInput();
    }

    private void applyFixedScreenSize() {
        this.width = FixedGui4ScaleUtil.virtualWidth(this.width);
        this.height = FixedGui4ScaleUtil.virtualHeight(this.height);
    }

    private SlotHit findSlotHit(double mouseX, double mouseY) {
        int size = card.getSize();
        ClientCardLayoutSettings.CardSkin cardSkin = ClientCardLayoutSettings.fullscreen.getSkin();
        ClientCardLayoutSettings.LayoutResult layout = ClientCardLayoutSettings.computeFullscreenLayout(
                width,
                height,
                size,
                ClientGameState.winCondition == WinCondition.HANGMAN
        );

        int spacing = layout.spacing;
        int boxSize = layout.boxSize;
        int startX = layout.startX;
        int startY = layout.startY;
        for (int index = 0; index < size * size; index++) {
                int c = index % size;
                int r = index / size;
                int displayC = c;
                int displayR = r;
                int bx = startX + displayC * (boxSize + spacing);
                int by = startY + displayR * (boxSize + spacing);
                BingoSlot slot = card.getSlot(c, r);
                if (slot != null) {
                    bx += ClientFlashSlots.shakeOffsetX(slot.getId());
                    by += ClientFlashSlots.shakeOffsetY(slot.getId());
                }
                if (mouseX >= bx && mouseX <= bx + boxSize
                        && mouseY >= by && mouseY <= by + boxSize) {
                    return new SlotHit(slot, r, c);
                }
        }
        return null;
    }

    private static final class SlotHit {
        final BingoSlot slot;
        final int row;
        final int col;

        private SlotHit(BingoSlot slot, int row, int col) {
            this.slot = slot;
            this.row = row;
            this.col = col;
        }
    }

    private void drawDiagonal(
            GuiGraphics graphics,
            int x1, int y1,
            int x2, int y2,
            int t, int col
    ) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            float a = i / (float) steps;
            int x = (int) (x1 + (x2 - x1) * a);
            int y = (int) (y1 + (y2 - y1) * a);
            graphics.fill(x - t / 2, y - t / 2, x + t / 2, y + t / 2, col);
        }
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

    private void renderMineSlot(GuiGraphics graphics, int mouseX, int mouseY, int startX, int startY, int boxSize) {
        if (!ClientMineState.isActive()) return;
        List<String> mineIds = ClientMineState.sourceQuestIds();
        if (mineIds.isEmpty()) return;
        String defuseId = ClientMineState.defuseQuestId();
        boolean hasDefuse = defuseId != null && !defuseId.isBlank();
        int clusterGap = Math.max(24, boxSize / 2);

        int count = mineIds.size();
        int maxVisualHeight = Math.max(46, (int) (boxSize * 3.0f));
        int desiredGap = 2;
        int mineSize = Math.min(18, Math.max(5, (maxVisualHeight - Math.max(0, count - 1) * desiredGap) / Math.max(1, count)));
        mineSize = Math.max(5, Math.min(27, Math.round(mineSize * 1.5f)));
        int gap = count > 1
                ? Math.max(0, Math.min(desiredGap, (maxVisualHeight - count * mineSize) / (count - 1)))
                : 0;
        if (count * mineSize + Math.max(0, count - 1) * gap > maxVisualHeight) {
            mineSize = Math.max(4, (maxVisualHeight - Math.max(0, count - 1) * gap) / Math.max(1, count));
        }
        int stackHeight = count * mineSize + (count - 1) * gap;
        int between = Math.max(22, mineSize + 6);
        int mineX = startX - mineSize - Math.max(8, boxSize / 3) - clusterGap;
        int defuseXPreview = hasDefuse ? mineX - mineSize - between : mineX;
        int leftEdge = hasDefuse ? defuseXPreview : mineX;
        if (leftEdge < 2) {
            mineX += (2 - leftEdge);
        }
        int mineY = startY + Math.max(8, (boxSize * 2 - stackHeight) / 2);
        int titleY = mineY - 10;

        graphics.drawCenteredString(this.font, "Mines", mineX + mineSize / 2, titleY, 0xFFFFFFFF);
        String triggeredId = ClientMineState.triggeredQuestId();
        String progressId = ClientMineState.progressQuestId();

        for (int i = 0; i < count; i++) {
            String questId = mineIds.get(i);
            int y = mineY + i * (mineSize + gap);
            graphics.fill(mineX, y, mineX + mineSize, y + mineSize, 0xAA101010);
            graphics.fill(mineX, y, mineX + mineSize, y + 1, 0x66FFFFFF);
            graphics.fill(mineX, y + mineSize - 1, mineX + mineSize, y + mineSize, 0x66000000);
            graphics.fill(mineX, y, mineX + 1, y + mineSize, 0x66FFFFFF);
            graphics.fill(mineX + mineSize - 1, y, mineX + mineSize, y + mineSize, 0x66000000);

            BingoSlot slot = ClientMineState.slotFor(questId);
            if (slot == null) {
                slot = new BingoSlot(questId, questId, "mine", "rare");
            }
            renderMineOrDefuseIcon(graphics, slot, mineX, y, mineSize);

            if (questId.equals(triggeredId) && ClientMineState.remainingSeconds() >= 0) {
                String timer = String.valueOf(ClientMineState.remainingSeconds());
                if (mineSize >= 8) {
                    graphics.drawCenteredString(this.font, timer, mineX + mineSize / 2, y + mineSize / 2 - 4, 0xFFFF5555);
                }
            } else if (mineSize >= 11 && questId.equals(progressId) && ClientMineState.progressMax() > 0) {
                String progress = ClientMineState.progress() + "/" + ClientMineState.progressMax();
                graphics.drawCenteredString(this.font, progress, mineX + mineSize / 2, y + mineSize - 8, 0xFFFFFFFF);
            }

            if (mouseX >= mineX && mouseX <= mineX + mineSize && mouseY >= y && mouseY <= y + mineSize) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(ClientMineState.displayNameFor(questId)));
                if (questId.equals(progressId) && ClientMineState.progressMax() > 0) {
                    tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Progress: " + ClientMineState.progress() + "/" + ClientMineState.progressMax()));
                }
                if (questId.equals(triggeredId) && ClientMineState.remainingSeconds() >= 0) {
                    tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Elimination in: " + ClientMineState.remainingSeconds() + "s"));
                }
                ScreenTooltipUtil.drawComponentTooltip(
                        graphics,
                        this.font,
                        tooltip,
                        mouseX,
                        mouseY,
                        this.width,
                        this.height,
                        Math.max(180, this.width - 24)
                );
            }
        }

        if (hasDefuse) {
            int defuseX = mineX - mineSize - between;
            int defuseY = mineY + Math.max(0, (stackHeight - mineSize) / 2);
            int defuseTitleY = defuseY - 10;
            graphics.drawCenteredString(this.font, "Defuse", defuseX + mineSize / 2, defuseTitleY, 0xFF99FF99);
            graphics.fill(defuseX, defuseY, defuseX + mineSize, defuseY + mineSize, 0xAA101010);
            graphics.fill(defuseX, defuseY, defuseX + mineSize, defuseY + 1, 0x66FFFFFF);
            graphics.fill(defuseX, defuseY + mineSize - 1, defuseX + mineSize, defuseY + mineSize, 0x66000000);
            graphics.fill(defuseX, defuseY, defuseX + 1, defuseY + mineSize, 0x66FFFFFF);
            graphics.fill(defuseX + mineSize - 1, defuseY, defuseX + mineSize, defuseY + mineSize, 0x66000000);

            BingoSlot defuseSlot = ClientMineState.slotFor(defuseId);
            if (defuseSlot == null) {
                defuseSlot = new BingoSlot(defuseId, defuseId, "mine", "rare");
            }
            renderMineOrDefuseIcon(graphics, defuseSlot, defuseX, defuseY, mineSize);

            if (mouseX >= defuseX && mouseX <= defuseX + mineSize && mouseY >= defuseY && mouseY <= defuseY + mineSize) {
                List<Component> tooltip = new ArrayList<>();
                String name = ClientMineState.defuseDisplayName();
                tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(name == null || name.isBlank() ? defuseId : name));
                tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Complete this goal to defuse all active mines."));
                ScreenTooltipUtil.drawComponentTooltip(
                        graphics,
                        this.font,
                        tooltip,
                        mouseX,
                        mouseY,
                        this.width,
                        this.height,
                        Math.max(180, this.width - 24)
                );
            }
        }
    }

    private void renderPowerSlot(GuiGraphics graphics, int mouseX, int mouseY, int startX, int startY, int boxSize) {
        if (!ClientPowerSlotState.isActive()) return;
        BingoSlot power = ClientPowerSlotState.slot();
        if (power == null) return;

        int extraGap = Math.max(18, boxSize / 3);
        int size = Math.min(20, Math.max(8, Math.round(boxSize * 1.1f)));
        int x = Math.max(2, startX - size - Math.max(6, boxSize / 3) - extraGap);
        int y = startY + boxSize + Math.max(6, boxSize / 4);

        if (ClientMineState.isActive()) {
            List<String> mineIds = ClientMineState.sourceQuestIds();
            String defuseId = ClientMineState.defuseQuestId();
            boolean hasDefuse = defuseId != null && !defuseId.isBlank();
            int count = Math.max(1, mineIds.size());
            int maxVisualHeight = Math.max(46, (int) (boxSize * 3.0f));
            int desiredGap = 2;
            int mineSize = Math.min(18, Math.max(5, (maxVisualHeight - Math.max(0, count - 1) * desiredGap) / Math.max(1, count)));
            mineSize = Math.max(5, Math.min(27, Math.round(mineSize * 1.5f)));
            int gap = count > 1
                    ? Math.max(0, Math.min(desiredGap, (maxVisualHeight - count * mineSize) / (count - 1)))
                    : 0;
            if (count * mineSize + Math.max(0, count - 1) * gap > maxVisualHeight) {
                mineSize = Math.max(4, (maxVisualHeight - Math.max(0, count - 1) * gap) / Math.max(1, count));
            }
            int stackHeight = count * mineSize + (count - 1) * gap;
            int between = Math.max(22, mineSize + 6);
            int mineX = startX - mineSize - Math.max(6, boxSize / 3) - extraGap;
            int defuseXPreview = hasDefuse ? mineX - mineSize - between : mineX;
            int leftEdge = hasDefuse ? defuseXPreview : mineX;
            if (leftEdge < 2) {
                mineX += (2 - leftEdge);
            }
            int mineY = startY + Math.max(8, (boxSize * 2 - stackHeight) / 2);
            if (hasDefuse) {
                int defuseX = mineX - mineSize - between;
                int defuseY = mineY + Math.max(0, (stackHeight - mineSize) / 2);
                x = defuseX;
                int bottom = Math.max(mineY + stackHeight, defuseY + mineSize);
                y = bottom + 24;
            } else {
                x = mineX;
                y = mineY + stackHeight + 24;
            }
        }

        graphics.drawCenteredString(this.font, "power", x + size / 2, y - 10, 0xFFFFFF66);
        graphics.fill(x, y, x + size, y + size, 0xAA101010);
        graphics.fill(x, y, x + size, y + 1, 0x66FFFFFF);
        graphics.fill(x, y + size - 1, x + size, y + size, 0x66000000);
        graphics.fill(x, y, x + 1, y + size, 0x66FFFFFF);
        graphics.fill(x + size - 1, y, x + size, y + size, 0x66000000);
        renderMineOrDefuseIcon(graphics, power, x, y, size);

        int remaining = ClientPowerSlotState.remainingSeconds();
        if (ClientPowerSlotState.isClaimed()) {
            graphics.drawCenteredString(this.font, "claimed", x + size / 2, y + size + 3, 0xFFFFFFFF);
        } else if (remaining >= 0) {
            graphics.drawCenteredString(this.font, remaining + "s", x + size / 2, y + size + 3, 0xFFFFFFFF);
        }

        if (mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size) {
            List<Component> tooltip = new ArrayList<>();
            String name = ClientPowerSlotState.displayName();
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(
                    name == null || name.isBlank() ? ClientPowerSlotState.slotId() : name
            ));
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Complete this goal to trigger a buff/sabotage wheel."));
            if (ClientPowerSlotState.isClaimed()) {
                tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Status: claimed"));
            } else if (remaining >= 0) {
                tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Rerolls in: " + remaining + "s"));
            }
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY, ItemStack.EMPTY);
        }
    }

    private void renderMineOrDefuseIcon(GuiGraphics graphics, BingoSlot slot, int x, int y, int size) {
        if (slot == null) return;
        String id = slot.getId();
        if (id != null && id.startsWith("quest.")) {
            renderQuestIconSharp(graphics, x, y, size, QuestIconProvider.iconFor(slot));
            return;
        }
        ItemStack stack = getStack(slot);
        if (stack == null || stack.isEmpty()) {
            renderQuestIconSharp(graphics, x, y, size, QuestIconProvider.iconFor(slot));
            return;
        }
        int iconX = x + (size - 16) / 2;
        int iconY = y + (size - 16) / 2;
        renderItemSharp(graphics, stack, iconX, iconY);
    }

    private void renderQuestIconSharp(
            GuiGraphics graphics,
            int x,
            int y,
            int boxSize,
            com.jamie.jamiebingo.quest.icon.QuestIconData icon
    ) {
        QuestIconRenderer.renderQuestIcon(graphics, x, y, icon, boxSize);
    }

    private void renderFakeMarkers(GuiGraphics graphics, net.minecraft.client.gui.Font font, int slotCount, int startX, int startY, int boxSize, int spacing, int displayColumns) {
        for (int fakeIdx = 0; fakeIdx < slotCount; fakeIdx++) {
                boolean fakeGreen = ClientCardData.isGreenFakeSlotIndex(fakeIdx);
                boolean fakeRed = ClientCardData.isRedFakeSlotIndex(fakeIdx);
                if (!fakeGreen && !fakeRed) continue;
                int bx = startX + (fakeIdx % displayColumns) * (boxSize + spacing);
                int by = startY + (fakeIdx / displayColumns) * (boxSize + spacing);
                float fakeScale = 0.55f;
                var fakePose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
                fakePose.pushMatrix();
                fakePose.scale(fakeScale, fakeScale);
                int tx = Math.round((bx + 3) / fakeScale);
                int ty = Math.round((by + 3) / fakeScale);
                graphics.drawString(font, "fake", tx, ty, fakeGreen ? 0xFF62E87A : 0xFFEF7676, true);
                fakePose.popMatrix();
        }
    }

    private void renderItemSharp(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        graphics.renderItem(stack, x, y);
    }

    private double fixedGui4RenderScale() {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null) return 1.0d;
        int guiScale = com.jamie.jamiebingo.util.WindowUtil.getGuiScale(ClientMinecraftUtil.getWindow(mc));
        if (guiScale <= 0) return 1.0d;
        return 4.0d / guiScale;
    }
}





