package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.ItemDefinition;
import com.jamie.jamiebingo.bingo.BingoRarityUtil;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.SlotResolver;
import com.jamie.jamiebingo.client.ClientBlacklistSettings;
import com.jamie.jamiebingo.client.ClientRarityOverrideSettings;
import com.jamie.jamiebingo.client.render.QuestIconRenderer;
import com.jamie.jamiebingo.data.ItemDatabase;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketSetRarityOverridesChunk;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RarityChangerScreen extends Screen {
    private static final int TOP = 48;
    private static final int PALETTE_TOP_OFFSET = 96;
    private static final int LIST_TOP = TOP;
    private static final int GRID_SIZE = 10;
    private static final int GRID_SLOT = 18;
    private static final int GRID_GAP = 2;
    private static final int PALETTE_COLS = 7;
    private static final int PALETTE_ROWS = 6;
    private static final long PRESET_CLICK_DELAY_MS = 350L;
    private static final int MAX_OVERRIDES_PER_PACKET = 180;

    private final List<PaletteEntry> paletteAll = new ArrayList<>();
    private final List<PaletteEntry> paletteFiltered = new ArrayList<>();
    private final List<String> overrideGrid = new ArrayList<>();
    private final List<PresetState> presets = new ArrayList<>();
    private final Map<String, String> overrides = new LinkedHashMap<>();

    private int paletteScroll = 0;
    private int overridePage = 0;
    private int presetIndex = 0;
    private int focusMode = 0;
    private int rarityFilterIndex = 0;
    private int categoryFilterIndex = 0;
    private long lastPresetActionAt = 0L;
    private String selectedId = "";
    private String lastSearchValue = "";
    private boolean draggingPaletteScroll = false;
    private int paletteScrollDragOffset = 0;
    private boolean listedItemsOnly = false;

    private EditBox searchBox;
    private EditBox presetNameBox;
    private Button prevPageButton;
    private Button nextPageButton;
    private Button prevPresetButton;
    private Button nextPresetButton;
    private Button newPresetButton;
    private Button deletePresetButton;
    private Button importAdventurePresetButton;
    private Button copySeedButton;
    private Button importSeedButton;
    private Button saveOverridesButton;
    private Button togglePresetButton;
    private Button focusButton;
    private Button filterRarityButton;
    private Button filterCategoryButton;
    private Button listedItemsButton;
    private Button selectedRarityButton;
    private Button resetOverrideButton;

    public RarityChangerScreen() {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Rarity Changer"));
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
        int presetActionWidth = 132;
        int presetActionX = Math.min(width - presetActionWidth - 20, paletteX + 214);

        prevPresetButton = addRenderableWidget(cycleButton(left - 6, presetY, "<", () -> switchPreset(-1)));
        presetNameBox = new EditBox(font, left + 14, presetY, 166, 16, com.jamie.jamiebingo.util.ComponentUtil.literal("Preset Name"));
        presetNameBox.setCanLoseFocus(true);
        presetNameBox.setResponder(s -> {
            if (!presets.isEmpty()) {
                presets.get(presetIndex).name = s == null ? "" : s;
                updatePresetButtons();
            }
        });
        addRenderableWidget(presetNameBox);
        nextPresetButton = addRenderableWidget(cycleButton(left + 182, presetY, ">", () -> switchPreset(1)));
        togglePresetButton = addRenderableWidget(button(left + 204, presetY, 112, "Preset: Enabled", this::toggleCurrentPresetEnabled));
        newPresetButton = addRenderableWidget(button(presetActionX, presetY, presetActionWidth, "Add New Preset", this::createPreset));
        deletePresetButton = addRenderableWidget(button(presetActionX, presetY + 18, presetActionWidth, "Remove Current Preset", this::deletePreset));
        copySeedButton = addRenderableWidget(button(presetActionX, presetY + 36, presetActionWidth, "Copy Preset Seed", this::copyPresetSeed));
        importSeedButton = addRenderableWidget(button(presetActionX, presetY + 54, presetActionWidth, "Import Preset Seed", this::importPresetSeedFromClipboard));
        importAdventurePresetButton = addRenderableWidget(button(presetActionX, presetY + 72, presetActionWidth, "Import Adventure Preset", this::importAdventurePreset));
        listedItemsButton = addRenderableWidget(button(presetActionX, presetY + 90, presetActionWidth, listedItemsLabel(), this::toggleListedItemsOnly));
        saveOverridesButton = addRenderableWidget(button(presetActionX, presetY + 108, presetActionWidth, "Save And Close", this::saveAndClose));

        searchBox = new EditBox(font, paletteX, top + 2, 156, 16, com.jamie.jamiebingo.util.ComponentUtil.literal("Search"));
        searchBox.setResponder(s -> applyFilter());
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

        prevPageButton = addRenderableWidget(button(left, height - 26, 64, "< Prev", () -> {
            overridePage = Math.max(0, overridePage - 1);
            rebuildGrid();
            updatePageButtons();
        }));
        nextPageButton = addRenderableWidget(button(left + 68, height - 26, 64, "Next >", () -> {
            overridePage = Math.min(maxOverridePage(), overridePage + 1);
            rebuildGrid();
            updatePageButtons();
        }));
        selectedRarityButton = addRenderableWidget(button(left + 150, height - 26, 170, "Selected Rarity", () -> cycleSelectedRarity(1)));
        resetOverrideButton = addRenderableWidget(button(left + 326, height - 26, 170, "Reset Rarity To Default", this::resetSelectedOverride));

        rebuildPalette();
        updatePresetButtons();
        updatePageButtons();
        updateSelectionButtons();
        super.init();
    }

    private void loadPresets() {
        ClientRarityOverrideSettings.load();
        presets.clear();
        for (ClientRarityOverrideSettings.Preset preset : ClientRarityOverrideSettings.getPresets()) {
            if (preset != null) presets.add(new PresetState(preset.name, preset.enabled, preset.overrides));
        }
        if (presets.isEmpty()) presets.add(new PresetState("Default", true, Map.of()));
        presetIndex = Math.max(0, Math.min(presetIndex, presets.size() - 1));
        loadCurrentPreset();
    }

    private void commitCurrentPreset() {
        if (presets.isEmpty()) return;
        PresetState preset = presets.get(presetIndex);
        preset.name = presetNameBox == null ? preset.name : presetNameBox.getValue();
        preset.overrides.clear();
        preset.overrides.putAll(overrides);
    }

    private void loadCurrentPreset() {
        overrides.clear();
        if (!presets.isEmpty()) {
            PresetState preset = presets.get(presetIndex);
            overrides.putAll(preset.overrides);
            if (presetNameBox != null) {
                presetNameBox.setValue(preset.name);
            }
        } else if (presetNameBox != null) {
            presetNameBox.setValue("");
        }
        selectedId = overrides.keySet().stream().findFirst().orElse("");
        rebuildGrid();
        rebuildPalette();
    }

    private void switchPreset(int delta) {
        if (!allowPresetAction() || presets.isEmpty()) return;
        commitCurrentPreset();
        presetIndex = Math.floorMod(presetIndex + delta, presets.size());
        loadCurrentPreset();
        updatePresetButtons();
        updateSelectionButtons();
    }

    private void createPreset() {
        if (!allowPresetAction()) return;
        commitCurrentPreset();
        presets.add(new PresetState("Preset " + (presets.size() + 1), true, Map.of()));
        presetIndex = presets.size() - 1;
        loadCurrentPreset();
        updatePresetButtons();
        updateSelectionButtons();
    }

    private void deletePreset() {
        if (!allowPresetAction() || presets.isEmpty()) return;
        if (presets.size() == 1) {
            presets.get(0).enabled = true;
            presets.get(0).overrides.clear();
        } else {
            presets.remove(presetIndex);
            presetIndex = Math.max(0, Math.min(presetIndex, presets.size() - 1));
        }
        loadCurrentPreset();
        updatePresetButtons();
        updateSelectionButtons();
    }

    private void importAdventurePreset() {
        if (!allowPresetAction()) return;
        Map<String, String> adventureOverrides = loadAdventurePresetOverrides();
        if (adventureOverrides.isEmpty()) return;
        commitCurrentPreset();
        int existing = -1;
        for (int i = 0; i < presets.size(); i++) {
            PresetState preset = presets.get(i);
            if (preset != null && "Adventure Mode".equalsIgnoreCase(preset.name)) {
                existing = i;
                break;
            }
        }
        PresetState adventure = new PresetState("Adventure Mode", true, adventureOverrides);
        if (existing >= 0) {
            presets.set(existing, adventure);
            presetIndex = existing;
        } else {
            presets.add(adventure);
            presetIndex = presets.size() - 1;
        }
        loadCurrentPreset();
        updatePresetButtons();
        updateSelectionButtons();
        updatePageButtons();
    }

    private void copyPresetSeed() {
        commitCurrentPreset();
        List<ClientRarityOverrideSettings.Preset> out = new ArrayList<>();
        for (PresetState preset : presets) {
            out.add(new ClientRarityOverrideSettings.Preset(preset.name, preset.enabled, preset.overrides));
        }
        ClientRarityOverrideSettings.setPresets(out);
        ClientRarityOverrideSettings.save();
        setClipboard(ClientRarityOverrideSettings.exportSeed());
    }

    private void importPresetSeedFromClipboard() {
        if (!allowPresetAction()) return;
        String seed = getClipboard();
        if (!ClientRarityOverrideSettings.importSeed(seed)) return;
        presetIndex = Integer.MAX_VALUE;
        loadPresets();
        updatePresetButtons();
        updateSelectionButtons();
        updatePageButtons();
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

    private Map<String, String> loadAdventurePresetOverrides() {
        Map<String, String> out = new LinkedHashMap<>();
        String resourcePath = "data/jamiebingo/adventure_rarity_overrides.json";
        InputStream stream = RarityChangerScreen.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) return out;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject overrides = root.getAsJsonObject("overrides");
            if (overrides == null) return out;
            for (Map.Entry<String, JsonElement> entry : overrides.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) continue;
                String rarity = BingoRarityUtil.normalize(entry.getValue().getAsString());
                if (!BingoRarityUtil.isKnown(rarity)) continue;
                out.put(entry.getKey(), rarity);
            }
        } catch (Exception ignored) {
            return Map.of();
        }
        return out;
    }

    private void toggleCurrentPresetEnabled() {
        if (presets.isEmpty()) return;
        presets.get(presetIndex).enabled = !presets.get(presetIndex).enabled;
        updatePresetButtons();
        rebuildPalette();
    }

    private boolean allowPresetAction() {
        long now = System.currentTimeMillis();
        if (now - lastPresetActionAt < PRESET_CLICK_DELAY_MS) return false;
        lastPresetActionAt = now;
        return true;
    }

    private void rebuildPalette() {
        paletteAll.clear();
        ItemDatabase.load();
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id == null || item == com.jamie.jamiebingo.util.ItemLookupUtil.item("minecraft:air")) continue;
            ItemDefinition def = ItemDatabase.getRawById(id.toString());
            paletteAll.add(new PaletteEntry(id.toString(), new ItemStack(item).getHoverName().getString(), false,
                    def == null ? "unknown" : def.category(), effectivePaletteRarity(id.toString(), def == null ? "unknown" : def.rarity())));
        }
        QuestDatabase.load();
        for (QuestDefinition quest : QuestDatabase.getQuests()) {
            if (quest != null && quest.id != null && !quest.id.isBlank()) {
                paletteAll.add(new PaletteEntry(quest.id, quest.name, true, quest.category, effectivePaletteRarity(quest.id, quest.rarity)));
            }
        }
        applyFilter();
    }

    private void applyFilter() {
        paletteFiltered.clear();
        String query = normalizeSearch(searchBox == null ? "" : searchBox.getValue());
        String rarity = currentRarityFilter();
        String category = currentCategoryFilter();
        Set<String> activeBlacklist = listedItemsOnly ? ClientBlacklistSettings.getActiveBlacklistUnion() : Set.of();
        Set<String> activeWhitelist = listedItemsOnly ? ClientBlacklistSettings.getActiveWhitelistUnion() : Set.of();
        boolean enforceWhitelist = listedItemsOnly && !activeWhitelist.isEmpty();
        for (PaletteEntry entry : paletteAll) {
            String name = normalizeSearch(entry.name);
            String id = normalizeSearch(entry.id.replace('_', ' '));
            if (!query.isBlank() && !name.contains(query) && !id.contains(query)) continue;
            if (focusMode == 1 && !entry.quest) continue;
            if (focusMode == 2 && entry.quest) continue;
            if (!"off".equals(rarity) && !rarity.equalsIgnoreCase(entry.rarity)) continue;
            if (!"off".equals(category) && !category.equalsIgnoreCase(entry.category)) continue;
            if (listedItemsOnly) {
                if (activeBlacklist.contains(entry.id)) continue;
                if (enforceWhitelist && !activeWhitelist.contains(entry.id)) continue;
            }
            paletteFiltered.add(entry);
        }
        paletteScroll = 0;
        lastSearchValue = searchBox == null ? "" : searchBox.getValue();
        updateFilterButtons();
    }

    private void rebuildGrid() {
        overrideGrid.clear();
        List<String> ids = new ArrayList<>(overrides.keySet());
        int maxPage = Math.max(0, (ids.size() - 1) / (GRID_SIZE * GRID_SIZE));
        overridePage = Math.max(0, Math.min(overridePage, maxPage));
        int from = overridePage * GRID_SIZE * GRID_SIZE;
        int to = Math.min(ids.size(), from + GRID_SIZE * GRID_SIZE);
        for (int i = from; i < to; i++) overrideGrid.add(ids.get(i));
        while (overrideGrid.size() < GRID_SIZE * GRID_SIZE) overrideGrid.add("");
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
        graphics.drawCenteredString(font, "Rarity Changer (Page " + (overridePage + 1) + "/" + (maxOverridePage() + 1) + ")", left + gridWidth / 2, 8, 0xFFFFFF);
        renderOverrideGrid(graphics);
        renderPalette(graphics);
        renderPaletteScrollBar(graphics);
        renderSelectedInfo(graphics);
        super.render(graphics, fixedMouseX, fixedMouseY, partialTick);
        renderTooltips(graphics, fixedMouseX, fixedMouseY);
        } finally {
            FixedGuiScaleUtil.endScaledRender(graphics, appliedScale);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    private void renderOverrideGrid(GuiGraphics graphics) {
        int left = 20;
        int top = listTop();
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                int idx = r * GRID_SIZE + c;
                int x = left + c * (GRID_SLOT + GRID_GAP);
                int y = top + r * (GRID_SLOT + GRID_GAP);
                graphics.fill(x, y, x + GRID_SLOT, y + GRID_SLOT, 0xFF202020);
                String id = idx < overrideGrid.size() ? overrideGrid.get(idx) : "";
                if (id == null || id.isBlank()) continue;
                renderSlotIcon(graphics, x, y, id);
                int border = id.equals(selectedId) ? 0xAAFFFF66 : 0xAA66AAFF;
                graphics.fill(x - 1, y - 1, x + GRID_SLOT + 1, y, border);
                graphics.fill(x - 1, y + GRID_SLOT, x + GRID_SLOT + 1, y + GRID_SLOT + 1, border);
                graphics.fill(x - 1, y, x, y + GRID_SLOT, border);
                graphics.fill(x + GRID_SLOT, y, x + GRID_SLOT + 1, y + GRID_SLOT, border);
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
            if (overrides.containsKey(entry.id)) {
                int border = entry.id.equals(selectedId) ? 0xAAFFFF66 : 0xAA66AAFF;
                graphics.fill(x - 1, y - 1, x + GRID_SLOT + 1, y, border);
                graphics.fill(x - 1, y + GRID_SLOT, x + GRID_SLOT + 1, y + GRID_SLOT + 1, border);
                graphics.fill(x - 1, y, x, y + GRID_SLOT, border);
                graphics.fill(x + GRID_SLOT, y, x + GRID_SLOT + 1, y + GRID_SLOT, border);
            }
        }
    }

    private void renderSelectedInfo(GuiGraphics graphics) {
        if (selectedId == null || selectedId.isBlank()) return;
        BingoSlot slot = SlotResolver.resolveSlot(selectedId);
        if (slot == null) return;
        graphics.drawString(font, "Selected: " + slot.getName(), 20, height - 42, 0xFFFFFF);
        graphics.drawString(font, "Default rarity: " + defaultRarityFor(selectedId) + " | Current rarity: " + overrides.getOrDefault(selectedId, defaultRarityFor(selectedId)), 20, height - 14, 0xCFCFCF);
    }

    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (isHovered(listedItemsButton, mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Filters the palette using your active blacklist/whitelist presets."));
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Blacklist entries are hidden. If any whitelist is active, only whitelisted entries are shown."));
            ScreenTooltipUtil.drawComponentTooltip(
                    graphics,
                    this.font,
                    tooltip,
                    mouseX,
                    mouseY,
                    this.width,
                    this.height,
                    Math.max(220, this.width - 24)
            );
            return;
        }
        String hovered = getHoveredOverrideId(mouseX, mouseY);
        if (hovered == null) hovered = getHoveredPaletteId(mouseX, mouseY);
        if (hovered == null) return;
        BingoSlot slot = SlotResolver.resolveSlot(hovered);
        if (slot == null) return;
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal(slot.getName()));
        tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("ID: " + hovered));
        tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Default rarity: " + defaultRarityFor(hovered)));
        if (overrides.containsKey(hovered)) {
            tooltip.add(com.jamie.jamiebingo.util.ComponentUtil.literal("Override rarity: " + overrides.get(hovered)));
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
        if (handlePaletteScrollbarClick(fixedEvent.x(), fixedEvent.y())) {
            return true;
        }
        String overrideId = getHoveredOverrideId((int) fixedEvent.x(), (int) fixedEvent.y());
        if (overrideId != null) {
            selectedId = overrideId;
            updateSelectionButtons();
            return true;
        }
        String paletteId = getHoveredPaletteId((int) fixedEvent.x(), (int) fixedEvent.y());
        if (paletteId != null) {
            selectedId = paletteId;
            overrides.putIfAbsent(paletteId, defaultRarityFor(paletteId));
            rebuildGrid();
            updateSelectionButtons();
            updatePageButtons();
            return true;
        }
        return super.mouseClicked(fixedEvent, isNew);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        double fixedMouseX = FixedGuiScaleUtil.virtualMouseX(mouseX, this.minecraft);
        double fixedMouseY = FixedGuiScaleUtil.virtualMouseY(mouseY, this.minecraft);
        if (isOverPalette(fixedMouseX, fixedMouseY)) {
            int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
            int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
            paletteScroll = Math.max(0, Math.min(maxScroll, paletteScroll + (deltaY > 0 ? -1 : 1)));
            return true;
        }
        return super.mouseScrolled(fixedMouseX, fixedMouseY, deltaX, deltaY);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        MouseButtonEvent fixedEvent = FixedGuiScaleUtil.virtualEvent(event, this.minecraft);
        double fixedDragX = FixedGuiScaleUtil.virtualDelta(dragX, this.minecraft);
        double fixedDragY = FixedGuiScaleUtil.virtualDelta(dragY, this.minecraft);
        if (!draggingPaletteScroll) {
            return super.mouseDragged(fixedEvent, fixedDragX, fixedDragY);
        }
        int top = paletteTop();
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;
        int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
        int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
        if (maxScroll <= 0) return true;
        float visibleRatio = Math.min(1.0f, PALETTE_ROWS / (float) maxRows);
        int handleH = Math.max(10, (int) ((paletteHeight - 2) * visibleRatio));
        updatePaletteScrollFromHandle(top, paletteHeight, handleH, (int) fixedEvent.y() - paletteScrollDragOffset);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingPaletteScroll = false;
        return super.mouseReleased(FixedGuiScaleUtil.virtualEvent(event, this.minecraft));
    }

    private void applyFixedScreenSize() {
        if (this.minecraft == null) return;
        this.width = FixedGuiScaleUtil.virtualWidth(this.minecraft, this.width);
        this.height = FixedGuiScaleUtil.virtualHeight(this.minecraft, this.height);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return presetNameBox != null && presetNameBox.keyPressed(event)
                || searchBox != null && searchBox.keyPressed(event)
                || super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return presetNameBox != null && presetNameBox.charTyped(event)
                || searchBox != null && searchBox.charTyped(event)
                || super.charTyped(event);
    }

    @Override
    public void tick() {
        if (searchBox != null && !searchBox.getValue().equals(lastSearchValue)) applyFilter();
        super.tick();
    }

    @Override
    public void onClose() {
        commitCurrentPreset();
        List<ClientRarityOverrideSettings.Preset> out = new ArrayList<>();
        for (PresetState preset : presets) out.add(new ClientRarityOverrideSettings.Preset(preset.name, preset.enabled, preset.overrides));
        ClientRarityOverrideSettings.setPresets(out);
        ClientRarityOverrideSettings.save();
        super.onClose();
    }

    private void saveAndClose() {
        commitCurrentPreset();
        List<ClientRarityOverrideSettings.Preset> out = new ArrayList<>();
        for (PresetState preset : presets) {
            out.add(new ClientRarityOverrideSettings.Preset(preset.name, preset.enabled, preset.overrides));
        }
        ClientRarityOverrideSettings.setPresets(out);
        ClientRarityOverrideSettings.save();
        sendOverridesInChunks(ClientRarityOverrideSettings.getActiveOverrides());
        super.onClose();
    }

    private void sendOverridesInChunks(Map<String, String> allOverrides) {
        if (allOverrides == null || allOverrides.isEmpty()) {
            NetworkHandler.sendToServer(new PacketSetRarityOverridesChunk(true, true, Map.of()));
            return;
        }
        List<Map.Entry<String, String>> entries = new ArrayList<>();
        for (Map.Entry<String, String> entry : allOverrides.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) continue;
            entries.add(Map.entry(entry.getKey(), BingoRarityUtil.normalize(entry.getValue())));
        }
        if (entries.isEmpty()) {
            NetworkHandler.sendToServer(new PacketSetRarityOverridesChunk(true, true, Map.of()));
            return;
        }
        Map<String, String> chunk = new LinkedHashMap<>();
        boolean first = true;
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, String> entry = entries.get(i);
            chunk.put(entry.getKey(), entry.getValue());
            if (chunk.size() >= MAX_OVERRIDES_PER_PACKET) {
                boolean finalChunk = (i == entries.size() - 1);
                NetworkHandler.sendToServer(new PacketSetRarityOverridesChunk(first, finalChunk, chunk));
                chunk = new LinkedHashMap<>();
                first = false;
            }
        }
        if (!chunk.isEmpty()) {
            NetworkHandler.sendToServer(new PacketSetRarityOverridesChunk(first, true, chunk));
        }
    }

    private void cycleSelectedRarity(int delta) {
        if (selectedId == null || selectedId.isBlank()) return;
        List<String> rarities = BingoRarityUtil.ORDERED_RARITIES;
        String current = overrides.getOrDefault(selectedId, defaultRarityFor(selectedId));
        int index = rarities.indexOf(BingoRarityUtil.normalize(current));
        index = index < 0 ? 0 : index;
        overrides.put(selectedId, rarities.get(Math.floorMod(index + delta, rarities.size())));
        rebuildPalette();
        updateSelectionButtons();
    }

    private void resetSelectedOverride() {
        if (selectedId == null || selectedId.isBlank()) return;
        overrides.remove(selectedId);
        selectedId = overrides.keySet().stream().findFirst().orElse("");
        rebuildGrid();
        rebuildPalette();
        updateSelectionButtons();
        updatePageButtons();
    }

    private void updateSelectionButtons() {
        boolean active = selectedId != null && !selectedId.isBlank() && overrides.containsKey(selectedId);
        selectedRarityButton.active = active;
        resetOverrideButton.active = active;
        selectedRarityButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Selected Rarity: " + (active ? overrides.get(selectedId) : "none")));
    }

    private void updatePresetButtons() {
        boolean many = presets.size() > 1;
        prevPresetButton.active = many;
        nextPresetButton.active = many;
        deletePresetButton.active = !presets.isEmpty();
        PresetState current = presets.isEmpty() ? null : presets.get(presetIndex);
        if (presetNameBox != null) {
            presetNameBox.setEditable(current != null);
            if (current != null && !presetNameBox.getValue().equals(current.name)) {
                presetNameBox.setValue(current.name);
            }
        }
        togglePresetButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(current == null ? "Preset: Disabled" : "Preset: " + (current.enabled ? "Enabled" : "Disabled")));
        if (listedItemsButton != null) {
            listedItemsButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(listedItemsLabel()));
        }
    }

    private void updatePageButtons() {
        prevPageButton.active = overridePage > 0;
        nextPageButton.active = overridePage < maxOverridePage();
    }

    private int maxOverridePage() {
        return Math.max(0, (overrides.size() - 1) / (GRID_SIZE * GRID_SIZE));
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

    private void toggleListedItemsOnly() {
        listedItemsOnly = !listedItemsOnly;
        if (listedItemsButton != null) {
            listedItemsButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(listedItemsLabel()));
        }
        applyFilter();
    }

    private String listedItemsLabel() {
        return "Toggle Listed Items: " + (listedItemsOnly ? "On" : "Off");
    }

    private List<String> rarityOptions() {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add("off");
        out.addAll(BingoRarityUtil.ORDERED_RARITIES);
        for (PaletteEntry entry : paletteAll) if (entry.rarity != null && !entry.rarity.isBlank()) out.add(entry.rarity);
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

    private boolean isOverPalette(double mouseX, double mouseY) {
        int left = 20;
        int top = paletteTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int paletteWidth = PALETTE_COLS * GRID_SLOT + (PALETTE_COLS - 1) * GRID_GAP;
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;
        return mouseX >= paletteX && mouseX <= paletteX + paletteWidth && mouseY >= top && mouseY <= top + paletteHeight;
    }

    private String getHoveredOverrideId(int mouseX, int mouseY) {
        int left = 20;
        int top = listTop();
        int size = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        if (mouseX < left || mouseX > left + size || mouseY < top || mouseY > top + size) return null;
        int idx = ((mouseY - top) / (GRID_SLOT + GRID_GAP)) * GRID_SIZE + ((mouseX - left) / (GRID_SLOT + GRID_GAP));
        if (idx < 0 || idx >= overrideGrid.size()) return null;
        String id = overrideGrid.get(idx);
        return id == null || id.isBlank() ? null : id;
    }

    private String getHoveredPaletteId(int mouseX, int mouseY) {
        int left = 20;
        int top = paletteTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int paletteWidth = PALETTE_COLS * GRID_SLOT + (PALETTE_COLS - 1) * GRID_GAP;
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;
        if (mouseX < paletteX || mouseX > paletteX + paletteWidth || mouseY < top || mouseY > top + paletteHeight) return null;
        int col = (mouseX - paletteX) / (GRID_SLOT + GRID_GAP);
        int row = (mouseY - top) / (GRID_SLOT + GRID_GAP);
        int idx = paletteScroll * PALETTE_COLS + row * PALETTE_COLS + col;
        if (idx < 0 || idx >= paletteFiltered.size()) return null;
        return paletteFiltered.get(idx).id;
    }

    private String defaultRarityFor(String id) {
        if (id.startsWith("quest.")) {
            QuestDefinition quest = QuestDatabase.getQuestById(id);
            return quest == null || quest.rarity == null || quest.rarity.isBlank() ? "impossible" : quest.rarity.toLowerCase(Locale.ROOT);
        }
        ItemDefinition def = ItemDatabase.getRawById(id);
        return def == null || def.rarity() == null || def.rarity().isBlank() ? "impossible" : def.rarity().toLowerCase(Locale.ROOT);
    }

    private String effectivePaletteRarity(String id, String fallback) {
        String normalizedFallback = fallback == null || fallback.isBlank() ? "impossible" : fallback.toLowerCase(Locale.ROOT);
        Map<String, String> effective = new LinkedHashMap<>();
        for (int i = 0; i < presets.size(); i++) {
            PresetState preset = presets.get(i);
            if (preset == null || !preset.enabled) continue;
            effective.putAll(preset.overrides);
            if (i == presetIndex) {
                effective.putAll(overrides);
            }
        }
        return effective.getOrDefault(id, normalizedFallback);
    }

    private int paletteTop() {
        return TOP + PALETTE_TOP_OFFSET;
    }

    private int listTop() {
        return LIST_TOP;
    }

    private void renderPaletteScrollBar(GuiGraphics graphics) {
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

    private boolean handlePaletteScrollbarClick(double mouseX, double mouseY) {
        int left = 20;
        int top = paletteTop();
        int gridWidth = GRID_SIZE * GRID_SLOT + (GRID_SIZE - 1) * GRID_GAP;
        int paletteX = left + gridWidth + 30;
        int paletteWidth = PALETTE_COLS * GRID_SLOT + (PALETTE_COLS - 1) * GRID_GAP;
        int paletteHeight = PALETTE_ROWS * GRID_SLOT + (PALETTE_ROWS - 1) * GRID_GAP;
        int trackX = paletteX + paletteWidth + 6;
        int trackY = top;
        int trackH = paletteHeight;
        int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
        int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
        if (maxScroll <= 0) return false;
        float visibleRatio = Math.min(1.0f, PALETTE_ROWS / (float) maxRows);
        int handleH = Math.max(10, (int) ((trackH - 2) * visibleRatio));
        int maxHandleY = trackH - 2 - handleH;
        int handleY = trackY + 1 + Math.round(maxHandleY * (paletteScroll / (float) maxScroll));
        if (mouseX >= trackX && mouseX <= trackX + 6 && mouseY >= trackY && mouseY <= trackY + trackH) {
            if (mouseY >= handleY && mouseY <= handleY + handleH) {
                draggingPaletteScroll = true;
                paletteScrollDragOffset = (int) mouseY - handleY;
            } else {
                updatePaletteScrollFromHandle(trackY, trackH, handleH, (int) mouseY - handleH / 2);
            }
            return true;
        }
        return false;
    }

    private void updatePaletteScrollFromHandle(int trackY, int trackH, int handleH, int handleY) {
        int maxRows = (int) Math.ceil(paletteFiltered.size() / (double) PALETTE_COLS);
        int maxScroll = Math.max(0, maxRows - PALETTE_ROWS);
        if (maxScroll <= 0) return;
        int minY = trackY + 1;
        int maxY = trackY + trackH - 1 - handleH;
        int clamped = Math.max(minY, Math.min(maxY, handleY));
        float ratio = (clamped - minY) / (float) Math.max(1, maxY - minY);
        paletteScroll = Math.max(0, Math.min(maxScroll, Math.round(ratio * maxScroll)));
    }

    private void renderSlotIcon(GuiGraphics graphics, int x, int y, String id) {
        if (id.startsWith("quest.")) {
            BingoSlot slot = SlotResolver.resolveSlot(id);
            if (slot != null) renderQuestIconSharp(graphics, x, y, QuestIconProvider.iconFor(slot), GRID_SLOT);
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

    private String normalizeSearch(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replace('_', ' ').replaceAll("\\s+", " ").trim();
    }

    private Button button(int x, int y, int w, String text, Runnable action) {
        return com.jamie.jamiebingo.util.ButtonUtil.builder(com.jamie.jamiebingo.util.ComponentUtil.literal(text), b -> action.run())
                .pos(x, y).size(w, 18).build();
    }

    private Button cycleButton(int x, int y, String text, Runnable action) {
        return com.jamie.jamiebingo.util.ButtonUtil.builder(com.jamie.jamiebingo.util.ComponentUtil.literal(text), b -> action.run())
                .pos(x, y).size(18, 16).build();
    }

    private Button labelButton(int x, int y, int w, String text) {
        Button button = com.jamie.jamiebingo.util.ButtonUtil.builder(com.jamie.jamiebingo.util.ComponentUtil.literal(text), b -> {})
                .pos(x, y).size(w, 16).build();
        button.active = false;
        return button;
    }

    private boolean isHovered(Button button, int mouseX, int mouseY) {
        return button != null
                && button.visible
                && mouseX >= button.getX()
                && mouseX < button.getX() + button.getWidth()
                && mouseY >= button.getY()
                && mouseY < button.getY() + button.getHeight();
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
        final Map<String, String> overrides = new LinkedHashMap<>();

        private PresetState(String name, boolean enabled, Map<String, String> overrides) {
            this.name = name == null || name.isBlank() ? "Preset" : name;
            this.enabled = enabled;
            if (overrides != null) {
                for (Map.Entry<String, String> entry : overrides.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().isBlank() && BingoRarityUtil.isKnown(entry.getValue())) {
                        this.overrides.put(entry.getKey(), BingoRarityUtil.normalize(entry.getValue()));
                    }
                }
            }
        }
    }
}
