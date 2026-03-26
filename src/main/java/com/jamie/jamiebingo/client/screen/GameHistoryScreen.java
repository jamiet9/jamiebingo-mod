package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.SettingsSeedCodec;
import com.jamie.jamiebingo.client.ClientGameHistoryStore;
import com.jamie.jamiebingo.client.ClientMinecraftUtil;
import com.jamie.jamiebingo.client.LeaderboardSubmissionClient;
import com.jamie.jamiebingo.client.LeaderboardSubmissionConfig;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketOpenGameHistory;
import com.jamie.jamiebingo.network.packet.PacketSeedPasteFinish;
import com.jamie.jamiebingo.network.packet.PacketSeedPastePart;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.registries.ForgeRegistries;

import java.awt.Desktop;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameHistoryScreen extends Screen {
    private static final long LEADERBOARD_MIN_FINISHED_AT_EPOCH_SECONDS = 1774396800L; // 2026-03-25 00:00:00 UTC
    private static final int PAGE_SIZE = 8;
    private static final int CHUNK_SIZE = 800;
    private static final Identifier HIDDEN_TEXTURE =
            com.jamie.jamiebingo.util.IdUtil.id("jamiebingo:textures/gui/slot_hidden.png");

    private final List<PacketOpenGameHistory.EntryData> entries;
    private final List<Button> entryButtons = new ArrayList<>();
    private int page = 0;
    private int selectedIndex = -1;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button copyCardSeedButton;
    private Button copyWorldSeedButton;
    private Button copySettingsSeedButton;
    private Button replayButton;
    private Button deleteEntryButton;
    private Button revealSlotsButton;
    private Button submitRunButton;
    private Button openLeaderboardButton;
    private final Set<String> revealOverrides = new HashSet<>();
    private String submissionStatus = "";
    private int submissionStatusColor = 0xFFBBBBBB;

    public GameHistoryScreen(List<PacketOpenGameHistory.EntryData> entries) {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Game History"));
        this.entries = entries == null ? List.of() : new ArrayList<>(entries);
        if (!this.entries.isEmpty()) {
            selectedIndex = 0;
        }
    }

    @Override
    protected void init() {
        applyFixedScreenSize();
        entryButtons.clear();
        int left = 20;
        int top = 36;
        int listWidth = 200;
        for (int i = 0; i < PAGE_SIZE; i++) {
            final int row = i;
            Button button = com.jamie.jamiebingo.util.ButtonUtil.builder(
                            com.jamie.jamiebingo.util.ComponentUtil.literal(""),
                            b -> {
                                int absolute = page * PAGE_SIZE + row;
                                if (absolute >= 0 && absolute < entries.size()) {
                                    selectedIndex = absolute;
                                    clearSubmissionStatus();
                                    refreshWidgets();
                                }
                            }
                    )
                    .pos(left, top + i * 22)
                    .size(listWidth, 20)
                    .build();
            if (button != null) {
                entryButtons.add(button);
                addRenderableWidget(button);
            }
        }

        int navGap = 4;
        int navWidth = (listWidth - navGap) / 2;

        prevPageButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("< Prev"),
                        b -> {
                            page = Math.max(0, page - 1);
                            refreshWidgets();
                        }
                )
                .pos(left, top + PAGE_SIZE * 22 + 4)
                .size(navWidth, 20)
                .build();
        if (prevPageButton != null) addRenderableWidget(prevPageButton);

        nextPageButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Next >"),
                        b -> {
                            int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
                            page = Math.min(totalPages - 1, page + 1);
                            refreshWidgets();
                        }
                )
                .pos(left + navWidth + navGap, top + PAGE_SIZE * 22 + 4)
                .size(navWidth, 20)
                .build();
        if (nextPageButton != null) addRenderableWidget(nextPageButton);

        int rightX = width - 250;
        copyCardSeedButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Copy Card Seed"),
                        b -> {
                            PacketOpenGameHistory.EntryData entry = selectedEntry();
                            if (entry == null) return;
                            setClipboard(entry.cardSeed);
                            notifyAction("Copied card seed");
                        }
                )
                .pos(rightX + 118, 74)
                .size(125, 20)
                .build();
        if (copyCardSeedButton != null) addRenderableWidget(copyCardSeedButton);

        copyWorldSeedButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Copy World Seed"),
                        b -> {
                            PacketOpenGameHistory.EntryData entry = selectedEntry();
                            if (entry == null) return;
                            setClipboard(entry.worldSeed);
                            notifyAction("Copied world seed");
                        }
                )
                .pos(rightX + 118, 52)
                .size(125, 20)
                .build();
        if (copyWorldSeedButton != null) addRenderableWidget(copyWorldSeedButton);

        copySettingsSeedButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Copy Settings Seed"),
                        b -> {
                            PacketOpenGameHistory.EntryData entry = selectedEntry();
                            if (entry == null) return;
                            String settingsSeed = entry.settingsSeed == null || entry.settingsSeed.isBlank()
                                    ? SettingsSeedCodec.fromCardSeed(entry.cardSeed)
                                    : entry.settingsSeed;
                            if (settingsSeed.isBlank()) return;
                            setClipboard(settingsSeed);
                            notifyAction("Copied settings seed");
                        }
                )
                .pos(rightX + 118, 96)
                .size(125, 20)
                .build();
        if (copySettingsSeedButton != null) addRenderableWidget(copySettingsSeedButton);

        replayButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Replay Card Seed"),
                        b -> {
                            PacketOpenGameHistory.EntryData entry = selectedEntry();
                            if (entry == null || entry.cardSeed.isBlank()) return;
                            sendSeed(entry.cardSeed);
                            notifyAction("Replayed card seed");
                            onClose();
                        }
                )
                .pos(width - 180, height - 30)
                .size(160, 20)
                .build();
        if (replayButton != null) addRenderableWidget(replayButton);

        deleteEntryButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Delete Entry"),
                        b -> deleteSelectedEntry()
                )
                .pos(width - 350, height - 30)
                .size(160, 20)
                .build();
        if (deleteEntryButton != null) addRenderableWidget(deleteEntryButton);

        revealSlotsButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Reveal All Slots"),
                        b -> toggleRevealSlots()
                )
                .pos(width - 520, height - 30)
                .size(160, 20)
                .build();
        if (revealSlotsButton != null) addRenderableWidget(revealSlotsButton);

        submitRunButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Submit Run"),
                        b -> submitSelectedEntry()
                )
                .pos(20, 260)
                .size(120, 20)
                .build();
        if (submitRunButton != null) addRenderableWidget(submitRunButton);

        openLeaderboardButton = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Open Leaderboard"),
                        b -> openLeaderboard()
                )
                .pos(20, 282)
                .size(120, 20)
                .build();
        if (openLeaderboardButton != null) addRenderableWidget(openLeaderboardButton);

        Button close = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Close"),
                        b -> onClose()
                )
                .pos(width - 90, 10)
                .size(70, 20)
                .build();
        if (close != null) addRenderableWidget(close);

        refreshWidgets();
    }

    private void refreshWidgets() {
        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));
        if (selectedIndex >= entries.size()) selectedIndex = entries.isEmpty() ? -1 : entries.size() - 1;
        for (int i = 0; i < entryButtons.size(); i++) {
            int absolute = page * PAGE_SIZE + i;
            Button button = entryButtons.get(i);
            if (absolute < entries.size()) {
                PacketOpenGameHistory.EntryData entry = entries.get(absolute);
                button.visible = true;
                button.active = true;
                String status = entry.completed ? "Complete" : "Failed";
                String prefix = absolute == selectedIndex ? "> " : "";
                button.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(
                        prefix + "#" + (absolute + 1) + " " + status
                                + " | " + formatDuration(entry.durationSeconds)
                                + " | " + trim(entry.cardSeed, 10)
                ));
            } else {
                button.visible = true;
                button.active = false;
                button.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(""));
            }
        }
        if (prevPageButton != null) prevPageButton.active = page > 0;
        if (nextPageButton != null) nextPageButton.active = page < totalPages - 1;
        boolean hasSelection = selectedEntry() != null;
        if (copyCardSeedButton != null) copyCardSeedButton.active = hasSelection;
        if (copyWorldSeedButton != null) copyWorldSeedButton.active = hasSelection;
        if (copySettingsSeedButton != null) copySettingsSeedButton.active = hasSelection;
        if (replayButton != null) replayButton.active = hasSelection;
        if (deleteEntryButton != null) deleteEntryButton.active = hasSelection;
        if (submitRunButton != null) {
            submitRunButton.active = hasSelection && !ClientGameHistoryStore.isSubmittedForCurrentAccount(selectedEntry());
        }
        if (openLeaderboardButton != null) {
            openLeaderboardButton.active = LeaderboardSubmissionConfig.load().hasLeaderboardUrl();
        }
        PacketOpenGameHistory.EntryData selected = selectedEntry();
        boolean maskable = isMaskableEntry(selected);
        if (revealSlotsButton != null) {
            revealSlotsButton.visible = maskable;
            revealSlotsButton.active = maskable;
            revealSlotsButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(
                    isSlotsRevealedForSelection() ? "Hide Slots" : "Reveal All Slots"
            ));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        applyFixedScreenSize();
        MenuTextureQualityUtil.ensureNearestFiltering();
        float appliedScale = FixedGuiScaleUtil.beginScaledRender(graphics, this.minecraft);
        int fixedMouseX = FixedGuiScaleUtil.virtualMouseX(mouseX, this.minecraft);
        int fixedMouseY = FixedGuiScaleUtil.virtualMouseY(mouseY, this.minecraft);
        try {
        this.renderBlurredBackground(graphics);
        super.render(graphics, fixedMouseX, fixedMouseY, partialTicks);

        if (font != null) {
            graphics.drawString(font, "Game History", 20, 14, 0xFFFFFFFF, true);
            int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
            int pageLabelY = 36 + PAGE_SIZE * 22 + 28;
            graphics.drawString(font, "Page " + (page + 1) + "/" + totalPages, 20, pageLabelY, 0xFFBBBBBB, false);
            graphics.drawString(font, "Entries: " + entries.size(), 20, pageLabelY + 12, 0xFFBBBBBB, false);
            if (!submissionStatus.isBlank()) {
                graphics.drawString(font, submissionStatus, width - 520, height - 42, submissionStatusColor, false);
            }
        }

        PacketOpenGameHistory.EntryData entry = selectedEntry();
        if (entry == null) {
            if (font != null) {
                graphics.drawString(font, "No history found yet.", width - 250, 40, 0xFFCCCCCC, false);
            }
            return;
        }

        int rightX = width - 250;
        int seedTextWidth = 106;
        int y = 40;
        if (font != null) {
            graphics.drawString(font, entry.completed ? "Result: Complete" : "Result: Failed", rightX, y, entry.completed ? 0xFF55FF55 : 0xFFFF5555, false);
            y += 12;
            graphics.drawString(font, "Duration: " + formatDuration(entry.durationSeconds), rightX, y, 0xFFDDDDDD, false);
            y += 12;
            graphics.drawString(font, "World seed:", rightX, y, 0xFFDDDDDD, false);
            y += 10;
            graphics.drawString(font, trimToWidth(entry.worldSeed, seedTextWidth), rightX, y, 0xFFAAAAFF, false);
            y += 12;
            graphics.drawString(font, "Card seed:", rightX, y, 0xFFDDDDDD, false);
            y += 10;
            graphics.drawString(font, trimToWidth(entry.cardSeed, seedTextWidth), rightX, y, 0xFFAAAAFF, false);
            y += 12;
            graphics.drawString(font, "Settings seed:", rightX, y, 0xFFDDDDDD, false);
            y += 10;
            String settingsSeed = entry.settingsSeed == null || entry.settingsSeed.isBlank()
                    ? SettingsSeedCodec.fromCardSeed(entry.cardSeed)
                    : entry.settingsSeed;
            graphics.drawString(font, trimToWidth(settingsSeed, seedTextWidth), rightX, y, 0xFFAAFFAA, false);
        }

        int cardX = width - 245;
        boolean blindMode = isBlindEntry(entry);
        boolean hideMasked = isMaskableEntry(entry) && !isSlotsRevealedForSelection();
        renderCardPreview(
                graphics,
                entry.previewCard(),
                cardX,
                130,
                220,
                200,
                hideMasked,
                blindMode,
                entry.completedSlotIds,
                entry.opponentCompletedSlotIds,
                entry.teamColorId
        );
        renderSettingsPreview(graphics, entry, 232, 40, Math.max(96, cardX - 276), Math.max(190, height - 92));
        } finally {
            FixedGuiScaleUtil.endScaledRender(graphics, appliedScale);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Prevent a second blur pass.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isNew) {
        return super.mouseClicked(FixedGuiScaleUtil.virtualEvent(event, this.minecraft), isNew);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent fixedEvent = FixedGuiScaleUtil.virtualEvent(event, this.minecraft);
        double fixedDragX = FixedGuiScaleUtil.virtualDelta(dragX, this.minecraft);
        double fixedDragY = FixedGuiScaleUtil.virtualDelta(dragY, this.minecraft);
        return super.mouseDragged(fixedEvent, fixedDragX, fixedDragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        return super.mouseReleased(FixedGuiScaleUtil.virtualEvent(event, this.minecraft));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double fixedMouseX = FixedGuiScaleUtil.virtualMouseX(mouseX, this.minecraft);
        double fixedMouseY = FixedGuiScaleUtil.virtualMouseY(mouseY, this.minecraft);
        return super.mouseScrolled(fixedMouseX, fixedMouseY, deltaX, deltaY);
    }

    private void applyFixedScreenSize() {
        if (this.minecraft == null) return;
        this.width = FixedGuiScaleUtil.virtualWidth(this.minecraft, this.width);
        this.height = FixedGuiScaleUtil.virtualHeight(this.minecraft, this.height);
    }

    private PacketOpenGameHistory.EntryData selectedEntry() {
        if (selectedIndex < 0 || selectedIndex >= entries.size()) return null;
        return entries.get(selectedIndex);
    }

    private void submitSelectedEntry() {
        PacketOpenGameHistory.EntryData entry = selectedEntry();
        ValidationResult validation = validateForSubmission(entry);
        setSubmissionStatus(validation.message(), validation.color());
        if (!validation.valid() || entry == null) {
            notifyAction(validation.message());
            return;
        }

        setSubmissionStatus("Valid: submitting...", 0xFFFFFF66);
        LeaderboardSubmissionClient.submit(entry).thenAccept(result -> {
            if (minecraft == null) return;
            minecraft.execute(() -> {
                if (result.success()) {
                    ClientGameHistoryStore.markSubmittedForCurrentAccount(entry);
                    refreshWidgets();
                }
                setSubmissionStatus(result.message(), result.success() ? 0xFF55FF55 : 0xFFFF7777);
                notifyAction(result.message());
            });
        });
    }

    private void openLeaderboard() {
        LeaderboardSubmissionConfig.ConfigData config = LeaderboardSubmissionConfig.load();
        if (!config.hasLeaderboardUrl()) {
            setSubmissionStatus("Invalid: leaderboard page is not configured", 0xFFFF7777);
            notifyAction("Invalid: leaderboard page is not configured");
            return;
        }
        try {
            URI uri = URI.create(config.leaderboardUrl.trim());
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(uri);
                    setSubmissionStatus("Opened leaderboard", 0xFF55FF55);
                    return;
                } catch (Exception ignored) {
                }
            }
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                new ProcessBuilder("cmd", "/c", "start", "", uri.toString()).start();
                setSubmissionStatus("Opened leaderboard", 0xFF55FF55);
                return;
            }
            setClipboard(uri.toString());
            setSubmissionStatus("Leaderboard URL copied to clipboard", 0xFFFFFF66);
            notifyAction("Leaderboard URL copied to clipboard");
        } catch (Exception ignored) {
            setSubmissionStatus("Invalid: leaderboard page URL is not valid", 0xFFFF7777);
            notifyAction("Invalid: leaderboard page URL is not valid");
        }
    }

    private ValidationResult validateForSubmission(PacketOpenGameHistory.EntryData entry) {
        if (entry == null) {
            return new ValidationResult(false, "Invalid: no run selected", 0xFFFF7777);
        }
        if (entry.finishedAtEpochSeconds < LEADERBOARD_MIN_FINISHED_AT_EPOCH_SECONDS) {
            return new ValidationResult(false, "Invalid: run was completed before March 25, 2026", 0xFFFF7777);
        }
        if (!entry.completed) {
            return new ValidationResult(false, "Invalid: card was not completed successfully", 0xFFFF7777);
        }
        if (entry.commandsUsed) {
            return new ValidationResult(false, "Invalid: commands or gamemode changes were used", 0xFFFF7777);
        }
        if (entry.voteRerollUsed) {
            return new ValidationResult(false, "Invalid: vote reroll unclaimed was used", 0xFFFF7777);
        }
        if (ClientGameHistoryStore.isSubmittedForCurrentAccount(entry)) {
            return new ValidationResult(false, "Invalid: this run was already submitted", 0xFFFF7777);
        }
        if (!LeaderboardSubmissionConfig.load().hasSubmitUrl()) {
            return new ValidationResult(false, "Invalid: leaderboard submit is not configured", 0xFFFF7777);
        }
        return new ValidationResult(true, "Valid: ready to submit", 0xFF55FF55);
    }

    private void clearSubmissionStatus() {
        submissionStatus = "";
        submissionStatusColor = 0xFFBBBBBB;
    }

    private void setSubmissionStatus(String message, int color) {
        submissionStatus = message == null ? "" : message;
        submissionStatusColor = color;
    }

    private void deleteSelectedEntry() {
        PacketOpenGameHistory.EntryData entry = selectedEntry();
        if (entry == null) return;
        revealOverrides.remove(entryKey(entry));
        ClientGameHistoryStore.deleteForCurrentAccount(entry);
        entries.remove(selectedIndex);
        if (entries.isEmpty()) {
            selectedIndex = -1;
            page = 0;
        } else if (selectedIndex >= entries.size()) {
            selectedIndex = entries.size() - 1;
        }
        clearSubmissionStatus();
        notifyAction("Deleted history entry");
        refreshWidgets();
    }

    private void toggleRevealSlots() {
        PacketOpenGameHistory.EntryData entry = selectedEntry();
        if (!isMaskableEntry(entry) || entry == null) return;
        String key = entryKey(entry);
        if (revealOverrides.contains(key)) {
            revealOverrides.remove(key);
        } else {
            revealOverrides.add(key);
        }
        refreshWidgets();
    }

    private boolean isSlotsRevealedForSelection() {
        PacketOpenGameHistory.EntryData entry = selectedEntry();
        if (entry == null) return false;
        return revealOverrides.contains(entryKey(entry));
    }

    private static String entryKey(PacketOpenGameHistory.EntryData entry) {
        if (entry == null) return "";
        return entry.cardSeed + "|" + entry.worldSeed + "|" + entry.finishedAtEpochSeconds + "|" + entry.durationSeconds;
    }

    private static boolean isMaskableEntry(PacketOpenGameHistory.EntryData entry) {
        return isBlindEntry(entry) || isHangmanEntry(entry);
    }

    private static boolean isBlindEntry(PacketOpenGameHistory.EntryData entry) {
        if (entry == null || entry.settingsLines == null) return false;
        for (String line : entry.settingsLines) {
            if (line == null) continue;
            if (line.trim().equalsIgnoreCase("Mode: BLIND")) return true;
        }
        return false;
    }

    private static boolean isHangmanEntry(PacketOpenGameHistory.EntryData entry) {
        if (entry == null || entry.settingsLines == null) return false;
        for (String line : entry.settingsLines) {
            if (line == null) continue;
            if (line.trim().equalsIgnoreCase("Mode: HANGMAN")) return true;
        }
        return false;
    }

    private void renderSettingsPreview(
            GuiGraphics graphics,
            PacketOpenGameHistory.EntryData entry,
            int x,
            int y,
            int width,
            int height
    ) {
        if (entry == null || font == null) return;
        graphics.drawString(font, "Settings Preview:", x, y, 0xFFFFFFFF, false);
        y += 12;
        int lineHeight = 9;
        int maxY = y + Math.max(40, height - 12);
        for (String line : entry.settingsLines) {
            if (line == null || line.isBlank()) continue;
            if (y + lineHeight > maxY) break;
            com.jamie.jamiebingo.client.SettingsLineRenderer.draw(graphics, font, x, y, trim(line, 38), false);
            y += lineHeight;
        }
    }

    private void renderCardPreview(
            GuiGraphics graphics,
            BingoCard card,
            int startX,
            int startY,
            int maxWidth,
            int maxHeight,
            boolean hidePreviewSlots,
            boolean blindMode,
            List<String> completedSlotIds,
            List<String> opponentCompletedSlotIds,
            int teamColorId
    ) {
        if (card == null) return;
        int size = card.getSize();
        if (size <= 0) return;
        Set<String> completedSet = completedSlotIds == null ? Set.of() : new HashSet<>(completedSlotIds);
        Set<String> opponentSet = opponentCompletedSlotIds == null ? Set.of() : new HashSet<>(opponentCompletedSlotIds);
        int completionFillColor = withAlpha(resolveTeamColorRgb(teamColorId), 0x66);
        int spacing = Math.max(2, 8 - size);
        int box = Math.max(12, Math.min(
                (maxWidth - spacing * (size - 1)) / size,
                (maxHeight - spacing * (size - 1)) / size
        ));

        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                int bx = startX + c * (box + spacing);
                int by = startY + r * (box + spacing);
                BingoSlot slot = card.getSlot(c, r);
                graphics.fill(bx, by, bx + box, by + box, 0xAA000000);
                graphics.fill(bx, by, bx + box, by + 1, 0x66FFFFFF);
                graphics.fill(bx, by + box - 1, bx + box, by + box, 0x66FFFFFF);
                graphics.fill(bx, by, bx + 1, by + box, 0x66FFFFFF);
                graphics.fill(bx + box - 1, by, bx + box, by + box, 0x66FFFFFF);

                if (slot == null) continue;
                if (hidePreviewSlots && !(blindMode && r == 0 && c == 0)) {
                    graphics.blit(HIDDEN_TEXTURE, bx + 1, by + 1, box - 2, box - 2, 0, 0, 16, 16);
                    if (font != null) {
                        graphics.drawCenteredString(
                                font,
                                "?",
                                bx + box / 2,
                                by + (box - font.lineHeight) / 2,
                                0xFFFFFFFF
                        );
                    }
                    continue;
                }
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

                if (completedSet.contains(slot.getId())) {
                    graphics.fill(bx + 1, by + 1, bx + box - 1, by + box - 1, completionFillColor);
                } else if (opponentSet.contains(slot.getId())) {
                    graphics.fill(bx + 1, by + 1, bx + box - 1, by + box - 1, 0x66AA2222);
                    graphics.renderItem(new ItemStack(Items.BARRIER), bx + (box - 16) / 2, by + (box - 16) / 2);
                }
            }
        }

        if (!hidePreviewSlots && !completedSet.isEmpty()) {
            renderCompletedLines(graphics, card, completedSet, startX, startY, box, spacing, teamColorId);
        }
    }

    private void renderCompletedLines(
            GuiGraphics graphics,
            BingoCard card,
            Set<String> completedSet,
            int startX,
            int startY,
            int box,
            int spacing,
            int teamColorId
    ) {
        int size = card.getSize();
        int thickness = Math.max(1, box / 12);
        int baseRgb = resolveTeamColorRgb(teamColorId);
        int rowColor = withAlpha(baseRgb, 0x22);
        int colColor = withAlpha(baseRgb, 0x22);
        int diagColor = withAlpha(baseRgb, 0x1C);

        for (int y = 0; y < size; y++) {
            if (!isRowComplete(card, completedSet, y)) continue;
            int yCenter = startY + y * (box + spacing) + box / 2;
            int x1 = startX;
            int x2 = startX + size * box + (size - 1) * spacing;
            graphics.fill(x1, yCenter - thickness, x2, yCenter + thickness + 1, rowColor);
        }
        for (int x = 0; x < size; x++) {
            if (!isColumnComplete(card, completedSet, x)) continue;
            int xCenter = startX + x * (box + spacing) + box / 2;
            int y1 = startY;
            int y2 = startY + size * box + (size - 1) * spacing;
            graphics.fill(xCenter - thickness, y1, xCenter + thickness + 1, y2, colColor);
        }
        if (isMainDiagonalComplete(card, completedSet)) {
            for (int i = 0; i < size; i++) {
                int cx = startX + i * (box + spacing) + box / 2;
                int cy = startY + i * (box + spacing) + box / 2;
                graphics.fill(cx - thickness, cy - thickness, cx + thickness + 1, cy + thickness + 1, diagColor);
            }
        }
        if (isAntiDiagonalComplete(card, completedSet)) {
            for (int i = 0; i < size; i++) {
                int cx = startX + (size - 1 - i) * (box + spacing) + box / 2;
                int cy = startY + i * (box + spacing) + box / 2;
                graphics.fill(cx - thickness, cy - thickness, cx + thickness + 1, cy + thickness + 1, diagColor);
            }
        }
    }

    private boolean isRowComplete(BingoCard card, Set<String> completedSet, int y) {
        for (int x = 0; x < card.getSize(); x++) {
            BingoSlot slot = card.getSlot(x, y);
            if (slot == null || !completedSet.contains(slot.getId())) return false;
        }
        return true;
    }

    private boolean isColumnComplete(BingoCard card, Set<String> completedSet, int x) {
        for (int y = 0; y < card.getSize(); y++) {
            BingoSlot slot = card.getSlot(x, y);
            if (slot == null || !completedSet.contains(slot.getId())) return false;
        }
        return true;
    }

    private boolean isMainDiagonalComplete(BingoCard card, Set<String> completedSet) {
        for (int i = 0; i < card.getSize(); i++) {
            BingoSlot slot = card.getSlot(i, i);
            if (slot == null || !completedSet.contains(slot.getId())) return false;
        }
        return true;
    }

    private boolean isAntiDiagonalComplete(BingoCard card, Set<String> completedSet) {
        int size = card.getSize();
        for (int i = 0; i < size; i++) {
            BingoSlot slot = card.getSlot(size - 1 - i, i);
            if (slot == null || !completedSet.contains(slot.getId())) return false;
        }
        return true;
    }

    private static int resolveTeamColorRgb(int teamColorId) {
        DyeColor color = DyeColor.byId(Math.max(0, Math.min(15, teamColorId)));
        int rgb = color.getTextColor();
        if ((rgb & 0xFFFFFF) == 0) {
            return 0xFFFFFF;
        }
        return rgb & 0xFFFFFF;
    }

    private static int withAlpha(int rgb, int alpha) {
        return ((alpha & 0xFF) << 24) | (rgb & 0xFFFFFF);
    }

    private ItemStack toItemStack(BingoSlot slot) {
        if (slot == null) return com.jamie.jamiebingo.util.ItemStackUtil.empty();
        try {
            Identifier id = com.jamie.jamiebingo.util.IdUtil.id(slot.getId());
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item != null) return new ItemStack(item);
        } catch (Throwable ignored) {
        }
        return com.jamie.jamiebingo.util.ItemStackUtil.empty();
    }

    private void setClipboard(String text) {
        var mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null) return;
        var keyboard = ClientMinecraftUtil.getKeyboardHandler(mc);
        if (keyboard == null) return;
        String value = text == null ? "" : text;
        for (Method method : keyboard.getClass().getMethods()) {
            if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != String.class) continue;
            if (!method.getName().toLowerCase().contains("clipboard")) continue;
            try {
                method.invoke(keyboard, value);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    private void sendSeed(String seed) {
        if (seed == null || seed.isBlank()) return;
        int total = (int) Math.ceil(seed.length() / (double) CHUNK_SIZE);
        for (int i = 0; i < total; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(seed.length(), start + CHUNK_SIZE);
            NetworkHandler.sendToServer(new PacketSeedPastePart(i + 1, total, seed.substring(start, end)));
        }
        NetworkHandler.sendToServer(new PacketSeedPasteFinish());
    }

    private void notifyAction(String action) {
        if (minecraft == null || minecraft.player == null) return;
        minecraft.player.displayClientMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(action), true);
    }

    private record ValidationResult(boolean valid, String message, int color) {
    }

    private static String trim(String value, int max) {
        if (value == null || value.isBlank()) return "(none)";
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String trimToWidth(String value, int maxWidth) {
        if (font == null) return trim(value, 12);
        if (value == null || value.isBlank()) return "(none)";
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
        if (out.isEmpty()) return ellipsis;
        return out + ellipsis;
    }

    private static String formatDuration(long seconds) {
        long s = Math.max(0L, seconds);
        long h = s / 3600L;
        long m = (s % 3600L) / 60L;
        long sec = s % 60L;
        if (h > 0L) return String.format("%d:%02d:%02d", h, m, sec);
        return String.format("%02d:%02d", m, sec);
    }
}
