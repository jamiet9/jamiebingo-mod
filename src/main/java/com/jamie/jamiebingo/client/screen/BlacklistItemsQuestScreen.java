package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.ItemDefinition;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.SlotResolver;
import com.jamie.jamiebingo.client.ClientBlacklistSettings;
import com.jamie.jamiebingo.client.ClientRarityOverrideSettings;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.data.ItemDatabase;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketSetBlacklistIds;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BlacklistItemsQuestScreen extends Screen {
    private static final int TOP = 48;
    private static final int LIST_TOP = TOP;
    private static final int PALETTE_TOP_OFFSET = 96;
    private static final long PRESET_CLICK_DELAY_MS = 350L;

    private static final int GRID_SIZE = 10;
    private static final int GRID_SLOT = 18;
    private static final int GRID_GAP = 2;
    private static final int PALETTE_COLS = 7;
    private static final int PALETTE_ROWS = 6;

    private final List<PaletteEntry> paletteAll = new ArrayList<>();
    private final List<PaletteEntry> paletteFiltered = new ArrayList<>();
    private final List<String> blacklistGrid = new ArrayList<>();
    private final List<PresetState> presets = new ArrayList<>();
    private final Set<String> blacklistedIds = new LinkedHashSet<>();

    private int paletteScroll = 0;
    private int blacklistPage = 0;
    private int presetIndex = 0;
    private String lastSearchValue = "";
    private boolean draggingScroll = false;
    private int scrollDragOffset = 0;
    private long lastPresetActionAt = 0L;
    private int focusMode = 0;
    private int rarityFilterIndex = 0;
    private int categoryFilterIndex = 0;

    private EditBox searchBox;
    private EditBox presetNameBox;
    private Button clearAllButton;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button prevPresetButton;
    private Button nextPresetButton;
    private Button newPresetButton;
    private Button deletePresetButton;
    private Button savePresetButton;
    private Button copySeedButton;
    private Button importSeedButton;
    private Button togglePresetButton;
    private Button presetModeButton;
    private Button focusButton;
    private Button filterRarityButton;
    private Button filterCategoryButton;
    private Button focusLeftButton;
    private Button focusRightButton;
    private Button rarityLeftButton;
    private Button rarityRightButton;
    private Button categoryLeftButton;
    private Button categoryRightButton;

    public BlacklistItemsQuestScreen() {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Black/White List"));
    }

    private void loadPresets() {
        presets.clear();
        for (ClientBlacklistSettings.Preset preset : ClientBlacklistSettings.getPresets()) {
            if (preset == null) continue;
            presets.add(new PresetState(preset.name, preset.enabled, preset.mode, preset.ids));
        }
        if (presets.isEmpty()) {
            presets.add(new PresetState("Default", true, ClientBlacklistSettings.Preset.MODE_BLACKLIST, Set.of()));
        }
        presetIndex = Math.max(0, Math.min(presetIndex, presets.size() - 1));
        loadCurrentPreset();
    }

    private void commitCurrentPreset() {
        if (presets.isEmpty()) return;
        PresetState current = presets.get(presetIndex);
        current.name = presetNameBox == null ? current.name : presetNameBox.getValue();
        current.ids.clear();
        current.ids.addAll(blacklistedIds);
    }

    private void loadCurrentPreset() {
        blacklistedIds.clear();
        if (!presets.isEmpty()) {
            PresetState preset = presets.get(presetIndex);
            blacklistedIds.addAll(preset.ids);
            if (presetNameBox != null) {
                presetNameBox.setValue(preset.name);
            }
        } else if (presetNameBox != null) {
            presetNameBox.setValue("");
        }
        rebuildGridFromSet();
    }

    private void switchPreset(int delta) {
        if (!allowPresetAction()) return;
        if (presets.isEmpty()) return;
        commitCurrentPreset();
        presetIndex = (presetIndex + delta + presets.size()) % presets.size();
        loadCurrentPreset();
        updatePresetButtons();
    }

    private void createPreset() {
        if (!allowPresetAction()) return;
        commitCurrentPreset();
        int n = presets.size() + 1;
        String mode = presets.isEmpty() ? ClientBlacklistSettings.Preset.MODE_BLACKLIST : presets.get(presetIndex).mode;
        presets.add(new PresetState("Preset " + n, true, mode, Set.of()));
        presetIndex = presets.size() - 1;
        loadCurrentPreset();
        updatePresetButtons();
    }

    private void deletePreset() {
        if (!allowPresetAction()) return;
        if (presets.isEmpty()) return;
        if (presets.size() == 1) {
            presets.get(0).ids.clear();
            presets.get(0).enabled = true;
            loadCurrentPreset();
            updatePresetButtons();
            return;
        }
        presets.remove(presetIndex);
        presetIndex = Math.max(0, Math.min(presetIndex, presets.size() - 1));
        loadCurrentPreset();
        updatePresetButtons();
    }

    private void toggleCurrentPresetEnabled() {
        if (presets.isEmpty()) return;
        PresetState current = presets.get(presetIndex);
        current.enabled = !current.enabled;
        updatePresetButtons();
    }

    private void cycleCurrentPresetMode() {
        if (presets.isEmpty()) return;
        PresetState current = presets.get(presetIndex);
        current.mode = ClientBlacklistSettings.Preset.MODE_BLACKLIST.equals(current.mode)
                ? ClientBlacklistSettings.Preset.MODE_WHITELIST
                : ClientBlacklistSettings.Preset.MODE_BLACKLIST;
        updatePresetButtons();
    }

    private boolean allowPresetAction() {
        long now = System.currentTimeMillis();
        if (now - lastPresetActionAt < PRESET_CLICK_DELAY_MS) {
            return false;
        }
        lastPresetActionAt = now;
        return true;
    }

    @Override
    protected void init() {
        applyFixedScreenSize();
        loadPresets();

        int left = 20;
        int top = TOP;
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int presetY = top - 18;

        prevPresetButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("<"),
                                b -> switchPreset(-1))
                        .pos(left - 6, presetY)
                        .size(18, 16)
                        .build()
        );
        presetNameBox = new EditBox(
                font,
                left + 14,
                presetY,
                166,
                16,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Preset Name")
        );
        presetNameBox.setCanLoseFocus(true);
        presetNameBox.setResponder(s -> {
            if (!presets.isEmpty()) {
                presets.get(presetIndex).name = s == null ? "" : s;
                updatePresetButtons();
            }
        });
        addRenderableWidget(presetNameBox);
        nextPresetButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal(">"),
                                b -> switchPreset(1))
                        .pos(left + 182, presetY)
                        .size(18, 16)
                        .build()
        );
        togglePresetButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("Preset: Enabled"),
                                b -> toggleCurrentPresetEnabled())
                        .pos(left + 204, presetY)
                        .size(92, 16)
                        .build()
        );
        presetModeButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("Blacklist/Whitelist: Blacklist"),
                                b -> cycleCurrentPresetMode())
                        .pos(left + 300, presetY)
                        .size(156, 16)
                        .build()
        );
        newPresetButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("Add New Preset"),
                                b -> createPreset())
                        .pos(left + 460, presetY)
                        .size(110, 16)
                        .build()
        );
        deletePresetButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("Remove Current Preset"),
                                b -> deletePreset())
                        .pos(left + 460, presetY + 18)
                        .size(132, 16)
                        .build()
        );
        savePresetButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("Save Preset"),
                                b -> saveCurrentPreset())
                        .pos(left + 460, presetY + 36)
                        .size(132, 16)
                        .build()
        );
        copySeedButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("Copy Preset Seed"),
                                b -> copyPresetSeed())
                        .pos(left + 460, presetY + 54)
                        .size(132, 16)
                        .build()
        );
        importSeedButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("Import Preset Seed"),
                                b -> importPresetSeedFromClipboard())
                        .pos(left + 460, presetY + 72)
                        .size(132, 16)
                        .build()
        );

        searchBox = new EditBox(
                font,
                paletteX,
                top + 2,
                156,
                16,
                com.jamie.jamiebingo.util.ComponentUtil.literal("Search")
        );
        searchBox.setValue("");
        searchBox.setResponder(s -> applyFilter());
        searchBox.setCanLoseFocus(true);
        addRenderableWidget(searchBox);

        int filterY = top + 22;
        focusLeftButton = addRenderableWidget(cycleButton(paletteX, filterY, "<", () -> cycleFocus(-1)));
        focusButton = addRenderableWidget(labelButton(paletteX + 20, filterY, 128, focusLabel()));
        focusRightButton = addRenderableWidget(cycleButton(paletteX + 150, filterY, ">", () -> cycleFocus(1)));
        rarityLeftButton = addRenderableWidget(cycleButton(paletteX, filterY + 18, "<", () -> cycleRarityFilter(-1)));
        filterRarityButton = addRenderableWidget(labelButton(paletteX + 20, filterY + 18, 128, rarityFilterLabel()));
        rarityRightButton = addRenderableWidget(cycleButton(paletteX + 150, filterY + 18, ">", () -> cycleRarityFilter(1)));
        categoryLeftButton = addRenderableWidget(cycleButton(paletteX, filterY + 36, "<", () -> cycleCategoryFilter(-1)));
        filterCategoryButton = addRenderableWidget(labelButton(paletteX + 20, filterY + 36, 128, categoryFilterLabel()));
        categoryRightButton = addRenderableWidget(cycleButton(paletteX + 150, filterY + 36, ">", () -> cycleCategoryFilter(1)));

        clearAllButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("Clear Current List"),
                                b -> {
                                    blacklistedIds.clear();
                                    blacklistPage = 0;
                                    rebuildGridFromSet();
                                    updateBlacklistPageButtons();
                                })
                        .pos(left - 6, height - 26)
                        .size(120, 18)
                        .build()
        );

        prevPageButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("< Prev"),
                                b -> {
                                    blacklistPage = Math.max(0, blacklistPage - 1);
                                    rebuildGridFromSet();
                                    updateBlacklistPageButtons();
                                })
                        .pos(left + 126, height - 26)
                        .size(64, 18)
                        .build()
        );

        nextPageButton = addRenderableWidget(
                com.jamie.jamiebingo.util.ButtonUtil.builder(
                                com.jamie.jamiebingo.util.ComponentUtil.literal("Next >"),
                                b -> {
                                    blacklistPage = Math.min(maxBlacklistPage(), blacklistPage + 1);
                                    rebuildGridFromSet();
                                    updateBlacklistPageButtons();
                                })
                        .pos(left + 194, height - 26)
                        .size(64, 18)
                        .build()
        );

        rebuildPalette();
        updateBlacklistPageButtons();
        updatePresetButtons();
        super.init();
    }

    private void rebuildGridFromSet() {
        blacklistGrid.clear();
        List<String> all = new ArrayList<>(blacklistedIds);
        int pageSize = GRID_SIZE * GRID_SIZE;
        int maxPage = Math.max(0, (all.size() - 1) / pageSize);
        blacklistPage = Math.max(0, Math.min(maxPage, blacklistPage));
        int from = blacklistPage * pageSize;
        int to = Math.min(all.size(), from + pageSize);
        for (int i = from; i < to; i++) {
            blacklistGrid.add(all.get(i));
        }
        while (blacklistGrid.size() < pageSize) {
            blacklistGrid.add("");
        }
    }

    private void rebuildPalette() {
        paletteAll.clear();
        ItemDatabase.load();

        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null) continue;
            if (item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:air")) continue;
            String name = new ItemStack(item).getHoverName().getString();
            ItemDefinition def = ItemDatabase.getRawById(id.toString());
            paletteAll.add(new PaletteEntry(
                    id.toString(),
                    name,
                    false,
                    def == null ? "unknown" : def.category(),
                    effectivePaletteRarity(id.toString(), def == null ? "unknown" : def.rarity())
            ));
        }

        if (paletteAll.isEmpty()) {
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

        QuestDatabase.load();
        for (QuestDefinition q : QuestDatabase.getQuests()) {
            if (q == null || q.id == null || q.id.isBlank()) continue;
            paletteAll.add(new PaletteEntry(q.id, q.name, true, q.category, effectivePaletteRarity(q.id, q.rarity)));
        }

        applyFilter();
    }

    private void applyFilter() {
        paletteFiltered.clear();
        String queryRaw = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT);
        String query = normalizeSearch(queryRaw);
        for (PaletteEntry entry : paletteAll) {
            String name = normalizeSearch(entry.name);
            String id = normalizeSearch(entry.id.replace('_', ' '));
            if (!query.isBlank() && !name.contains(query) && !id.contains(query)) {
                continue;
            }
            if (focusMode == 1 && !entry.quest) continue;
            if (focusMode == 2 && entry.quest) continue;
            String rarity = currentRarityFilter();
            if (!"off".equals(rarity) && !rarity.equalsIgnoreCase(entry.rarity)) continue;
            String category = currentCategoryFilter();
            if (!"off".equals(category) && !category.equalsIgnoreCase(entry.category)) continue;
            paletteFiltered.add(entry);
        }
        paletteScroll = 0;
        lastSearchValue = searchBox == null ? "" : searchBox.getValue();
        updateFilterButtons();
    }

    private String normalizeSearch(String value) {
        if (value == null) return "";
        String normalized = value.toLowerCase(Locale.ROOT).replace('_', ' ');
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private void addToBlacklist(String id) {
        if (id == null || id.isBlank()) return;
        if (blacklistedIds.add(id)) {
            blacklistPage = maxBlacklistPage();
            rebuildGridFromSet();
            updateBlacklistPageButtons();
        }
    }

    private void removeFromBlacklist(String id) {
        if (id == null || id.isBlank()) return;
        if (blacklistedIds.remove(id)) {
            rebuildGridFromSet();
            updateBlacklistPageButtons();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        applyFixedScreenSize();
        MenuTextureQualityUtil.ensureNearestFiltering();
        float appliedScale = FixedGuiScaleUtil.beginScaledRender(graphics, this.minecraft);
        int fixedMouseX = FixedGuiScaleUtil.virtualMouseX(mouseX, this.minecraft);
        int fixedMouseY = FixedGuiScaleUtil.virtualMouseY(mouseY, this.minecraft);
        try {
        renderBlurredBackground(graphics);
        int left = 20;
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int titleCenter = left + gridWidth / 2;
        PresetState currentPreset = presets.isEmpty() ? null : presets.get(Math.max(0, Math.min(presetIndex, presets.size() - 1)));
        graphics.drawCenteredString(
                font,
                "Black/White List (Page " + (blacklistPage + 1) + "/" + (maxBlacklistPage() + 1) + ")",
                titleCenter,
                8,
                0xFFFFFF
        );
        if (currentPreset != null) {
            // Label is rendered by presetLabelButton for consistent visibility.
        }

        renderBlacklistGrid(graphics, fixedMouseX, fixedMouseY);
        renderPalette(graphics);
        renderScrollBar(graphics);
        renderTooltips(graphics, fixedMouseX, fixedMouseY);

        super.render(graphics, fixedMouseX, fixedMouseY, partialTick);
        } finally {
            FixedGuiScaleUtil.endScaledRender(graphics, appliedScale);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void renderBlacklistGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int left = 20;
        int top = listTop();

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                int idx = r * GRID_SIZE + c;
                int x = left + c * (GRID_SLOT + GRID_GAP);
                int y = top + r * (GRID_SLOT + GRID_GAP);
                graphics.fill(x, y, x + GRID_SLOT, y + GRID_SLOT, 0xFF202020);
                String id = idx < blacklistGrid.size() ? blacklistGrid.get(idx) : "";
                if (id != null && !id.isBlank()) {
                    renderSlotIcon(graphics, x, y, id);
                }
            }
        }
    }

    private void renderPalette(GuiGraphics graphics) {
        int left = 20;
        int top = paletteTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int startIndex = paletteScroll * PALETTE_COLS;
        int perPage = PALETTE_COLS * PALETTE_ROWS;

        for (int i = 0; i < perPage; i++) {
            int idx = startIndex + i;
            if (idx >= paletteFiltered.size()) break;
            int col = i % PALETTE_COLS;
            int row = i / PALETTE_COLS;
            int x = paletteX + col * (GRID_SLOT + GRID_GAP);
            int y = top + row * (GRID_SLOT + GRID_GAP);

            PaletteEntry entry = paletteFiltered.get(idx);
            graphics.fill(x, y, x + GRID_SLOT, y + GRID_SLOT, 0xFF202020);
            renderSlotIcon(graphics, x, y, entry.id);

            if (blacklistedIds.contains(entry.id)) {
                int border = currentPresetBorderColor();
                graphics.fill(x - 1, y - 1, x + GRID_SLOT + 1, y, border);
                graphics.fill(x - 1, y + GRID_SLOT, x + GRID_SLOT + 1, y + GRID_SLOT + 1, border);
                graphics.fill(x - 1, y, x, y + GRID_SLOT, border);
                graphics.fill(x + GRID_SLOT, y, x + GRID_SLOT + 1, y + GRID_SLOT, border);
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
        graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF1A1A1A);

        int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
        int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
        if (maxScroll <= 0) {
            graphics.fill(trackX + 1, trackY + 1, trackX + trackW - 1, trackY + trackH - 1, 0xFF3A3A3A);
            return;
        }

        float visibleRatio = Math.min(1.0f, PALETTE_ROWS / (float) maxRows);
        int handleH = Math.max(10, (int) ((trackH - 2) * visibleRatio));
        int maxHandleY = trackH - 2 - handleH;
        int handleY = trackY + 1 + Math.round(maxHandleY * (paletteScroll / (float) maxScroll));
        graphics.fill(trackX + 1, handleY, trackX + trackW - 1, handleY + handleH, 0xFF6A6A6A);
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        String hoveredId = getHoveredGridId(mouseX, mouseY);
        if (hoveredId == null) {
            hoveredId = getHoveredPaletteId(mouseX, mouseY);
        }
        if (hoveredId == null || hoveredId.isBlank()) return;

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
        }
    }

    private List<Component> buildEntryTooltip(String id) {
        List<Component> tooltip = new ArrayList<>();
        BingoSlot slot = SlotResolver.resolveSlot(id);
        if (slot == null) return tooltip;

        tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(slot.getName()));
        tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("ID: " + id));
        PresetState current = presets.isEmpty() ? null : presets.get(presetIndex);
        boolean whitelistMode = current != null && ClientBlacklistSettings.Preset.MODE_WHITELIST.equals(current.mode);
        if (blacklistedIds.contains(id)) {
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Click to remove from current " + (whitelistMode ? "whitelist" : "blacklist")));
        } else {
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Click to add to current " + (whitelistMode ? "whitelist" : "blacklist")));
        }
        return tooltip;
    }

    private String getHoveredGridId(int mouseX, int mouseY) {
        int left = 20;
        int top = listTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int gridHeight = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        if (mouseX < left || mouseX > left + gridWidth || mouseY < top || mouseY > top + gridHeight) {
            return null;
        }

        int c = (mouseX - left) / (GRID_SLOT + GRID_GAP);
        int r = (mouseY - top) / (GRID_SLOT + GRID_GAP);
        int idx = r * GRID_SIZE + c;
        if (idx < 0 || idx >= blacklistGrid.size()) return null;
        String id = blacklistGrid.get(idx);
        return (id == null || id.isBlank()) ? null : id;
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

    private void renderSlotIcon(GuiGraphics graphics, int x, int y, String id) {
        if (id == null || id.isBlank()) return;
        if (id.startsWith("quest.")) {
            BingoSlot slot = SlotResolver.resolveSlot(id);
            if (slot == null) return;
            renderQuestIconSharp(graphics, x, y, QuestIconProvider.iconFor(slot), GRID_SLOT);
            return;
        }

        Identifier key = com.jamie.jamiebingo.util.IdUtil.id(id);
        Item item = ForgeRegistries.ITEMS.getValue(key);
        if (item == null || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:air")) {
            item = BuiltInRegistries.ITEM.get(key).map(Holder.Reference::value).orElse(null);
        }
        if (item != null && item != com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:air")) {
            renderItemSharp(graphics, new ItemStack(item), x + 1, y + 1);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isNew) {
        MouseButtonEvent fixedEvent = FixedGuiScaleUtil.virtualEvent(event, this.minecraft);
        if (presetNameBox != null && presetNameBox.mouseClicked(fixedEvent, isNew)) {
            setFocused(presetNameBox);
            return true;
        }
        if (searchBox != null && searchBox.mouseClicked(fixedEvent, isNew)) {
            setFocused(searchBox);
            return true;
        }

        double mouseX = fixedEvent.x();
        double mouseY = fixedEvent.y();

        String gridId = getHoveredGridId((int) mouseX, (int) mouseY);
        if (gridId != null) {
            removeFromBlacklist(gridId);
            return true;
        }

        String paletteId = getHoveredPaletteId((int) mouseX, (int) mouseY);
        if (paletteId != null) {
            if (blacklistedIds.contains(paletteId)) {
                removeFromBlacklist(paletteId);
            } else {
                addToBlacklist(paletteId);
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
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent fixedEvent = FixedGuiScaleUtil.virtualEvent(event, this.minecraft);
        double fixedDragX = FixedGuiScaleUtil.virtualDelta(dragX, this.minecraft);
        double fixedDragY = FixedGuiScaleUtil.virtualDelta(dragY, this.minecraft);
        if (!draggingScroll) {
            return super.mouseDragged(fixedEvent, fixedDragX, fixedDragY);
        }

        int top = paletteTop();
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;
        int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
        int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
        if (maxScroll <= 0) return true;

        float visibleRatio = Math.min(1.0f, PALETTE_ROWS / (float) maxRows);
        int handleH = Math.max(10, (int) ((paletteHeight - 2) * visibleRatio));
        int targetY = (int) fixedEvent.y() - scrollDragOffset;
        updateScrollFromHandle(top, paletteHeight, handleH, targetY);
        return true;
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

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (presetNameBox != null && presetNameBox.keyPressed(event)) {
            return true;
        }
        if (searchBox != null && searchBox.keyPressed(event)) {
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (presetNameBox != null && presetNameBox.charTyped(event)) {
            return true;
        }
        if (searchBox != null && searchBox.charTyped(event)) {
            return true;
        }
        return super.charTyped(event);
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
        persistPresetsAndSync();
        super.onClose();
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

    private int paletteTop() {
        return TOP + PALETTE_TOP_OFFSET;
    }

    private int listTop() {
        return LIST_TOP;
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

    private int maxBlacklistPage() {
        int total = blacklistedIds.size();
        int pageSize = GRID_SIZE * GRID_SIZE;
        return Math.max(0, (total - 1) / pageSize);
    }

    private void updateBlacklistPageButtons() {
        if (prevPageButton == null || nextPageButton == null) return;
        int max = maxBlacklistPage();
        prevPageButton.active = blacklistPage > 0;
        nextPageButton.active = blacklistPage < max;
    }

    private void updatePresetButtons() {
        if (prevPresetButton == null || nextPresetButton == null || newPresetButton == null
                || deletePresetButton == null || savePresetButton == null || togglePresetButton == null || presetModeButton == null) {
            return;
        }
        boolean many = presets.size() > 1;
        prevPresetButton.active = many;
        nextPresetButton.active = many;
        deletePresetButton.active = !presets.isEmpty();
        savePresetButton.active = !presets.isEmpty();
        PresetState current = presets.isEmpty() ? null : presets.get(presetIndex);
        if (presetNameBox != null) {
            presetNameBox.setEditable(current != null);
            if (current != null && !presetNameBox.getValue().equals(current.name)) {
                presetNameBox.setValue(current.name);
            }
        }
        String toggleLabel = current == null
                ? "Preset: Disabled"
                : ("Preset: " + (current.enabled ? "Enabled" : "Disabled"));
        togglePresetButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(toggleLabel));
        String modeLabel = current == null
                ? "Blacklist/Whitelist: Blacklist"
                : "Blacklist/Whitelist: " + ("whitelist".equals(current.mode) ? "Whitelist" : "Blacklist");
        presetModeButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(modeLabel));
    }

    private void saveCurrentPreset() {
        persistPresetsAndSync();
    }

    private void copyPresetSeed() {
        commitCurrentPreset();
        List<ClientBlacklistSettings.Preset> out = new ArrayList<>();
        for (PresetState preset : presets) {
            out.add(new ClientBlacklistSettings.Preset(preset.name, preset.enabled, preset.mode, preset.ids));
        }
        ClientBlacklistSettings.setPresets(out);
        ClientBlacklistSettings.save();
        setClipboard(ClientBlacklistSettings.exportSeed());
    }

    private void importPresetSeedFromClipboard() {
        if (!allowPresetAction()) return;
        String seed = getClipboard();
        if (!ClientBlacklistSettings.importSeed(seed)) return;
        presetIndex = Integer.MAX_VALUE;
        loadPresets();
        updatePresetButtons();
        updateBlacklistPageButtons();
    }

    private String getClipboard() {
        net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
        if (mc == null || mc.keyboardHandler == null) return "";
        String raw = mc.keyboardHandler.getClipboard();
        return raw == null ? "" : raw.trim();
    }

    private void setClipboard(String value) {
        net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
        if (mc == null || mc.keyboardHandler == null) return;
        mc.keyboardHandler.setClipboard(value == null ? "" : value);
    }

    private void persistPresetsAndSync() {
        commitCurrentPreset();
        List<ClientBlacklistSettings.Preset> out = new ArrayList<>();
        for (PresetState preset : presets) {
            out.add(new ClientBlacklistSettings.Preset(preset.name, preset.enabled, preset.mode, preset.ids));
        }
        ClientBlacklistSettings.setPresets(out);
        ClientBlacklistSettings.save();
        Set<String> activeBlacklist = ClientBlacklistSettings.getActiveBlacklistUnion();
        Set<String> activeWhitelist = ClientBlacklistSettings.getActiveWhitelistUnion();
        if (activeBlacklist.size() + activeWhitelist.size() <= 1024) {
            NetworkHandler.sendToServer(new PacketSetBlacklistIds(new ArrayList<>(activeBlacklist), new ArrayList<>(activeWhitelist)));
        }
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
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("off");
        out.add("common");
        out.add("uncommon");
        out.add("rare");
        out.add("epic");
        out.add("legendary");
        out.add("mythic");
        out.add("impossible");
        for (PaletteEntry entry : paletteAll) {
            if (entry.rarity != null && !entry.rarity.isBlank()) {
                out.add(entry.rarity.toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(out);
    }

    private List<String> categoryOptions() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
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

    private int currentPresetBorderColor() {
        PresetState current = presets.isEmpty() ? null : presets.get(presetIndex);
        return current != null && ClientBlacklistSettings.Preset.MODE_WHITELIST.equals(current.mode)
                ? 0xAA44FF66
                : 0xAAFF4444;
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

    private static final class PresetState {
        String name;
        boolean enabled;
        String mode;
        final Set<String> ids = new LinkedHashSet<>();

        private PresetState(String name, boolean enabled, String mode, Set<String> ids) {
            this.name = (name == null || name.isBlank()) ? "Preset" : name;
            this.enabled = enabled;
            this.mode = ClientBlacklistSettings.Preset.MODE_WHITELIST.equalsIgnoreCase(mode)
                    ? ClientBlacklistSettings.Preset.MODE_WHITELIST
                    : ClientBlacklistSettings.Preset.MODE_BLACKLIST;
            if (ids != null) {
                for (String id : ids) {
                    if (id != null && !id.isBlank()) this.ids.add(id);
                }
            }
        }
    }
}
