package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.client.ClientCustomCardState;
import com.jamie.jamiebingo.client.ClientRarityOverrideSettings;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.SlotResolver;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.client.screen.BingoControllerScreen;
import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketImportCustomSeed;
import com.jamie.jamiebingo.network.packet.PacketRequestCustomSeed;
import com.jamie.jamiebingo.network.packet.PacketSaveCustomCardState;
import com.jamie.jamiebingo.network.packet.PacketStartCustomCardGame;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import com.jamie.jamiebingo.mines.MineModeManager;
import com.jamie.jamiebingo.data.ItemDatabase;
import com.jamie.jamiebingo.ItemDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CustomCardMakerScreen extends Screen {
    private static final int PALETTE_TOP_OFFSET = 96;
    private static final int MINE_PANEL_W = 208;
    private static final int MINE_PANEL_H = 244;
    private static final int MINE_ROW_HEIGHT = 16;
    private static final int MINE_BOX_SIZE = 12;

    private static final int GRID_SIZE = 10;
    private static final int GRID_SLOT = 18;
    private static final int GRID_GAP = 2;
    private static final int PALETTE_COLS = 7;
    private static final int PALETTE_ROWS = 6;

    private final List<String> cardSlots = new ArrayList<>();
    private final List<String> poolSlots = new ArrayList<>();
    private boolean poolMode = false;
    private final List<List<String>> poolPages = new ArrayList<>();
    private int poolPageIndex = 0;

    private final List<PaletteEntry> paletteAll = new ArrayList<>();
    private final List<PaletteEntry> paletteFiltered = new ArrayList<>();
    private int paletteScroll = 0;
    private PaletteEntry selected = null;
    private String lastSearchValue = "";
    private boolean draggingScroll = false;
    private int scrollDragOffset = 0;
    private int focusMode = 0;
    private int rarityFilterIndex = 0;
    private int categoryFilterIndex = 0;

    private EditBox searchBox;
    private EditBox seedBox;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button poolToggleButton;
    private Button settingsButton;
    private Button copySeedButton;
    private Button clearCardButton;
    private Button startGameButton;
    private Button importSeedButton;
    private Button focusButton;
    private Button filterRarityButton;
    private Button filterCategoryButton;
    private Button addMinesButton;
    private Button applyMinesSelectionButton;
    private Button closeMinesSelectionButton;
    private boolean minePickerOpen = false;
    private final List<MinePickEntry> minePickEntries = new ArrayList<>();

    public CustomCardMakerScreen() {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Custom Card Maker"));
    }

    @Override
    protected void init() {
        applyFixedScreenSize();
        ClientCustomCardState.ensureCardSlots(GRID_SIZE * GRID_SIZE);
        cardSlots.clear();
        cardSlots.addAll(ClientCustomCardState.customCardSlots);
        poolSlots.clear();
        poolSlots.addAll(ClientCustomCardState.customPoolIds);
        minePickEntries.clear();
        Set<String> selectedMineIds = new LinkedHashSet<>(ClientCustomCardState.customMineIds);
        for (MineModeManager.MineDef def : MineModeManager.definitionsForAmount(13)) {
            minePickEntries.add(new MinePickEntry(def.sourceQuestId(), def.displayName(), selectedMineIds.contains(def.sourceQuestId())));
        }
        rebuildPoolPagesFromState();
        poolMode = ClientCustomCardState.customPoolEnabled;

        ensureSize(cardSlots, GRID_SIZE * GRID_SIZE);
        ensureSize(poolSlots, GRID_SIZE * GRID_SIZE);

        int left = 20;
        int top = 24;
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;

        searchBox = new EditBox(font, paletteX, top + 2, 156, 16, com.jamie.jamiebingo.util.ComponentUtil.literal("Search"));
        searchBox.setValue("");
        searchBox.setResponder(s -> applyFilter());
        searchBox.setCanLoseFocus(true);
        addRenderableWidget(searchBox);
        addRenderableWidget(cycleButton(paletteX, top + 22, "<", () -> cycleFocus(-1)));
        focusButton = addRenderableWidget(labelButton(paletteX + 20, top + 22, 128, focusLabel()));
        addRenderableWidget(cycleButton(paletteX + 150, top + 22, ">", () -> cycleFocus(1)));
        addRenderableWidget(cycleButton(paletteX, top + 40, "<", () -> cycleRarityFilter(-1)));
        filterRarityButton = addRenderableWidget(labelButton(paletteX + 20, top + 40, 128, rarityFilterLabel()));
        addRenderableWidget(cycleButton(paletteX + 150, top + 40, ">", () -> cycleRarityFilter(1)));
        addRenderableWidget(cycleButton(paletteX, top + 58, "<", () -> cycleCategoryFilter(-1)));
        filterCategoryButton = addRenderableWidget(labelButton(paletteX + 20, top + 58, 128, categoryFilterLabel()));
        addRenderableWidget(cycleButton(paletteX + 150, top + 58, ">", () -> cycleCategoryFilter(1)));

        int btnW = 88;
        int btnH = 18;
        int gap = 6;
        int row1Y = height - 48;
        int row2Y = height - 26;
        int pageRowY = row1Y - 20;

        seedBox = new EditBox(font, left - 6 + (btnW + gap) * 2, row2Y, 240, 16, com.jamie.jamiebingo.util.ComponentUtil.literal("Seed"));
        seedBox.setValue("");
        seedBox.setMaxLength(100000);
        seedBox.setCanLoseFocus(true);
        addRenderableWidget(seedBox);

        poolToggleButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(poolMode ? "Pool: ON" : "Pool: OFF"),
                        b -> {
                            List<String> snapshot = new ArrayList<>(poolMode ? poolSlots : cardSlots);
                            saveCurrentSlots();
                            poolMode = !poolMode;
                            b.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(poolMode ? "Pool: ON" : "Pool: OFF"));
                            applySnapshotToActive(snapshot);
                        }
                )
                .pos(left - 6, row1Y)
                .size(btnW, btnH)
                .build());

        settingsButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Settings"),
                        b -> openSettings()
                )
                .pos(left - 6 + (btnW + gap), row1Y)
                .size(btnW, btnH)
                .build());

        copySeedButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Copy Seed"),
                        b -> requestSeed()
                )
                .pos(left - 6 + (btnW + gap) * 2, row1Y)
                .size(btnW, btnH)
                .build());

        clearCardButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Clear Card"),
                        b -> clearActiveGrid()
                )
                .pos(left - 6 + (btnW + gap) * 3, row1Y)
                .size(btnW, btnH)
                .build());

        startGameButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Start Game"),
                        b -> startCustomGame()
                )
                .pos(left - 6 + (btnW + gap) * 4, row1Y)
                .size(btnW, btnH)
                .build());

        importSeedButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Import Seed"),
                        b -> importSeedFromBox()
                )
                .pos(left - 6, row2Y)
                .size(btnW + 20, btnH)
                .build());

        prevPageButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("< Page"),
                        b -> changePoolPage(-1)
                )
                .pos(left - 6 + (btnW + gap) * 3, pageRowY)
                .size(64, btnH)
                .build());
        nextPageButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Page >"),
                        b -> changePoolPage(1)
                )
                .pos(left - 6 + (btnW + gap) * 3 + 70, pageRowY)
                .size(64, btnH)
                .build());
        addMinesButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Add Mines"),
                        b -> openMinePicker()
                )
                .pos(left - 6 + (btnW + gap) * 3 + 140, pageRowY)
                .size(78, btnH)
                .build());
        applyMinesSelectionButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Apply Mines"),
                        b -> applySelectedMinesToActiveGrid()
                )
                .pos(left - 6 + (btnW + gap) * 3 + 140, pageRowY - 22)
                .size(78, btnH)
                .build());
        closeMinesSelectionButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Close"),
                        b -> minePickerOpen = false
                )
                .pos(left - 6 + (btnW + gap) * 3 + 224, pageRowY - 22)
                .size(64, btnH)
                .build());

        rebuildPalette();
        super.init();

    }

    private void ensureSize(List<String> list, int size) {
        while (list.size() < size) {
            list.add("");
        }
        if (list.size() > size) {
            list.subList(size, list.size()).clear();
        }
    }

    private void rebuildPalette() {
        paletteAll.clear();
        ItemDatabase.load();

        // ---------- ITEMS (client-safe) ----------
        int itemCount = 0;
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;

            // Skip air
            if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:air")) continue;

            ItemDefinition def = ItemDatabase.getRawById(id.toString());
            paletteAll.add(new PaletteEntry(
                    id.toString(),
                    item.getDescriptionId(),
                    false,
                    def == null ? "unknown" : def.category(),
                    effectivePaletteRarity(id.toString(), def == null ? "unknown" : def.rarity())
            ));
            itemCount++;
        }

        if (itemCount == 0) {
            ItemDatabase.load();
            for (ItemDefinition def : ItemDatabase.getAllItems()) {
                if (def == null || def.id() == null || def.id().isBlank()) continue;
                paletteAll.add(new PaletteEntry(
                        def.id(),
                        def.name() == null ? def.id() : def.name(),
                        false,
                        def.category(),
                        effectivePaletteRarity(def.id(), def.rarity())
                ));
            }
        }

        // ---------- QUESTS ----------
        QuestDatabase.load();
        for (QuestDefinition q : QuestDatabase.getQuests()) {
            if (q == null || q.id == null) continue;
            paletteAll.add(new PaletteEntry(q.id, q.name, true, q.category, effectivePaletteRarity(q.id, q.rarity)));
        }

        applyFilter();
    }

    private void applyFilter() {
        paletteFiltered.clear();
        String queryRaw = searchBox.getValue().toLowerCase(Locale.ROOT);
        String query = normalizeSearch(queryRaw);
        for (PaletteEntry entry : paletteAll) {
            String name = normalizeSearch(entry.name);
            String id = normalizeSearch(entry.id.replace('_', ' '));
            if (!query.isBlank() && !name.contains(query) && !id.contains(query)) continue;
            if (focusMode == 1 && !entry.quest) continue;
            if (focusMode == 2 && entry.quest) continue;
            String rarity = currentRarityFilter();
            if (!"off".equals(rarity) && !rarity.equalsIgnoreCase(entry.rarity)) continue;
            String category = currentCategoryFilter();
            if (!"off".equals(category) && !category.equalsIgnoreCase(entry.category)) continue;
            paletteFiltered.add(entry);
        }
        paletteScroll = 0;
        lastSearchValue = searchBox.getValue();
        updateFilterButtons();
    }

    private String normalizeSearch(String value) {
        if (value == null) return "";
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private void saveCurrentSlots() {
        ClientCustomCardState.customPoolEnabled = poolMode;
        ClientCustomCardState.customCardEnabled = !poolMode;

        ClientCustomCardState.customCardSlots.clear();
        ClientCustomCardState.customCardSlots.addAll(cardSlots);

        if (poolMode) {
            ensurePoolPageIndex();
            poolPages.set(poolPageIndex, new ArrayList<>(poolSlots));
        }
        savePoolPagesToState();
    }

    private void loadCurrentSlots() {
        if (poolMode) {
            ensurePoolPageIndex();
            poolSlots.clear();
            poolSlots.addAll(poolPages.get(poolPageIndex));
            ensureSize(poolSlots, GRID_SIZE * GRID_SIZE);
        } else {
            cardSlots.clear();
            cardSlots.addAll(ClientCustomCardState.customCardSlots);
            ensureSize(cardSlots, GRID_SIZE * GRID_SIZE);
        }
    }

    private void applySnapshotToActive(List<String> snapshot) {
        if (snapshot == null) return;
        if (poolMode) {
            ensurePoolPageIndex();
            poolSlots.clear();
            poolSlots.addAll(snapshot);
            ensureSize(poolSlots, GRID_SIZE * GRID_SIZE);
            poolPages.set(poolPageIndex, new ArrayList<>(poolSlots));
        } else {
            cardSlots.clear();
            cardSlots.addAll(snapshot);
            ensureSize(cardSlots, GRID_SIZE * GRID_SIZE);
        }
    }

    private void clearActiveGrid() {
        List<String> active = poolMode ? poolSlots : cardSlots;
        for (int i = 0; i < active.size(); i++) {
            active.set(i, "");
        }
        if (poolMode) {
            ensurePoolPageIndex();
            poolPages.set(poolPageIndex, new ArrayList<>(poolSlots));
        }
    }

    private void openSettings() {
        saveCurrentSlots();
        ControllerSettingsSnapshot snapshot = ClientCustomCardState.settings;
        if (snapshot == null) {
            return;
        }

        BingoControllerScreen.openWithStateForSettings(
                snapshot.win(),
                snapshot.questMode() == 1
                        ? com.jamie.jamiebingo.bingo.CardComposition.HYBRID_CATEGORY
                        : snapshot.questMode() == 2
                        ? com.jamie.jamiebingo.bingo.CardComposition.HYBRID_PERCENT
                        : com.jamie.jamiebingo.bingo.CardComposition.CLASSIC_ONLY,
                snapshot.questPercent(),
                snapshot.categoryLogicEnabled(),
                snapshot.rarityLogicEnabled(),
                snapshot.itemColorVariantsSeparate(),
                snapshot.casino(),
                snapshot.casinoMode(),
                snapshot.rerollsMode(),
                snapshot.rerollsCount(),
                snapshot.gunRounds(),
                snapshot.hangmanRounds(),
                snapshot.hangmanBaseSeconds(),
                snapshot.hangmanPenaltySeconds(),
                snapshot.cardDifficulty(),
                "random".equalsIgnoreCase(snapshot.cardDifficulty()),
                snapshot.gameDifficulty(),
                "random".equalsIgnoreCase(snapshot.gameDifficulty()),
                snapshot.effectsInterval(),
                false,
                snapshot.rtpEnabled(),
                snapshot.randomRtp(),
                snapshot.hostileMobsEnabled(),
                snapshot.randomHostileMobs(),
                snapshot.hungerEnabled(),
                snapshot.naturalRegenEnabled(),
                snapshot.randomNaturalRegen(),
                snapshot.randomHunger(),
                snapshot.cardSize(),
                snapshot.randomCardSize(),
                snapshot.keepInventoryEnabled(),
                snapshot.randomKeepInventory(),
                snapshot.hardcoreEnabled(),
                snapshot.randomHardcore(),
                snapshot.daylightMode(),
                snapshot.randomDaylight(),
                snapshot.startDelaySeconds(),
                snapshot.countdownEnabled(),
                snapshot.countdownMinutes(),
                snapshot.rushEnabled(),
                snapshot.rushSeconds(),
                snapshot.allowLateJoin(),
                snapshot.pvpEnabled(),
                snapshot.adventureMode(),
                snapshot.prelitPortalsMode(),
                snapshot.randomPvp(),
                snapshot.registerMode(),
                snapshot.randomRegister(),
                snapshot.teamSyncEnabled(),
                snapshot.teamChestEnabled(),
                snapshot.shuffleMode(),
                snapshot.starterKitMode(),
                snapshot.hideGoalDetailsInChat(),
                snapshot.minesEnabled(),
                snapshot.mineAmount(),
                snapshot.mineTimeSeconds(),
                snapshot.powerSlotEnabled(),
                snapshot.powerSlotIntervalSeconds(),
                newSnap -> {
                    ClientCustomCardState.settings = newSnap;
                    com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(
                            com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft(),
                            this
                    );
                }
        );
        com.jamie.jamiebingo.client.ClientMinecraftUtil.setScreen(
                com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft(),
                new BingoControllerScreen()
        );
    }

    private void importSeedFromBox() {
        String seed = seedBox.getValue();
        if (seed == null || seed.isBlank()) {
            var mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
            var keyboard = com.jamie.jamiebingo.client.ClientMinecraftUtil.getKeyboardHandler(mc);
            if (keyboard != null) {
                seed = keyboard.getClipboard();
            }
        }
        if (seed == null || seed.isBlank()) return;
        NetworkHandler.sendToServer(new PacketImportCustomSeed(seed));
    }

    private void requestSeed() {
        saveCurrentSlots();
        ControllerSettingsSnapshot snapshot = ClientCustomCardState.settings;
        if (snapshot == null) return;

        List<String> card = new ArrayList<>(ClientCustomCardState.customCardSlots);
        List<String> pool = new ArrayList<>(ClientCustomCardState.customPoolIds);
        List<String> mines = selectedMineIds();

        NetworkHandler.sendToServer(new PacketRequestCustomSeed(
                snapshot.win(),
                snapshot.questMode(),
                snapshot.questPercent(),
                snapshot.categoryLogicEnabled(),
                snapshot.rarityLogicEnabled(),
                snapshot.itemColorVariantsSeparate(),
                snapshot.casino(),
                snapshot.rerollsMode(),
                snapshot.rerollsCount(),
                snapshot.gunRounds(),
                snapshot.hangmanRounds(),
                snapshot.hangmanBaseSeconds(),
                snapshot.hangmanPenaltySeconds(),
                snapshot.cardDifficulty(),
                snapshot.gameDifficulty(),
                snapshot.effectsInterval(),
                snapshot.rtpEnabled(),
                snapshot.randomRtp(),
                snapshot.hostileMobsEnabled(),
                snapshot.randomHostileMobs(),
                snapshot.hungerEnabled(),
                snapshot.randomHunger(),
                snapshot.cardSize(),
                snapshot.randomCardSize(),
                snapshot.keepInventoryEnabled(),
                snapshot.randomKeepInventory(),
                snapshot.hardcoreEnabled(),
                snapshot.randomHardcore(),
                snapshot.daylightMode(),
                snapshot.randomDaylight(),
                snapshot.startDelaySeconds(),
                snapshot.countdownEnabled(),
                snapshot.countdownMinutes(),
                snapshot.rushEnabled(),
                snapshot.rushSeconds(),
                snapshot.allowLateJoin(),
                snapshot.pvpEnabled(),
                snapshot.adventureMode(),
                snapshot.randomPvp(),
                snapshot.registerMode(),
                snapshot.teamSyncEnabled(),
                snapshot.shuffleMode(),
                snapshot.starterKitMode(),
                snapshot.hideGoalDetailsInChat(),
                ClientCustomCardState.customCardEnabled,
                ClientCustomCardState.customPoolEnabled,
                card,
                pool,
                mines
        ));
    }

    private void startCustomGame() {
        saveCurrentSlots();
        ControllerSettingsSnapshot snapshot = ClientCustomCardState.settings;
        if (snapshot == null) return;

        List<String> card = new ArrayList<>(ClientCustomCardState.customCardSlots);
        List<String> pool = new ArrayList<>(ClientCustomCardState.customPoolIds);
        List<String> mines = selectedMineIds();

        NetworkHandler.sendToServer(new PacketStartCustomCardGame(
                snapshot.win(),
                snapshot.questMode(),
                snapshot.questPercent(),
                snapshot.categoryLogicEnabled(),
                snapshot.rarityLogicEnabled(),
                snapshot.itemColorVariantsSeparate(),
                snapshot.casino(),
                snapshot.casinoMode(),
                snapshot.rerollsMode(),
                snapshot.rerollsCount(),
                snapshot.gunRounds(),
                snapshot.hangmanRounds(),
                snapshot.hangmanBaseSeconds(),
                snapshot.hangmanPenaltySeconds(),
                snapshot.cardDifficulty(),
                snapshot.gameDifficulty(),
                snapshot.effectsInterval(),
                snapshot.rtpEnabled(),
                snapshot.randomRtp(),
                snapshot.hostileMobsEnabled(),
                snapshot.randomHostileMobs(),
                snapshot.hungerEnabled(),
                snapshot.randomHunger(),
                snapshot.cardSize(),
                snapshot.randomCardSize(),
                snapshot.keepInventoryEnabled(),
                snapshot.randomKeepInventory(),
                snapshot.hardcoreEnabled(),
                snapshot.randomHardcore(),
                snapshot.daylightMode(),
                snapshot.randomDaylight(),
                snapshot.startDelaySeconds(),
                snapshot.countdownEnabled(),
                snapshot.countdownMinutes(),
                snapshot.rushEnabled(),
                snapshot.rushSeconds(),
                snapshot.allowLateJoin(),
                snapshot.pvpEnabled(),
                snapshot.adventureMode(),
                snapshot.randomPvp(),
                snapshot.registerMode(),
                snapshot.teamSyncEnabled(),
                snapshot.shuffleMode(),
                snapshot.starterKitMode(),
                snapshot.hideGoalDetailsInChat(),
                snapshot.minesEnabled(),
                snapshot.mineAmount(),
                snapshot.mineTimeSeconds(),
                ClientCustomCardState.customCardEnabled,
                ClientCustomCardState.customPoolEnabled,
                card,
                pool,
                mines
        ));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        applyFixedScreenSize();
        MenuTextureQualityUtil.ensureNearestFiltering();
        float appliedScale = FixedGuiScaleUtil.beginScaledRender(graphics, this.minecraft);
        int fixedMouseX = FixedGuiScaleUtil.virtualMouseX(mouseX, this.minecraft);
        int fixedMouseY = FixedGuiScaleUtil.virtualMouseY(mouseY, this.minecraft);
        try {
        if (this.children().isEmpty()) {
            // Launcher-safe: ensure init runs even if Minecraft#setScreen skipped it.
            this.init(this.width, this.height);
        }
        renderBlurredBackground(graphics);
        int left = 20;
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int titleCenter = left + gridWidth / 2;
        graphics.drawCenteredString(font, "Custom Card Maker", titleCenter, 8, 0xFFFFFF);

        renderGrid(graphics, fixedMouseX, fixedMouseY);
        renderPalette(graphics, fixedMouseX, fixedMouseY);
        renderScrollBar(graphics);
        renderMinePicker(graphics, fixedMouseX, fixedMouseY);
        renderTooltips(graphics, fixedMouseX, fixedMouseY);
        updatePageButtons();

        super.render(graphics, fixedMouseX, fixedMouseY, partialTick);
        } finally {
            FixedGuiScaleUtil.endScaledRender(graphics, appliedScale);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Avoid double-blur from Screen.render calling renderBackground again.
    }

    private void renderGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = 20;
        int top = 24;

        List<String> active = poolMode ? poolSlots : cardSlots;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                int idx = r * GRID_SIZE + c;
                int x = left + c * (GRID_SLOT + GRID_GAP);
                int y = top + r * (GRID_SLOT + GRID_GAP);
                graphics.fill( x, y, x + GRID_SLOT, y + GRID_SLOT, 0xFF202020);

                String id = idx < active.size() ? active.get(idx) : "";
                if (id != null && !id.isBlank()) {
                    renderSlotIcon(graphics, x, y, id);
                }
            }
        }
    }

    private void renderPalette(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = 20;
        int top = paletteTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int paletteY = top;

        int startIndex = paletteScroll * PALETTE_COLS;
        int perPage = PALETTE_COLS * PALETTE_ROWS;

        for (int i = 0; i < perPage; i++) {
            int idx = startIndex + i;
            if (idx >= paletteFiltered.size()) break;
            int col = i % PALETTE_COLS;
            int row = i / PALETTE_COLS;
            int x = paletteX + col * (GRID_SLOT + GRID_GAP);
            int y = paletteY + row * (GRID_SLOT + GRID_GAP);

            PaletteEntry entry = paletteFiltered.get(idx);
            graphics.fill( x, y, x + GRID_SLOT, y + GRID_SLOT, 0xFF202020);
            renderSlotIcon(graphics, x, y, entry.id);

            if (entry == selected) {
                graphics.fill( x - 1, y - 1, x + GRID_SLOT + 1, y, 0xAAFFD700);
                graphics.fill( x - 1, y + GRID_SLOT, x + GRID_SLOT + 1, y + GRID_SLOT + 1, 0xAAFFD700);
                graphics.fill( x - 1, y, x, y + GRID_SLOT, 0xAAFFD700);
                graphics.fill( x + GRID_SLOT, y, x + GRID_SLOT + 1, y + GRID_SLOT, 0xAAFFD700);
            }
        }
    }

    private void renderScrollBar(GuiGraphics graphics) {
        int left = 20;
        int top = paletteTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int paletteWidth = PALETTE_COLS * GRID_SLOT + (PALETTE_COLS - 1) * GRID_GAP;
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;

        int trackX = paletteX + paletteWidth + 6;
        int trackY = top;
        int trackW = 6;
        int trackH = paletteHeight;

        graphics.fill( trackX, trackY, trackX + trackW, trackY + trackH, 0xFF1A1A1A);

        int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
        int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
        if (maxScroll <= 0) {
            graphics.fill( trackX + 1, trackY + 1, trackX + trackW - 1, trackY + trackH - 1, 0xFF3A3A3A);
            return;
        }

        float visibleRatio = Math.min(1.0f, PALETTE_ROWS / (float) maxRows);
        int handleH = Math.max(10, (int) ((trackH - 2) * visibleRatio));
        int maxHandleY = trackH - 2 - handleH;
        int handleY = trackY + 1 + Math.round(maxHandleY * (paletteScroll / (float) maxScroll));
        graphics.fill( trackX + 1, handleY, trackX + trackW - 1, handleY + handleH, 0xFF6A6A6A);
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (minePickerOpen) return;
        String hoveredId = getHoveredGridId(mouseX, mouseY);
        if (hoveredId == null) {
            hoveredId = getHoveredPaletteId(mouseX, mouseY);
        }

        if (hoveredId != null) {
            List<Component> tooltip = buildEntryTooltip(hoveredId);
            if (!tooltip.isEmpty()) {
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
                return;
            }
        }

        if (poolToggleButton != null && poolToggleButton.isMouseOver(mouseX, mouseY)) {
            ScreenTooltipUtil.drawComponentTooltip(graphics, this.font, List.of(com.jamie.jamiebingo.util.ComponentUtil.literal("Toggle pool mode")), mouseX, mouseY, this.width, this.height, 240);
        } else if (settingsButton != null && settingsButton.isMouseOver(mouseX, mouseY)) {
            ScreenTooltipUtil.drawComponentTooltip(graphics, this.font, List.of(com.jamie.jamiebingo.util.ComponentUtil.literal("Edit settings")), mouseX, mouseY, this.width, this.height, 240);
        } else if (copySeedButton != null && copySeedButton.isMouseOver(mouseX, mouseY)) {
            ScreenTooltipUtil.drawComponentTooltip(graphics, this.font, List.of(com.jamie.jamiebingo.util.ComponentUtil.literal("Copy seed to chat")), mouseX, mouseY, this.width, this.height, 240);
        } else if (startGameButton != null && startGameButton.isMouseOver(mouseX, mouseY)) {
            ScreenTooltipUtil.drawComponentTooltip(graphics, this.font, List.of(com.jamie.jamiebingo.util.ComponentUtil.literal("Start a game with this card")), mouseX, mouseY, this.width, this.height, 240);
        } else if (clearCardButton != null && clearCardButton.isMouseOver(mouseX, mouseY)) {
            ScreenTooltipUtil.drawComponentTooltip(graphics, this.font, List.of(com.jamie.jamiebingo.util.ComponentUtil.literal("Clear all slots")), mouseX, mouseY, this.width, this.height, 240);
        } else if (importSeedButton != null && importSeedButton.isMouseOver(mouseX, mouseY)) {
            ScreenTooltipUtil.drawComponentTooltip(graphics, this.font, List.of(com.jamie.jamiebingo.util.ComponentUtil.literal("Import seed from box/clipboard")), mouseX, mouseY, this.width, this.height, 240);
        } else if (addMinesButton != null && addMinesButton.isMouseOver(mouseX, mouseY)) {
            ScreenTooltipUtil.drawComponentTooltip(graphics, this.font, List.of(com.jamie.jamiebingo.util.ComponentUtil.literal("Open mine list and tick which mines to add")), mouseX, mouseY, this.width, this.height, 240);
        }
    }

    private void renderMinePicker(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!minePickerOpen) return;
        int panelW = MINE_PANEL_W;
        int panelH = MINE_PANEL_H;
        int panelX = Math.max(8, this.width - panelW - 8);
        int panelY = 16;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xE0101010);
        graphics.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xAAFFFFFF);
        graphics.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xAA000000);
        graphics.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xAAFFFFFF);
        graphics.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xAA000000);
        graphics.drawCenteredString(this.font, "Mines", panelX + panelW / 2, panelY + 8, 0xFFFFFF);

        int rowY = panelY + 24;
        for (int i = 0; i < minePickEntries.size(); i++) {
            MinePickEntry e = minePickEntries.get(i);
            int y = rowY + i * MINE_ROW_HEIGHT;
            int boxX = panelX + 8;
            int boxY = y + 2;
            graphics.fill(boxX, boxY, boxX + MINE_BOX_SIZE, boxY + MINE_BOX_SIZE, 0xFF202020);
            graphics.fill(boxX, boxY, boxX + MINE_BOX_SIZE, boxY + 1, 0xFF909090);
            graphics.fill(boxX, boxY + MINE_BOX_SIZE - 1, boxX + MINE_BOX_SIZE, boxY + MINE_BOX_SIZE, 0xFF000000);
            graphics.fill(boxX, boxY, boxX + 1, boxY + MINE_BOX_SIZE, 0xFF909090);
            graphics.fill(boxX + MINE_BOX_SIZE - 1, boxY, boxX + MINE_BOX_SIZE, boxY + MINE_BOX_SIZE, 0xFF000000);
            if (e.selected) {
                graphics.drawString(this.font, "X", boxX + 2, boxY + 1, 0xFF55FF55);
            }
            graphics.drawString(this.font, e.name, panelX + 24, y + 3, 0xFFFFFFFF);
        }
    }

    private void openMinePicker() {
        ControllerSettingsSnapshot snapshot = ClientCustomCardState.settings;
        if (snapshot == null || !snapshot.minesEnabled()) return;
        minePickerOpen = true;
    }

    private void applySelectedMinesToActiveGrid() {
        ClientCustomCardState.customMineIds.clear();
        ClientCustomCardState.customMineIds.addAll(selectedMineIds());
        minePickerOpen = false;
    }

    private List<String> selectedMineIds() {
        List<String> out = new ArrayList<>();
        for (MinePickEntry e : minePickEntries) {
            if (e.selected && e.id != null && !e.id.isBlank()) {
                out.add(e.id);
            }
        }
        return out;
    }

    private List<Component> buildEntryTooltip(String id) {
        List<Component> tooltip = new ArrayList<>();
        BingoSlot slot = SlotResolver.resolveSlot(id);
        if (slot == null) return tooltip;

        if (id.startsWith("quest.")) {
            QuestDefinition quest = QuestDatabase.getQuestById(id);
            String name = quest != null && quest.name != null ? quest.name : slot.getName();
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(name));
            if (quest != null) {
                if (quest.category != null) {
                    tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Category: " + quest.category));
                }
                if (quest.rarity != null) {
                    tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Rarity: " + quest.rarity));
                }
            }
        } else {
            ItemDefinition def = ItemDatabase.getById(id);
            String name = def != null && def.name() != null ? def.name() : slot.getName();
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(name));
            if (def != null) {
                if (def.category() != null) {
                    tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Category: " + def.category()));
                }
                if (def.rarity() != null) {
                    tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Rarity: " + def.rarity()));
                }
            }
        }
        tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("ID: " + id));
        tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Right click to clear slot"));
        return tooltip;
    }

    private String getHoveredGridId(int mouseX, int mouseY) {
        int left = 20;
        int top = 24;
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int gridHeight = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;

        if (mouseX < left || mouseX > left + gridWidth || mouseY < top || mouseY > top + gridHeight) {
            return null;
        }

        int c = (mouseX - left) / (GRID_SLOT + GRID_GAP);
        int r = (mouseY - top) / (GRID_SLOT + GRID_GAP);
        int idx = r * GRID_SIZE + c;
        List<String> active = poolMode ? poolSlots : cardSlots;
        if (idx < 0 || idx >= active.size()) return null;

        String id = active.get(idx);
        if (id == null || id.isBlank()) return null;
        return id;
    }

    private String getHoveredPaletteId(int mouseX, int mouseY) {
        int left = 20;
        int top = paletteTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int paletteWidth = PALETTE_COLS * GRID_SLOT + (PALETTE_COLS - 1) * GRID_GAP;
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;

        if (mouseX < paletteX || mouseX > paletteX + paletteWidth || mouseY < top || mouseY > top + paletteHeight) {
            return null;
        }

        int col = (mouseX - paletteX) / (GRID_SLOT + GRID_GAP);
        int row = (mouseY - top) / (GRID_SLOT + GRID_GAP);
        int index = paletteScroll * PALETTE_COLS + row * PALETTE_COLS + col;
        if (index < 0 || index >= paletteFiltered.size()) return null;
        return paletteFiltered.get(index).id;
    }

    private boolean isHoveringPoolButton(int mouseX, int mouseY) {
        return poolToggleButton != null && poolToggleButton.isMouseOver(mouseX, mouseY);
    }

    private boolean isHoveringSettingsButton(int mouseX, int mouseY) {
        return settingsButton != null && settingsButton.isMouseOver(mouseX, mouseY);
    }

    private boolean isHoveringCopySeedButton(int mouseX, int mouseY) {
        return copySeedButton != null && copySeedButton.isMouseOver(mouseX, mouseY);
    }

    private boolean isHoveringStartButton(int mouseX, int mouseY) {
        return startGameButton != null && startGameButton.isMouseOver(mouseX, mouseY);
    }

    private boolean isHoveringClearButton(int mouseX, int mouseY) {
        return clearCardButton != null && clearCardButton.isMouseOver(mouseX, mouseY);
    }

    private boolean isHoveringImportButton(int mouseX, int mouseY) {
        return importSeedButton != null && importSeedButton.isMouseOver(mouseX, mouseY);
    }

    private void renderSlotIcon(GuiGraphics graphics, int x, int y, String id) {
        if (id == null || id.isBlank()) return;

        if (id.startsWith("quest.")) {
            BingoSlot slot = SlotResolver.resolveSlot(id);
            if (slot == null) return;
                renderQuestIconSharp(graphics, x, y, QuestIconProvider.iconFor(slot), GRID_SLOT);
        } else {
            Identifier key = com.jamie.jamiebingo.util.IdUtil.id(id);
            Item item = ForgeRegistries.ITEMS.getValue(key);
            if (item == null || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:air")) {
                item = BuiltInRegistries.ITEM.get(key).map(Holder.Reference::value).orElse(null);
            }
            if (item != null && item != com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:air")) {
                renderItemSharp(graphics, new ItemStack(item), x + 1, y + 1);
            }
        }
    }

    private boolean isOverPalette(double mouseX, double mouseY) {
        int left = 20;
        int top = paletteTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int paletteWidth = PALETTE_COLS * GRID_SLOT + (PALETTE_COLS - 1) * GRID_GAP;
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;
        return mouseX >= paletteX && mouseX <= paletteX + paletteWidth
                && mouseY >= top && mouseY <= top + paletteHeight;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isNew) {
        MouseButtonEvent fixedEvent = FixedGuiScaleUtil.virtualEvent(event, this.minecraft);
        if (!minePickerOpen && searchBox != null && searchBox.mouseClicked(fixedEvent, isNew)) {
            setFocused(searchBox);
            if (seedBox != null) seedBox.setFocused(false);
            return true;
        }
        if (!minePickerOpen && seedBox != null && seedBox.mouseClicked(fixedEvent, isNew)) {
            setFocused(seedBox);
            if (searchBox != null) searchBox.setFocused(false);
            return true;
        }

        double mouseX = fixedEvent.x();
        double mouseY = fixedEvent.y();
        int button = fixedEvent.button();

        if (minePickerOpen) {
            if (handleMinePickerClick((int) mouseX, (int) mouseY, button)) {
                return true;
            }
            if (super.mouseClicked(fixedEvent, isNew)) {
                return true;
            }
            int panelW = 300;
            int panelH = 248;
            int panelX = (this.width - panelW) / 2;
            int panelY = (this.height - panelH) / 2;
            return mouseX >= panelX && mouseX <= panelX + panelW && mouseY >= panelY && mouseY <= panelY + panelH;
        }

        int left = 20;
        int top = 24;
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int gridHeight = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;

        if (mouseX >= left && mouseX <= left + gridWidth
                && mouseY >= top && mouseY <= top + gridHeight) {
            int c = (int) ((mouseX - left) / (GRID_SLOT + GRID_GAP));
            int r = (int) ((mouseY - top) / (GRID_SLOT + GRID_GAP));
            int idx = r * GRID_SIZE + c;
            List<String> active = poolMode ? poolSlots : cardSlots;
            if (idx >= 0 && idx < active.size()) {
                if (button == 1) {
                    active.set(idx, "");
                } else if (selected != null) {
                    active.set(idx, selected.id);
                }
            }
            return true;
        }

        int paletteTop = paletteTop();
        int paletteWidth = PALETTE_COLS * GRID_SLOT + (PALETTE_COLS - 1) * GRID_GAP;
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;
        if (mouseX >= paletteX && mouseX <= paletteX + paletteWidth
                && mouseY >= paletteTop && mouseY <= paletteTop + paletteHeight) {
            int col = (int) ((mouseX - paletteX) / (GRID_SLOT + GRID_GAP));
            int row = (int) ((mouseY - paletteTop) / (GRID_SLOT + GRID_GAP));
            int index = paletteScroll * PALETTE_COLS + row * PALETTE_COLS + col;
            if (index >= 0 && index < paletteFiltered.size()) {
                selected = paletteFiltered.get(index);
            }
            return true;
        }

        if (handleScrollbarClick(mouseX, mouseY)) {
            return true;
        }

        return super.mouseClicked(fixedEvent, isNew);
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY) {
        int left = 20;
        int top = paletteTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int paletteWidth = PALETTE_COLS * GRID_SLOT + (PALETTE_COLS - 1) * GRID_GAP;
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;

        int trackX = paletteX + paletteWidth + 6;
        int trackY = top;
        int trackW = 6;
        int trackH = paletteHeight;

        int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
        int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
        if (maxScroll <= 0) return false;

        float visibleRatio = Math.min(1.0f, PALETTE_ROWS / (float) maxRows);
        int handleH = Math.max(10, (int) ((trackH - 2) * visibleRatio));
        int maxHandleY = trackH - 2 - handleH;
        int handleY = trackY + 1 + Math.round(maxHandleY * (paletteScroll / (float) maxScroll));

        if (mouseX >= trackX && mouseX <= trackX + trackW && mouseY >= trackY && mouseY <= trackY + trackH) {
            if (mouseY >= handleY && mouseY <= handleY + handleH) {
                draggingScroll = true;
                scrollDragOffset = (int) mouseY - handleY;
            } else {
                int targetY = (int) mouseY - handleH / 2;
                updateScrollFromHandle(trackY, trackH, handleH, targetY);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double fixedMouseX = FixedGuiScaleUtil.virtualMouseX(mouseX, this.minecraft);
        double fixedMouseY = FixedGuiScaleUtil.virtualMouseY(mouseY, this.minecraft);
        if (isOverPalette(fixedMouseX, fixedMouseY)) {
            int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
            int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
            int dir = deltaY > 0 ? -1 : 1;
            paletteScroll = Math.max(0, Math.min(maxScroll, paletteScroll + dir));
            return true;
        }
        return super.mouseScrolled(fixedMouseX, fixedMouseY, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (minePickerOpen) {
            if (event.key() == 256) {
                minePickerOpen = false;
                return true;
            }
            return super.keyPressed(event);
        }
        if (searchBox != null && searchBox.keyPressed(event)) {
            return true;
        }
        if (seedBox != null && seedBox.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (minePickerOpen) return true;
        if (searchBox != null && searchBox.charTyped(event)) {
            return true;
        }
        if (seedBox != null && seedBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent fixedEvent = FixedGuiScaleUtil.virtualEvent(event, this.minecraft);
        double fixedDragX = FixedGuiScaleUtil.virtualDelta(dragX, this.minecraft);
        double fixedDragY = FixedGuiScaleUtil.virtualDelta(dragY, this.minecraft);
        double mouseX = fixedEvent.x();
        double mouseY = fixedEvent.y();
        int button = fixedEvent.button();
        if (draggingScroll) {
            int left = 20;
            int top = paletteTop();
            int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
            int paletteX = left + gridWidth + 30;
            int paletteWidth = PALETTE_COLS * GRID_SLOT + (PALETTE_COLS - 1) * GRID_GAP;
            int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;

            int trackY = top;
            int trackH = paletteHeight;

            int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
            int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
            if (maxScroll <= 0) return true;

            float visibleRatio = Math.min(1.0f, PALETTE_ROWS / (float) maxRows);
            int handleH = Math.max(10, (int) ((trackH - 2) * visibleRatio));
            int targetY = (int) mouseY - scrollDragOffset;
            updateScrollFromHandle(trackY, trackH, handleH, targetY);
            return true;
        }
        return super.mouseDragged(fixedEvent, fixedDragX, fixedDragY);
    }

    private int paletteTop() {
        return 24 + PALETTE_TOP_OFFSET;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingScroll = false;
        return super.mouseReleased(FixedGuiScaleUtil.virtualEvent(event, this.minecraft));
    }

    private void applyFixedScreenSize() {
        if (this.minecraft == null) return;
        this.width = FixedGuiScaleUtil.virtualWidth(this.minecraft, this.width);
        this.height = FixedGuiScaleUtil.virtualHeight(this.minecraft, this.height);
    }

    private void renderItemSharp(GuiGraphics graphics, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) return;
        graphics.renderItem(stack, x, y);
    }

    private void renderQuestIconSharp(
            GuiGraphics graphics,
            int x,
            int y,
            com.jamie.jamiebingo.quest.icon.QuestIconData icon,
            int boxSize
    ) {
        QuestIconRenderer.renderQuestIcon(graphics, x, y, icon, boxSize);
    }

    private void updateScrollFromHandle(int trackY, int trackH, int handleH, int handleY) {
        int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
        int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
        if (maxScroll <= 0) return;

        int minY = trackY + 1;
        int maxY = trackY + trackH - 1 - handleH;
        int clamped = Math.max(minY, Math.min(maxY, handleY));
        float ratio = (clamped - minY) / (float) Math.max(1, maxY - minY);
        paletteScroll = Math.max(0, Math.min(maxScroll, Math.round(ratio * maxScroll)));
    }

    @Override
    public void tick() {
        if (searchBox != null && !searchBox.getValue().equals(lastSearchValue)) {
            applyFilter();
        }
        super.tick();
    }

    @Override
    public void onClose() {
        saveCurrentSlots();
        List<String> mineIds = selectedMineIds();
        ClientCustomCardState.customMineIds.clear();
        ClientCustomCardState.customMineIds.addAll(mineIds);
        NetworkHandler.sendToServer(
                new PacketSaveCustomCardState(
                        ClientCustomCardState.customCardEnabled,
                        ClientCustomCardState.customPoolEnabled,
                        new ArrayList<>(ClientCustomCardState.customCardSlots),
                        new ArrayList<>(ClientCustomCardState.customPoolIds),
                        mineIds
                )
        );
        super.onClose();
    }

    private void rebuildPoolPagesFromState() {
        poolPages.clear();
        List<String> flat = new ArrayList<>(ClientCustomCardState.customPoolIds);
        int pageSize = GRID_SIZE * GRID_SIZE;
        if (flat.isEmpty()) {
            poolPages.add(new ArrayList<>());
        } else {
            for (int i = 0; i < flat.size(); i += pageSize) {
                List<String> page = new ArrayList<>();
                int end = Math.min(flat.size(), i + pageSize);
                for (int j = i; j < end; j++) {
                    page.add(flat.get(j));
                }
                poolPages.add(page);
            }
        }
        poolPageIndex = Math.max(0, Math.min(poolPageIndex, poolPages.size() - 1));
        ensurePoolPageIndex();
        poolSlots.clear();
        poolSlots.addAll(poolPages.get(poolPageIndex));
        ensureSize(poolSlots, pageSize);
    }

    private void ensurePoolPageIndex() {
        if (poolPages.isEmpty()) {
            poolPages.add(new ArrayList<>());
        }
        poolPageIndex = Math.max(0, Math.min(poolPageIndex, poolPages.size() - 1));
        ensureSize(poolPages.get(poolPageIndex), GRID_SIZE * GRID_SIZE);
    }

    private void savePoolPagesToState() {
        ClientCustomCardState.customPoolIds.clear();
        int pageSize = GRID_SIZE * GRID_SIZE;
        for (List<String> page : poolPages) {
            if (page == null) continue;
            ensureSize(page, pageSize);
            for (String id : page) {
                if (id != null && !id.isBlank()) {
                    ClientCustomCardState.customPoolIds.add(id);
                }
            }
        }
    }

    private void changePoolPage(int delta) {
        if (!poolMode) return;
        saveCurrentSlots();
        int next = poolPageIndex + delta;
        if (next < 0) return;
        if (next >= poolPages.size()) {
            if (!isCurrentPageFull()) return;
            poolPages.add(new ArrayList<>());
            next = poolPages.size() - 1;
        }
        poolPageIndex = next;
        loadCurrentSlots();
    }

    private boolean isCurrentPageFull() {
        for (String id : poolSlots) {
            if (id == null || id.isBlank()) return false;
        }
        return true;
    }

    private void updatePageButtons() {
        if (prevPageButton == null || nextPageButton == null) return;
        boolean show = poolMode;
        prevPageButton.visible = show && poolPages.size() > 1;
        nextPageButton.visible = show && isCurrentPageFull();
        prevPageButton.active = prevPageButton.visible;
        nextPageButton.active = nextPageButton.visible;
        if (addMinesButton != null) {
            addMinesButton.visible = true;
            ControllerSettingsSnapshot snapshot = ClientCustomCardState.settings;
            addMinesButton.active = !minePickerOpen && snapshot != null && snapshot.minesEnabled();
        }
        if (applyMinesSelectionButton != null) {
            int panelW = MINE_PANEL_W;
            int panelH = MINE_PANEL_H;
            int panelX = Math.max(8, this.width - panelW - 8);
            int panelY = 16;
            applyMinesSelectionButton.setPosition(panelX + 8, panelY + panelH + 4);
            applyMinesSelectionButton.visible = minePickerOpen;
            applyMinesSelectionButton.active = minePickerOpen;
        }
        if (closeMinesSelectionButton != null) {
            int panelW = MINE_PANEL_W;
            int panelH = MINE_PANEL_H;
            int panelX = Math.max(8, this.width - panelW - 8);
            int panelY = 16;
            closeMinesSelectionButton.setPosition(panelX + 104, panelY + panelH + 4);
            closeMinesSelectionButton.visible = minePickerOpen;
            closeMinesSelectionButton.active = minePickerOpen;
        }
    }

    private boolean handleMinePickerClick(int mouseX, int mouseY, int button) {
        if (button != 0) return false;
        int panelW = MINE_PANEL_W;
        int panelH = MINE_PANEL_H;
        int panelX = Math.max(8, this.width - panelW - 8);
        int panelY = 16;
        int rowY = panelY + 24;
        for (int i = 0; i < minePickEntries.size(); i++) {
            int y = rowY + i * MINE_ROW_HEIGHT;
            if (mouseX >= panelX + 8 && mouseX <= panelX + panelW - 8 && mouseY >= y && mouseY <= y + MINE_ROW_HEIGHT) {
                MinePickEntry e = minePickEntries.get(i);
                minePickEntries.set(i, new MinePickEntry(e.id, e.name, !e.selected));
                return true;
            }
        }
        return false;
    }

    private Button cycleButton(int x, int y, String text, Runnable action) {
        return com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(text),
                        b -> action.run())
                .pos(x, y)
                .size(18, 16)
                .build();
    }

    private Button labelButton(int x, int y, int width, String text) {
        Button button = com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(text),
                        b -> {})
                .pos(x, y)
                .size(width, 16)
                .build();
        button.active = false;
        return button;
    }

    private void cycleFocus(int delta) {
        focusMode = Math.floorMod(focusMode + delta, 3);
        applyFilter();
    }

    private void cycleRarityFilter(int delta) {
        List<String> options = rarityOptions();
        rarityFilterIndex = Math.floorMod(rarityFilterIndex + delta, options.size());
        applyFilter();
    }

    private void cycleCategoryFilter(int delta) {
        List<String> options = categoryOptions();
        categoryFilterIndex = Math.floorMod(categoryFilterIndex + delta, options.size());
        applyFilter();
    }

    private List<String> rarityOptions() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        out.add("off");
        out.add("common");
        out.add("uncommon");
        out.add("rare");
        out.add("epic");
        out.add("legendary");
        out.add("mythic");
        out.add("impossible");
        for (PaletteEntry entry : paletteAll) {
            if (entry.rarity != null && !entry.rarity.isBlank()) out.add(entry.rarity);
        }
        return new ArrayList<>(out);
    }

    private List<String> categoryOptions() {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        out.add("off");
        for (PaletteEntry entry : paletteAll) {
            if (entry.category != null && !entry.category.isBlank()) {
                out.add(entry.category.toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(out);
    }

    private String currentRarityFilter() {
        List<String> options = rarityOptions();
        rarityFilterIndex = Math.max(0, Math.min(rarityFilterIndex, options.size() - 1));
        return options.get(rarityFilterIndex);
    }

    private String currentCategoryFilter() {
        List<String> options = categoryOptions();
        categoryFilterIndex = Math.max(0, Math.min(categoryFilterIndex, options.size() - 1));
        return options.get(categoryFilterIndex);
    }

    private String focusLabel() {
        return switch (focusMode) {
            case 1 -> "Focus: Quests";
            case 2 -> "Focus: Items";
            default -> "Focus: Off";
        };
    }

    private String rarityFilterLabel() {
        String value = currentRarityFilter();
        return "Filter By Rarity: " + ("off".equals(value) ? "Off" : value);
    }

    private String categoryFilterLabel() {
        String value = currentCategoryFilter();
        return "Filter By Category: " + ("off".equals(value) ? "Off" : value);
    }

    private void updateFilterButtons() {
        if (focusButton != null) {
            focusButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(focusLabel()));
        }
        if (filterRarityButton != null) {
            filterRarityButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(rarityFilterLabel()));
        }
        if (filterCategoryButton != null) {
            filterCategoryButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(categoryFilterLabel()));
        }
    }

    private String effectivePaletteRarity(String id, String fallback) {
        String normalizedFallback = fallback == null || fallback.isBlank() ? "impossible" : fallback.toLowerCase(Locale.ROOT);
        return ClientRarityOverrideSettings.getActiveOverrides().getOrDefault(id, normalizedFallback);
    }

    private static final class PaletteEntry {
        final String id;
        final String name;
        final boolean quest;
        final String category;
        final String rarity;

        private PaletteEntry(String id, String name, boolean quest, String category, String rarity) {
            this.id = id;
            this.name = name == null ? id : name;
            this.quest = quest;
            this.category = category == null || category.isBlank() ? "unknown" : category;
            this.rarity = rarity == null || rarity.isBlank() ? "impossible" : rarity.toLowerCase(Locale.ROOT);
        }
    }

    private static final class MinePickEntry {
        final String id;
        final String name;
        final boolean selected;

        private MinePickEntry(String id, String name, boolean selected) {
            this.id = id;
            this.name = name;
            this.selected = selected;
        }
    }
}


