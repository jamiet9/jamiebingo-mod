package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.client.LeaderboardSubmissionConfig;
import com.jamie.jamiebingo.client.SettingsLineRenderer;
import com.jamie.jamiebingo.client.WeeklyChallengeClient;
import com.jamie.jamiebingo.client.WeeklyChallengeClientState;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketStartWeeklyChallenge;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.awt.Desktop;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class WeeklyChallengeScreen extends Screen {
    private static final Identifier HIDDEN_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/slot_hidden.png");
    private static final DateTimeFormatter RESET_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault());

    private Button startButton;
    private Button refreshButton;
    private int settingsScroll;

    public WeeklyChallengeScreen() {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Weekly Challenge"));
    }

    @Override
    protected void init() {
        int right = width - 180;
        startButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Start Weekly Challenge"),
                        b -> startWeekly()
                )
                .pos(right, height - 76)
                .size(160, 20)
                .build());

        refreshButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Refresh"),
                        b -> WeeklyChallengeClient.refresh()
                )
                .pos(right, height - 54)
                .size(76, 20)
                .build());

        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Leaderboard"),
                        b -> openLeaderboard()
                )
                .pos(right + 84, height - 54)
                .size(76, 20)
                .build());

        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Close"),
                        b -> onClose()
                )
                .pos(width - 90, 10)
                .size(70, 20)
                .build());

        refreshButton.active = !WeeklyChallengeClientState.isLoading();
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        updateButtons();
    }

    private void updateButtons() {
        if (refreshButton != null) {
            refreshButton.active = !WeeklyChallengeClientState.isLoading();
        }
        if (startButton != null) {
            startButton.active = !WeeklyChallengeClientState.isLoading()
                    && WeeklyChallengeClientState.getBaseSeed() > 0L
                    && WeeklyChallengeClientState.getCard() != null
                    && WeeklyChallengeClientState.getError().isBlank();
        }
    }

    private void startWeekly() {
        long baseSeed = WeeklyChallengeClientState.getBaseSeed();
        if (baseSeed <= 0L) return;
        NetworkHandler.sendToServer(new PacketStartWeeklyChallenge(baseSeed));
        onClose();
    }

    private void openLeaderboard() {
        LeaderboardSubmissionConfig.ConfigData config = LeaderboardSubmissionConfig.load();
        if (!config.hasLeaderboardUrl()) {
            notifyAction("Leaderboard page is not configured");
            return;
        }
        String target = config.leaderboardUrl.trim();
        if (!target.contains("?")) {
            target += "?category=weekly";
        }
        try {
            URI uri = URI.create(target);
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(uri);
                    return;
                } catch (Exception ignored) {
                }
            }
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", uri.toString()).start();
                return;
            }
            notifyAction(uri.toString());
        } catch (Exception ignored) {
            notifyAction("Leaderboard page URL is not valid");
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBlurredBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);

        if (font != null) {
            graphics.drawString(font, "Weekly Challenge", 20, 16, 0xFFFFFFFF, true);
            graphics.drawString(font, buildStatusLine(), 20, 34, 0xFFBBBBBB, false);
            graphics.drawString(font, "Next reset: " + formatResetTime(WeeklyChallengeClientState.getNextResetEpochSeconds()), 20, 46, 0xFFAAAAFF, false);
            graphics.drawString(font, "Challenge ID: " + blankFallback(WeeklyChallengeClientState.getChallengeId()), 20, 58, 0xFFAAAAAA, false);
            if (!WeeklyChallengeClientState.getCardSeed().isBlank()) {
                graphics.drawString(font, trimToWidth("Card Seed: " + WeeklyChallengeClientState.getCardSeed(), width - 230), 20, height - 54, 0xFF9FDB9F, false);
                graphics.drawString(font, trimToWidth("Settings Seed: " + WeeklyChallengeClientState.getSettingsSeed(), width - 230), 20, height - 42, 0xFF9FCBFF, false);
                graphics.drawString(font, trimToWidth("World Seed: " + WeeklyChallengeClientState.getWorldSeed(), width - 230), 20, height - 30, 0xFFFFD59A, false);
            }
            if (!WeeklyChallengeClientState.getError().isBlank()) {
                graphics.drawString(font, WeeklyChallengeClientState.getError(), 20, height - 76, 0xFFFF7777, false);
            }
        }

        renderSettingsPreview(graphics);
        renderCardPreview(graphics);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Avoid double blur.
    }

    private void renderSettingsPreview(GuiGraphics graphics) {
        List<String> lines = WeeklyChallengeClientState.getSettingsLines();
        if (font == null) return;
        int x = 20;
        int y = 84;
        int cardLeft = Math.max(340, width - 360);
        int maxWidth = Math.max(180, cardLeft - x - 20);
        int maxY = height - 92;
        graphics.drawString(font, "Weekly Settings", x, y, 0xFFFFFFFF, false);
        y += 12;
        List<WrappedSettingLine> wrappedLines = buildWrappedSettingLines(lines, maxWidth);
        int lineHeight = font.lineHeight + 2;
        int visibleLineCount = Math.max(1, (maxY - y) / lineHeight);
        int maxScroll = Math.max(0, wrappedLines.size() - visibleLineCount);
        settingsScroll = Math.max(0, Math.min(settingsScroll, maxScroll));
        int drawY = y;
        for (int i = settingsScroll; i < wrappedLines.size() && drawY + lineHeight <= maxY; i++) {
            WrappedSettingLine wrapped = wrappedLines.get(i);
            if (wrapped.isContinuation()) {
                graphics.drawString(font, wrapped.text(), x + wrapped.indent(), drawY, 0xFFBBCADF, false);
            } else {
                SettingsLineRenderer.draw(graphics, font, x + wrapped.indent(), drawY, wrapped.rawLine(), false);
            }
            drawY += lineHeight;
        }
        if (maxScroll > 0) {
            graphics.drawString(font, "Scroll for more (" + (settingsScroll + 1) + "-" + Math.min(wrappedLines.size(), settingsScroll + visibleLineCount) + "/" + wrappedLines.size() + ")", x, maxY + 6, 0xFF9AA7B9, false);
        }
    }

    private List<WrappedSettingLine> buildWrappedSettingLines(List<String> lines, int maxWidth) {
        if (font == null || lines == null) return List.of();
        List<WrappedSettingLine> out = new java.util.ArrayList<>();
        for (String line : lines) {
            if (line == null || line.isBlank()) continue;
            List<FormattedCharSequence> wrapped = font.split(com.jamie.jamiebingo.util.ComponentUtil.literal(line), maxWidth);
            if (wrapped.isEmpty()) {
                out.add(new WrappedSettingLine(com.jamie.jamiebingo.util.ComponentUtil.literal(line).getVisualOrderText(), line, 0, false));
                continue;
            }
            boolean first = true;
            for (FormattedCharSequence segment : wrapped) {
                out.add(new WrappedSettingLine(segment, line, first ? 0 : 10, !first));
                first = false;
            }
        }
        return out;
    }

    private void renderCardPreview(GuiGraphics graphics) {
        BingoCard card = WeeklyChallengeClientState.getCard();
        if (card == null) return;
        int size = card.getSize();
        if (size <= 0) return;

        int padding = 20;
        int maxWidth = 300;
        int maxHeight = height - 120;
        int spacing = Math.max(2, 8 - size);
        int box = Math.max(12, Math.min(
                (maxWidth - spacing * (size - 1)) / size,
                (maxHeight - spacing * (size - 1)) / size
        ));
        int totalWidth = box * size + spacing * (size - 1);
        int startX = width - padding - totalWidth;
        int startY = 46;

        if (font != null) {
            graphics.drawString(font, "Weekly Card", startX, 28, 0xFFFFFFFF, false);
        }

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int bx = startX + x * (box + spacing);
                int by = startY + y * (box + spacing);
                BingoSlot slot = card.getSlot(x, y);

                graphics.fill(bx, by, bx + box, by + box, 0xAA000000);
                graphics.fill(bx, by, bx + box, by + 1, 0x66FFFFFF);
                graphics.fill(bx, by + box - 1, bx + box, by + box, 0x66FFFFFF);
                graphics.fill(bx, by, bx + 1, by + box, 0x66FFFFFF);
                graphics.fill(bx + box - 1, by, bx + box, by + box, 0x66FFFFFF);

                if (slot == null) continue;
                if (slot.getId().startsWith("quest.")) {
                    QuestIconRenderer.renderQuestIcon(graphics, bx, by, QuestIconProvider.iconFor(slot), box);
                    continue;
                }
                ItemStack stack = toItemStack(slot);
                if (!stack.isEmpty()) {
                    graphics.renderItem(stack, bx + (box - 16) / 2, by + (box - 16) / 2);
                } else {
                    graphics.blit(HIDDEN_TEXTURE, bx + 1, by + 1, box - 2, box - 2, 0, 0, 16, 16);
                }
            }
        }
    }

    private ItemStack toItemStack(BingoSlot slot) {
        if (slot == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        try {
            Identifier id = com.jamie.jamiebingo.util.IdUtil.id(slot.getId());
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null) {
                return new ItemStack(item);
            }
        } catch (Throwable ignored) {
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private String buildStatusLine() {
        if (WeeklyChallengeClientState.isLoading()) {
            return "Loading weekly challenge from leaderboard service...";
        }
        if (!WeeklyChallengeClientState.getError().isBlank()) {
            return "Weekly challenge unavailable";
        }
        return "Using the shared weekly seeds and board for everyone this week.";
    }

    private String formatResetTime(long epochSeconds) {
        if (epochSeconds <= 0L) return "--";
        return RESET_FORMATTER.format(Instant.ofEpochSecond(epochSeconds));
    }

    private void notifyAction(String action) {
        if (minecraft == null || minecraft.player == null) return;
        minecraft.player.displayClientMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(action), true);
    }

    private String trimToWidth(String value, int maxWidth) {
        if (font == null) return value;
        if (font.width(value) <= maxWidth) return value;
        String ellipsis = "...";
        int targetWidth = Math.max(0, maxWidth - font.width(ellipsis));
        if (targetWidth <= 0) return ellipsis;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (font.width(out.toString() + ch) > targetWidth) break;
            out.append(ch);
        }
        return out.isEmpty() ? ellipsis : out + ellipsis;
    }

    private static String blankFallback(String value) {
        return value == null || value.isBlank() ? "--" : value;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (deltaY == 0.0D) {
            return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
        }
        settingsScroll = Math.max(0, settingsScroll - (int) Math.signum(deltaY) * 3);
        return true;
    }

    private record WrappedSettingLine(FormattedCharSequence text, String rawLine, int indent, boolean isContinuation) {
    }

}
