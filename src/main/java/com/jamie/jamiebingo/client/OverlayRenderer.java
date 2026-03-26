package com.jamie.jamiebingo.client;


import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.bingo.*;
import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;

public class OverlayRenderer implements ForgeLayer {

    // ✅ Blind mode hidden slot texture
    private static final Identifier HIDDEN_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/slot_hidden.png");
    private static final int HIDDEN_TEXTURE_SIZE = 1024;

    /* ===============================
       RANDOM EFFECT HUD STATE
       =============================== */

    /** Countdown text to render (empty = hidden) */
    public static String randomEffectText = "";

    /** Ticks remaining to display (client-side visual only) */
    public static int randomEffectTicksRemaining = 0;


    /* ===============================
       HELPERS
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

    private int rarityPoints(BingoSlot slot) {
        return RarityScoreCalculator.base(slot);
    }

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

    private String formatTimerSeconds(int totalSeconds) {
        int mins = Math.max(0, totalSeconds) / 60;
        int secs = Math.max(0, totalSeconds) % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    /* ===============================
       DOUBLED SLOT DETECTION (RARITY)
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
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
    int rawWidth = com.jamie.jamiebingo.util.GuiGraphicsUtil.getGuiWidth(graphics);
    int rawHeight = com.jamie.jamiebingo.util.GuiGraphicsUtil.getGuiHeight(graphics);
    int width = FixedGui4ScaleUtil.virtualWidth(rawWidth);
    int height = FixedGui4ScaleUtil.virtualHeight(rawHeight);
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        float appliedScale = FixedGui4ScaleUtil.beginScaledRender(graphics, ClientMinecraftUtil.getMinecraft());
        try {

        ClientChatIconOverlay.render(graphics);
        ClientGameTimer.tickVisual();
        ClientMineState.tickVisual();
        ClientPowerSlotState.tickVisual();
        ClientPowerSlotWheelAnimation.tick();

  /* ===============================
   RANDOM EFFECT COUNTDOWN (TOP HUD)
   =============================== */

if (!randomEffectText.isEmpty() && randomEffectTicksRemaining > 0) {

    var font = ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());

    int textWidth = font.width(randomEffectText);
    int x = width / 2 - textWidth / 2;
    int y = 12; // higher top-center, below vanilla HUD
    graphics.drawString(font, randomEffectText, x, y, 0xFFFFFF00, true);
}

  /* ===============================
     START DELAY COUNTDOWN (TOP HUD)
     =============================== */

if (ClientStartCountdown.isActive()) {
    int countdownSeconds = ClientStartCountdown.getSecondsRemaining();
    if (countdownSeconds > 0) {
        var font = ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());
        String text = "Game starts in " + countdownSeconds + "s";
        int textWidth = font.width(text);
        int x = width / 2 - textWidth / 2;
        int y = randomEffectText.isEmpty() ? 12 : 26;
        graphics.drawString(font, text, x, y, 0xFFFFFFFF, true);
    }
}

  /* ===============================
     GAME TIMER (TOP-LEFT)
     =============================== */

if (ClientGameTimer.active) {
    var font = ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());
    String label = ClientGameTimer.countdownEnabled ? "Time Left: " : "Time: ";
    String text = label + formatTimerSeconds(ClientGameTimer.seconds);
    graphics.drawString(font, text, 8, 8, 0xFFFFFFFF, true);
    if (ClientGameTimer.rushActive) {
        String rushText = "rush until elimination " + ClientGameTimer.rushSeconds + "s";
        graphics.drawString(font, rushText, 8, 18, 0xFFFF5555, true);
    }
    if (ClientMineState.isActive() && ClientMineState.isTriggered() && ClientMineState.remainingSeconds() >= 0) {
        int mineY = ClientGameTimer.rushActive ? 28 : 18;
        String mineText = "mine until elimination " + ClientMineState.remainingSeconds() + "s";
        graphics.drawString(font, mineText, 8, mineY, 0xFFFF5555, true);
    }
}

    renderSettingsOverlay(graphics, width, height);

    /* ===============================
       HANGMAN HUD (TOP MIDDLE)
       =============================== */

    if (ClientHangmanState.active) {
        var font = ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());
        String line1 = ClientHangmanState.line1;
        String line2 = ClientHangmanState.line2;

        int baseY = randomEffectText.isEmpty() ? 12 : 26;
        int y1 = baseY + 2;
        int y2 = y1 + font.lineHeight + 2;

        if (line1 != null && !line1.isBlank()) {
            int w1 = font.width(line1);
            graphics.drawString(font, line1, (int) (width / 2f - w1 / 2f), y1, 0xFFFFFFFF, true);
        }

        if (line2 != null && !line2.isBlank()) {
            int w2 = font.width(line2);
            graphics.drawString(font, line2, (int) (width / 2f - w2 / 2f), y2, 0xFFFFFFFF, true);
        }
    }

    int wheelTop = 44;
    if (!randomEffectText.isEmpty()) wheelTop += 12;
    if (ClientStartCountdown.isActive()) wheelTop += 12;
    if (ClientHangmanState.active) wheelTop += 24;
    ClientPowerSlotWheelAnimation.render(graphics, width, wheelTop);

      if (ClientCasinoState.isActive()
              || ClientCasinoState.isFinishing()
              || ClientCasinoState.isRerollPhase()) {
          return;
      }

      renderScoreboard(graphics, width, height);

      if (!ClientEvents.cardOverlayEnabled) {
          return;
      }

    /* ===============================
       BINGO CARD HUD (CARD DEPENDENT)
       =============================== */

    if (ClientMinecraftUtil.getPlayer() == null) return;
    if (!ClientCardData.hasCard()) return;

    BingoCard card = ClientCardData.getCard();
    int size = card.getSize();
    ClientCardLayoutSettings.CardSkin cardSkin = ClientCardLayoutSettings.overlay.getSkin();
        ClientCardLayoutSettings.LayoutResult layout = ClientCardLayoutSettings.computeOverlayLayout(
                width,
                height,
                size,
                ClientGameState.winCondition == WinCondition.HANGMAN
        );

        int spacing = layout.spacing;
        int boxSize = layout.boxSize;
        int totalWidth = layout.totalWidth;
        int totalHeight = layout.totalHeight;
        int visualHeight = totalHeight + Math.max(8, boxSize / 2);
        int startX = layout.startX;
        int startY = layout.startY;
        int slotCount = size * size;

        var itemRenderer = ClientMinecraftUtil.getItemRenderer(ClientMinecraftUtil.getMinecraft());
        var font = ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());
        if (font == null) return;

        final int colStep = boxSize + spacing;
        final int rowStep = boxSize + spacing;

        DyeColor myColor = getLocalTeamColor();

        boolean showAllTeamsLines =
                ClientGameState.winCondition == WinCondition.LOCKOUT
                        || ClientGameState.winCondition == WinCondition.RARITY;

        /* ===============================
           COMPLETED SLOTS BY TEAM
           =============================== */

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

        Set<String> doubled =
                ClientGameState.winCondition == WinCondition.RARITY
                        ? computeAllDoubledSlots(card, completedByTeam)
                        : Set.of();
        CardSkinDecorRenderer.drawBoardBackdrop(
                graphics, cardSkin, startX, startY, totalWidth, visualHeight, false
        );

        /* ===============================
           DRAW CARD
           =============================== */

        for (int index = 0; index < slotCount; index++) {
                int c = index % size;
                int r = index / size;
                int displayC = c;
                int displayR = r;

                int bx = startX + displayC * colStep;
                int by = startY + displayR * rowStep;

                BingoSlot slot = card.getSlot(c, r);

                // BLIND: top-left (0,0) is ALWAYS visible
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
                    case BLOOM -> (0x4D << 24) | ownerColor.getTextColor();
                    case RUNIC -> (0x4D << 24) | ownerColor.getTextColor();
                    case CELESTIAL -> (0x4D << 24) | ownerColor.getTextColor();
                    case GLASS -> (0x33 << 24) | ownerColor.getTextColor();
                    case MINIMAL -> (0x28 << 24) | ownerColor.getTextColor();
                    case NEON -> (0x48 << 24) | ownerColor.getTextColor();
                    case LAVA -> (0x55 << 24) | ownerColor.getTextColor();
                    case CANDY -> (0x44 << 24) | ownerColor.getTextColor();
                    case TERMINAL -> (0x44 << 24) | ownerColor.getTextColor();
                    case ROYAL -> (0x45 << 24) | ownerColor.getTextColor();
                    case VOID -> (0x66 << 24) | ownerColor.getTextColor();
                    case PRISM -> (0x58 << 24) | ownerColor.getTextColor();
                    case TOXIC -> (0x5A << 24) | ownerColor.getTextColor();
                    case ICE -> (0x58 << 24) | ownerColor.getTextColor();
                    case SUNSET -> (0x5A << 24) | ownerColor.getTextColor();
                    case GLITCH -> (0x63 << 24) | ownerColor.getTextColor();
                    case CHROME -> (0x5F << 24) | ownerColor.getTextColor();
                    default -> (0x22 << 24) | ownerColor.getTextColor();
                }
                        : switch (cardSkin) {
                    case BLOOM -> 0xAA4E3144;
                    case RUNIC -> 0xAA1D2940;
                    case CELESTIAL -> 0xAA2E223C;
                    case GLASS -> 0x66456F9B;
                    case MINIMAL -> 0xCC101010;
                    case NEON -> 0xAA0A1530;
                    case LAVA -> 0xAA2A0E00;
                    case CANDY -> 0xAA2D1231;
                    case TERMINAL -> 0xAA001A08;
                    case ROYAL -> 0xAA241900;
                    case VOID -> 0xAA060018;
                    case PRISM -> 0xAA10183A;
                    case TOXIC -> 0xAA121D00;
                    case ICE -> 0xAA051B2B;
                    case SUNSET -> 0xAA2D1018;
                    case GLITCH -> 0xAA120B1A;
                    case CHROME -> 0xAA1A1E24;
                    default -> 0x88000000;
                };

                int border = ownerColor != null
                        ? switch (cardSkin) {
                    case BLOOM -> (0xCC << 24) | ownerColor.getTextColor();
                    case RUNIC -> (0xCC << 24) | ownerColor.getTextColor();
                    case CELESTIAL -> (0xCC << 24) | ownerColor.getTextColor();
                    case GLASS -> (0x77 << 24) | ownerColor.getTextColor();
                    case MINIMAL -> (0xAA << 24) | ownerColor.getTextColor();
                    case NEON -> (0xDD << 24) | ownerColor.getTextColor();
                    case LAVA -> (0xDD << 24) | ownerColor.getTextColor();
                    case CANDY -> (0xDD << 24) | ownerColor.getTextColor();
                    case TERMINAL -> (0xDD << 24) | ownerColor.getTextColor();
                    case ROYAL -> (0xDD << 24) | ownerColor.getTextColor();
                    case VOID -> (0xDD << 24) | ownerColor.getTextColor();
                    case PRISM -> (0xDD << 24) | ownerColor.getTextColor();
                    case TOXIC -> (0xDD << 24) | ownerColor.getTextColor();
                    case ICE -> (0xDD << 24) | ownerColor.getTextColor();
                    case SUNSET -> (0xDD << 24) | ownerColor.getTextColor();
                    case GLITCH -> (0xDD << 24) | ownerColor.getTextColor();
                    case CHROME -> (0xDD << 24) | ownerColor.getTextColor();
                    default -> (0x44 << 24) | ownerColor.getTextColor();
                }
                        : switch (cardSkin) {
                    case BLOOM -> 0xFFDFA1C4;
                    case RUNIC -> 0xFF89C4FF;
                    case CELESTIAL -> 0xFFD9B9FF;
                    case GLASS -> 0xAAC4E4FF;
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
                    default -> 0x88FFFFFF;
                };
                int borderThickness = cardSkin == ClientCardLayoutSettings.CardSkin.MINIMAL ? 1 : 2;
                boolean customSlotFrame = CardSkinDecorRenderer.usesCustomSlotFrame(cardSkin);

                if (!customSlotFrame) {
                    graphics.fill( bx, by, bx + boxSize, by + boxSize, bg);
                    int accent = slotAccentColor(cardSkin, displayC, displayR);
                    if (accent != 0) {
                        graphics.fill(bx + borderThickness, by + borderThickness, bx + boxSize - borderThickness, by + borderThickness + 1, accent);
                        if ((displayC + displayR) % 2 == 0) {
                            graphics.fill(bx + borderThickness, by + boxSize - borderThickness - 1, bx + boxSize - borderThickness, by + boxSize - borderThickness, accent);
                        }
                    }
                    graphics.fill( bx, by, bx + boxSize, by + borderThickness, border);
                    graphics.fill( bx, by + boxSize - borderThickness, bx + boxSize, by + boxSize, border);
                    graphics.fill( bx, by, bx + borderThickness, by + boxSize, border);
                    graphics.fill( bx + boxSize - borderThickness, by, bx + boxSize, by + boxSize, border);
                }
                CardSkinDecorRenderer.drawSlotDecoration(
                        graphics, cardSkin, bx, by, boxSize, displayC, displayR, borderThickness, border, bg, false
                );

                boolean flashing =
                        ClientFlashSlots.isFlashing(slot.getId())
                                && !blindHidden
                                && !hangmanHidden;

                if (flashing) {
                    float pulse = ClientFlashSlots.pulse(0f);
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
                    float pulse = ClientRerollAnimation.pulse(0f);
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
                    graphics.fill( bx - t, by - t, bx + boxSize + t, by, hCol);
                    graphics.fill( bx - t, by + boxSize, bx + boxSize + t, by + boxSize + t, hCol);
                    graphics.fill( bx - t, by, bx, by + boxSize, hCol);
                    graphics.fill( bx + boxSize, by, bx + boxSize + t, by + boxSize, hCol);
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

                /* ---------- BLIND / HANGMAN ---------- */
                if (blindHidden || hangmanHidden) {
                    String q = "?";
                    int qx = bx + (boxSize - font.width(q)) / 2;
                    int qy = by + (boxSize - font.lineHeight) / 2;
                    graphics.drawString(font, q, qx, qy, 0xFFFFFFFF, true);
                    continue;
                }

                // ---- render quest or item icon FIRST ----
                if (slot.getId().startsWith("quest.")) {
                    int max = ClientQuestProgressData.getMax(slot.getId());
                    int progress = ClientQuestProgressData.getProgress(slot.getId());
                    boolean completed = ClientProgressData.isCompleted(slot.getId());
                    if (max > 0 && progress > 0 && !completed) {
                        int rgb = localTeamColorARGB(0xFF);
                        float ratio = Math.min(1f, progress / (float) max);
                        renderQuestProgress(graphics, bx, by, boxSize, ratio, rgb);
                    }
                    var questIcon = QuestIconProvider.iconFor(slot);
                    renderQuestIconSharp(graphics, bx, by, boxSize, questIcon);
                } else {
                    ItemStack stack = getStack(slot);
                    if (!stack.isEmpty()) {
                        renderItemSharp(graphics, stack, bx, by, boxSize);
                    }
                }

                // ---- render barrier LAST (ABSOLUTE TOP) ----
                if (lockedOut) {
                    renderItemSharp(graphics, com.jamie.jamiebingo.util.ItemLookupUtil.stack("minecraft:barrier"), bx, by, boxSize);
                }

                {
                    String rarity = slot.getRarity();
                    String text = "";
                    if (rarity != null && rarity.length() >= 3) {
                        text = rarity.substring(0, 3).toUpperCase();
                    }
                    if (ClientGameState.winCondition == WinCondition.RARITY) {
                        int pts = rarityPoints(slot);
                        if (doubled.contains(slot.getId())) pts *= 2;
                        text = text.isEmpty() ? String.valueOf(pts) : text + " " + pts;
                    }

                    float rarityScale = 0.55f;
                    int labelHeight = Math.max(7, Math.round(font.lineHeight * rarityScale) + 4);
                    int textWidth = Math.round(font.width(text) * rarityScale);
                    int labelW = Math.min(boxSize - 4, Math.max(12, textWidth + 8));
                    int labelH = Math.min(Math.max(8, boxSize / 2 - 2), Math.min(labelHeight, Math.max(1, spacing - 2)));
                    int labelX1 = bx + (boxSize - labelW) / 2;
                    int labelY1 = by + boxSize + 1;
                    int labelX2 = labelX1 + labelW;
                    int labelY2 = labelY1 + labelH;
                    int labelBg = switch (cardSkin) {
                        case BLOOM -> 0xCC5C3A4E;
                        case RUNIC -> 0xCC22324A;
                        case CELESTIAL -> 0xCC3B2D4A;
                        case GLASS -> 0xB0253D5A;
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

                    pose.pushMatrix();
                    pose.scale(rarityScale, rarityScale);
                    int rarityTextX = Math.round((labelX1 + (labelW - textWidth) / 2f) / rarityScale);
                    int rarityTextY = Math.round((labelY1 + (labelH - Math.round(font.lineHeight * rarityScale)) / 2f) / rarityScale);
                    graphics.drawString(font, text, rarityTextX, rarityTextY, rarityColor(slot.getRarity()), true);
                    pose.popMatrix();
                }

            }
        renderMineSlot(graphics, startX, startY, boxSize);
        renderPowerSlot(graphics, startX, startY, boxSize);

        /* ===============================
           COMPLETED LINES
           =============================== */

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
                        int y = startY + line.index() * rowStep + boxSize / 2;
                        graphics.fill(
                                startX, y - thickness / 2,
                                startX + totalWidth, y + thickness / 2,
                                lineColor);
                    }
                    case COLUMN -> {
                        int x = startX + line.index() * colStep + boxSize / 2;
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
        renderFakeMarkers(graphics, ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft()), slotCount, startX, startY, boxSize, rowStep, colStep, size);
        } finally {
            FixedGui4ScaleUtil.endScaledRender(graphics, appliedScale);
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

    private void renderSettingsOverlay(GuiGraphics graphics, int width, int height) {
        if (!ClientEvents.settingsOverlayEnabled) return;
        List<String> lines = ClientSettingsOverlay.getLines();
        if (lines.isEmpty()) return;

        var font = ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());
        if (font == null) return;

        ClientCardLayoutSettings.load();
        ClientCardLayoutSettings.WidgetConfig config = ClientCardLayoutSettings.settingsOverlay;
        float scale = Math.max(ClientCardLayoutSettings.MIN_SCALE, config.scale);

        int maxWidth = 0;
        for (String line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line));
        }
        int baseLineHeight = font.lineHeight + 2;
        int contentHeight = baseLineHeight * lines.size();
        int defaultX = 8;
        int defaultY = height / 2 - Math.round(contentHeight * scale / 2f);

        ClientCardLayoutSettings.LayoutResult bounds = ClientCardLayoutSettings.resolveWidgetBounds(
                width, height, maxWidth, contentHeight, defaultX, defaultY, config
        );

        OverlayWidgetSkinRenderer.draw(graphics, config.getSkin(), bounds.startX, bounds.startY, bounds.totalWidth, bounds.totalHeight);

        int drawX = Math.round(bounds.startX / scale);
        int drawY = Math.round(bounds.startY / scale);
        var pose = com.jamie.jamiebingo.util.GuiGraphicsUtil.getPose(graphics);
        pose.pushMatrix();
        pose.scale(scale, scale);
        for (String line : lines) {
            drawSettingsLine(graphics, font, drawX, drawY, line);
            drawY += baseLineHeight;
        }
        pose.popMatrix();
    }

    private void renderScoreboard(GuiGraphics graphics, int width, int height) {
        if (!ClientEvents.scoreboardOverlayEnabled) return;
        if (!ClientTeamScoreData.hasData()) return;

        var font = ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft());
        if (font == null) return;

        List<ScoreLine> lines = new ArrayList<>();
        for (ClientTeamScoreData.TeamEntry team : ClientTeamScoreData.getTeamsSorted()) {
            String header = team.color.getName() + " Team (" + team.totalScore + ")";
            if (team.completedLines >= 0) {
                header = header + " | Lines: " + team.completedLines;
            }
            lines.add(new ScoreLine(header, team.color.getTextColor()));
            for (var entry : team.memberScores.entrySet()) {
                var level = ClientMinecraftUtil.getLevel(ClientMinecraftUtil.getMinecraft());
                Player p = level != null ? level.getPlayerByUUID(entry.getKey()) : null;
                String name = p != null
                        ? p.getName().getString()
                        : team.memberNames.getOrDefault(entry.getKey(), "Player");
                lines.add(new ScoreLine("  " + name + " - " + entry.getValue(), 0xFFFFFFFF));
            }
            lines.add(new ScoreLine("", 0x00000000));
        }
        if (lines.isEmpty()) return;

        ClientCardLayoutSettings.load();
        ClientCardLayoutSettings.WidgetConfig config = ClientCardLayoutSettings.scoreboard;
        float scale = Math.max(ClientCardLayoutSettings.MIN_SCALE, config.scale);

        int lineHeight = Math.max(8, font.lineHeight + 1);
        int maxWidth = 0;
        for (ScoreLine line : lines) {
            maxWidth = Math.max(maxWidth, font.width(line.text));
        }
        int contentHeight = lineHeight * lines.size();
        int defaultX = width - Math.round(maxWidth * scale) - 12;
        int defaultY = height - Math.round(contentHeight * scale) - 22;

        ClientCardLayoutSettings.LayoutResult bounds = ClientCardLayoutSettings.resolveWidgetBounds(
                width, height, maxWidth, contentHeight, defaultX, defaultY, config
        );

        OverlayWidgetSkinRenderer.draw(graphics, config.getSkin(), bounds.startX, bounds.startY, bounds.totalWidth, bounds.totalHeight);

        int drawX = Math.round(bounds.startX / scale);
        int drawY = Math.round(bounds.startY / scale);
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

    private record ScoreLine(String text, int color) {}

    private void drawSettingsLine(GuiGraphics graphics, net.minecraft.client.gui.Font font, int x, int y, String line) {
        SettingsLineRenderer.draw(graphics, font, x, y, line, true);
    }

    private int settingValueColor(String value) {
        return SettingsLineRenderer.valueColor(value);
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

    private void renderMineSlot(GuiGraphics graphics, int startX, int startY, int boxSize) {
        if (!ClientMineState.isActive()) return;
        List<String> mineIds = ClientMineState.sourceQuestIds();
        if (mineIds.isEmpty()) return;
        String defuseId = ClientMineState.defuseQuestId();
        boolean hasDefuse = defuseId != null && !defuseId.isBlank();
        int clusterGap = Math.max(24, boxSize / 2);

        int count = mineIds.size();
        int maxVisualHeight = Math.max(36, (int) (boxSize * 2.4f));
        int desiredGap = 2;
        int mineSize = Math.min(13, Math.max(5, (maxVisualHeight - Math.max(0, count - 1) * desiredGap) / Math.max(1, count)));
        mineSize = Math.max(5, Math.min(20, Math.round(mineSize * 1.5f)));
        int gap = count > 1
                ? Math.max(0, Math.min(desiredGap, (maxVisualHeight - count * mineSize) / (count - 1)))
                : 0;
        if (count * mineSize + Math.max(0, count - 1) * gap > maxVisualHeight) {
            mineSize = Math.max(4, (maxVisualHeight - Math.max(0, count - 1) * gap) / Math.max(1, count));
        }
        int stackHeight = count * mineSize + (count - 1) * gap;
        int between = Math.max(22, mineSize + 6);
        int mineX = startX - mineSize - Math.max(6, boxSize / 3) - clusterGap;
        int defuseXPreview = hasDefuse ? mineX - mineSize - between : mineX;
        int leftEdge = hasDefuse ? defuseXPreview : mineX;
        if (leftEdge < 2) {
            mineX += (2 - leftEdge);
        }
        int mineY = startY + Math.max(4, (boxSize * 2 - stackHeight) / 2);
        int titleY = mineY - 9;

        graphics.drawCenteredString(ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft()), "Mines", mineX + mineSize / 2, titleY, 0xFFFFFFFF);
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
                    graphics.drawCenteredString(ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft()), timer, mineX + mineSize / 2, y + mineSize / 2 - 3, 0xFFFF5555);
                }
            } else if (mineSize >= 11 && questId.equals(progressId) && ClientMineState.progressMax() > 0) {
                String progress = ClientMineState.progress() + "/" + ClientMineState.progressMax();
                graphics.drawCenteredString(ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft()), progress, mineX + mineSize / 2, y + mineSize - 7, 0xFFFFFFFF);
            }
        }
        if (hasDefuse) {
            int defuseX = mineX - mineSize - between;
            int defuseY = mineY + Math.max(0, (stackHeight - mineSize) / 2);
            int defuseTitleY = defuseY - 9;
            graphics.drawCenteredString(ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft()), "Defuse", defuseX + mineSize / 2, defuseTitleY, 0xFF99FF99);
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
        }
    }

    private void renderPowerSlot(GuiGraphics graphics, int startX, int startY, int boxSize) {
        if (!ClientPowerSlotState.isActive()) return;
        BingoSlot powerSlot = ClientPowerSlotState.slot();
        if (powerSlot == null) return;

        int extraGap = Math.max(18, boxSize / 3);
        int size = Math.min(20, Math.max(8, Math.round(boxSize * 1.1f)));
        int x = Math.max(2, startX - size - Math.max(6, boxSize / 3) - extraGap);
        int y = startY + boxSize + Math.max(6, boxSize / 4);

        if (ClientMineState.isActive()) {
            List<String> mineIds = ClientMineState.sourceQuestIds();
            String defuseId = ClientMineState.defuseQuestId();
            boolean hasDefuse = defuseId != null && !defuseId.isBlank();
            int count = Math.max(1, mineIds.size());
            int maxVisualHeight = Math.max(36, (int) (boxSize * 2.4f));
            int desiredGap = 2;
            int mineSize = Math.min(13, Math.max(5, (maxVisualHeight - Math.max(0, count - 1) * desiredGap) / count));
            mineSize = Math.max(5, Math.min(20, Math.round(mineSize * 1.5f)));
            int gap = count > 1
                    ? Math.max(0, Math.min(desiredGap, (maxVisualHeight - count * mineSize) / (count - 1)))
                    : 0;
            int stackHeight = count * mineSize + (count - 1) * gap;
            int between = Math.max(22, mineSize + 6);
            int mineX = startX - mineSize - Math.max(6, boxSize / 3) - extraGap;
            int defuseXPreview = hasDefuse ? mineX - mineSize - between : mineX;
            int leftEdge = hasDefuse ? defuseXPreview : mineX;
            if (leftEdge < 2) {
                mineX += (2 - leftEdge);
            }
            int mineY = startY + Math.max(4, (boxSize * 2 - stackHeight) / 2);
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

        graphics.drawCenteredString(ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft()), "power", x + size / 2, y - 9, 0xFFFFFF66);
        graphics.fill(x, y, x + size, y + size, 0xAA101010);
        graphics.fill(x, y, x + size, y + 1, 0x66FFFFFF);
        graphics.fill(x, y + size - 1, x + size, y + size, 0x66000000);
        graphics.fill(x, y, x + 1, y + size, 0x66FFFFFF);
        graphics.fill(x + size - 1, y, x + size, y + size, 0x66000000);
        renderMineOrDefuseIcon(graphics, powerSlot, x, y, size);

        int remaining = ClientPowerSlotState.remainingSeconds();
        if (ClientPowerSlotState.isClaimed()) {
            graphics.drawCenteredString(
                    ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft()),
                    "claimed",
                    x + size / 2,
                    y + size + 3,
                    0xFFFFFFFF
            );
        } else if (remaining >= 0) {
            graphics.drawCenteredString(
                    ClientMinecraftUtil.getFont(ClientMinecraftUtil.getMinecraft()),
                    remaining + "s",
                    x + size / 2,
                    y + size + 3,
                    0xFFFFFFFF
            );
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
        renderItemSharp(graphics, stack, x, y, size);
    }

    private void renderQuestIconSharp(
            GuiGraphics graphics,
            int x,
            int y,
            int boxSize,
            com.jamie.jamiebingo.quest.icon.QuestIconData icon
    ) {
        QuestIconRenderer.pushFixedGui4Context();
        try {
            QuestIconRenderer.renderQuestIcon(graphics, x, y, icon, boxSize);
        } finally {
            QuestIconRenderer.popFixedGui4Context();
        }
    }

    private void renderItemSharp(GuiGraphics graphics, ItemStack stack, int x, int y, int boxSize) {
        if (stack == null || stack.isEmpty()) return;
        CardSkinDecorRenderer.renderScaledItem(graphics, stack, x, y, boxSize);
    }

    private void renderFakeMarkers(GuiGraphics graphics, net.minecraft.client.gui.Font font, int slotCount, int startX, int startY, int boxSize, int rowStep, int colStep, int displayColumns) {
        for (int fakeIdx = 0; fakeIdx < slotCount; fakeIdx++) {
                boolean fakeGreen = ClientCardData.isGreenFakeSlotIndex(fakeIdx);
                boolean fakeRed = ClientCardData.isRedFakeSlotIndex(fakeIdx);
                if (!fakeGreen && !fakeRed) continue;
                int bx = startX + (fakeIdx % displayColumns) * colStep;
                int by = startY + (fakeIdx / displayColumns) * rowStep;
                float fakeScale = 0.5f;
                var fakePose = graphics.pose();
                fakePose.pushMatrix();
                fakePose.scale(fakeScale, fakeScale);
                int tx = Math.round((bx + 2) / fakeScale);
                int ty = Math.round((by + 2) / fakeScale);
                graphics.drawString(font, "fake", tx, ty, fakeGreen ? 0xFF62E87A : 0xFFEF7676, true);
                fakePose.popMatrix();
        }
    }

    private double fixedGui4RenderScale() {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null) return 1.0d;
        int guiScale = com.jamie.jamiebingo.util.WindowUtil.getGuiScale(ClientMinecraftUtil.getWindow(mc));
        if (guiScale <= 0) return 1.0d;
        return 4.0d / guiScale;
    }

}





