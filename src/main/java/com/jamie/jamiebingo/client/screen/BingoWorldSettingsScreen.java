package com.jamie.jamiebingo.client.screen;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketSaveWorldSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BingoWorldSettingsScreen extends Screen {
    private static final int MIN_BIOME_SIZE = 40;
    private static final int MAX_BIOME_SIZE = 100;
    private static final int BIOME_SIZE_RANGE = MAX_BIOME_SIZE - MIN_BIOME_SIZE;
    private static final int MIN_TERRAIN_HILLYNESS = 0;
    private static final int MAX_TERRAIN_HILLYNESS = 100;
    private static final int TERRAIN_HILLYNESS_RANGE = MAX_TERRAIN_HILLYNESS - MIN_TERRAIN_HILLYNESS;
    private static final int MIN_STRUCTURE_FREQUENCY = 25;
    private static final int MAX_STRUCTURE_FREQUENCY = 500;
    private static final int STRUCTURE_FREQUENCY_RANGE = MAX_STRUCTURE_FREQUENCY - MIN_STRUCTURE_FREQUENCY;

    private static boolean initialNewSeedEachGame;
    private static int initialWorldTypeMode;
    private static int initialWorldCustomBiomeSizeBlocks = 96;
    private static int initialWorldTerrainHillinessPercent = 50;
    private static int initialWorldStructureFrequencyPercent = 100;
    private static String initialSingleBiomeId = "minecraft:plains";
    private static boolean initialSurfaceCaveBiomes = false;
    private static String initialSetSeedText = "";
    private static boolean initialAdventureMode = false;
    private static int initialPrelitPortalsMode = BingoGameData.PRELIT_PORTALS_OFF;

    private static final int[] WORLD_TYPE_CYCLE = new int[] {
            BingoGameData.WORLD_TYPE_NORMAL,
            BingoGameData.WORLD_TYPE_AMPLIFIED,
            BingoGameData.WORLD_TYPE_SUPERFLAT,
            BingoGameData.WORLD_TYPE_SINGLE_BIOME,
            BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE
    };

    private boolean newSeedEachGame;
    private int worldTypeMode;
    private int worldCustomBiomeSizeBlocks;
    private int worldTerrainHillinessPercent;
    private int worldStructureFrequencyPercent;
    private String singleBiomeId;
    private boolean surfaceCaveBiomes;
    private String setSeedText;
    private boolean adventureMode;
    private int prelitPortalsMode;
    private List<Identifier> cachedBiomes;

    private Button modeButton;
    private Button worldTypeLeftButton;
    private Button worldTypeCenterButton;
    private Button worldTypeRightButton;
    private Button biomeLeftButton;
    private Button biomeCenterButton;
    private Button biomeRightButton;
    private BiomeSizeSlider biomeSizeSlider;
    private int biomeSizeSliderBaseX;
    private int biomeSizeSliderBaseY;
    private TerrainHillinessSlider terrainHillinessSlider;
    private int terrainHillinessSliderBaseX;
    private int terrainHillinessSliderBaseY;
    private StructureFrequencySlider structureFrequencySlider;
    private Button surfaceCavesButton;
    private Button prelitLeftButton;
    private Button prelitCenterButton;
    private Button prelitRightButton;
    private Button generateFromSeedButton;
    private Button pasteSeedButton;
    private EditBox seedBox;

    public static void openWithState(
            boolean newSeedEachGame,
            int worldTypeMode,
            int worldCustomBiomeSizeBlocks,
            int worldTerrainHillinessPercent,
            int worldStructureFrequencyPercent,
            String singleBiomeId,
            boolean surfaceCaveBiomes,
            String setSeedText,
            boolean adventureMode,
            int prelitPortalsMode
    ) {
        initialNewSeedEachGame = newSeedEachGame;
        initialWorldTypeMode = worldTypeMode;
        initialWorldCustomBiomeSizeBlocks = clampBiomeSize(worldCustomBiomeSizeBlocks);
        initialWorldTerrainHillinessPercent = clampTerrainHilliness(worldTerrainHillinessPercent);
        initialWorldStructureFrequencyPercent = clampStructureFrequency(worldStructureFrequencyPercent);
        initialSingleBiomeId = (singleBiomeId == null || singleBiomeId.isBlank()) ? "minecraft:plains" : singleBiomeId;
        initialSurfaceCaveBiomes = surfaceCaveBiomes;
        initialSetSeedText = setSeedText == null ? "" : setSeedText;
        initialAdventureMode = adventureMode;
        initialPrelitPortalsMode = BingoGameData.clampPrelitPortalsMode(prelitPortalsMode);
    }

    public BingoWorldSettingsScreen() {
        super(com.jamie.jamiebingo.util.ComponentUtil.literal("Bingo World Settings"));
        this.newSeedEachGame = initialNewSeedEachGame;
        this.worldTypeMode = clampWorldType(initialWorldTypeMode);
        this.worldCustomBiomeSizeBlocks = clampBiomeSize(initialWorldCustomBiomeSizeBlocks);
        this.worldTerrainHillinessPercent = clampTerrainHilliness(initialWorldTerrainHillinessPercent);
        this.worldStructureFrequencyPercent = clampStructureFrequency(initialWorldStructureFrequencyPercent);
        this.singleBiomeId = initialSingleBiomeId;
        this.surfaceCaveBiomes = initialSurfaceCaveBiomes;
        this.setSeedText = initialSetSeedText;
        this.adventureMode = initialAdventureMode;
        this.prelitPortalsMode = BingoGameData.clampPrelitPortalsMode(initialPrelitPortalsMode);
        this.cachedBiomes = listOverworldBiomeIds();
        if (this.cachedBiomes.isEmpty()) {
            this.cachedBiomes = List.of(Identifier.parse("minecraft:plains"));
        }
        if (!containsBiome(this.singleBiomeId)) {
            this.singleBiomeId = "minecraft:plains";
        }
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = this.height / 2 - 112;
        int w = 260;

        modeButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(modeLabel()),
                        b -> {
                            newSeedEachGame = !newSeedEachGame;
                            if (!newSeedEachGame) {
                                prelitPortalsMode = BingoGameData.PRELIT_PORTALS_OFF;
                            }
                            refreshLabels();
                        })
                .pos(centerX - w / 2, y)
                .size(w, 20)
                .build());
        y += 24;

        worldTypeLeftButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("<"),
                        b -> {
                            if (!newSeedEachGame) return;
                            worldTypeMode = prevWorldType(worldTypeMode);
                            refreshLabels();
                        })
                .pos(centerX - w / 2, y)
                .size(22, 20)
                .build());
        worldTypeCenterButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(worldTypeLabel()),
                        b -> {})
                .pos(centerX - w / 2 + 24, y)
                .size(w - 48, 20)
                .build());
        worldTypeRightButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(">"),
                        b -> {
                            if (!newSeedEachGame) return;
                            worldTypeMode = nextWorldType(worldTypeMode);
                            refreshLabels();
                        })
                .pos(centerX + w / 2 - 22, y)
                .size(22, 20)
                .build());
        y += 24;

        biomeLeftButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("<"),
                        b -> {
                            if (!newSeedEachGame || worldTypeMode != BingoGameData.WORLD_TYPE_SINGLE_BIOME) return;
                            cycleBiome(-1);
                            refreshLabels();
                        })
                .pos(centerX - w / 2, y)
                .size(22, 20)
                .build());
        biomeCenterButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(singleBiomeLabel()),
                        b -> {})
                .pos(centerX - w / 2 + 24, y)
                .size(w - 48, 20)
                .build());
        biomeRightButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(">"),
                        b -> {
                            if (!newSeedEachGame || worldTypeMode != BingoGameData.WORLD_TYPE_SINGLE_BIOME) return;
                            cycleBiome(1);
                            refreshLabels();
                        })
                .pos(centerX + w / 2 - 22, y)
                .size(22, 20)
                .build());
        y += 24;

        biomeSizeSliderBaseX = centerX - w / 2;
        biomeSizeSliderBaseY = y;
        biomeSizeSlider = addRenderableWidget(new BiomeSizeSlider(biomeSizeSliderBaseX, biomeSizeSliderBaseY, w, 20, worldCustomBiomeSizeBlocks, this::onBiomeSizeChanged));
        y += 24;

        terrainHillinessSliderBaseX = centerX - w / 2;
        terrainHillinessSliderBaseY = y;
        terrainHillinessSlider = addRenderableWidget(new TerrainHillinessSlider(
                terrainHillinessSliderBaseX,
                terrainHillinessSliderBaseY,
                w,
                20,
                worldTerrainHillinessPercent,
                this::onTerrainHillinessChanged
        ));
        y += 24;

        structureFrequencySlider = addRenderableWidget(new StructureFrequencySlider(
                centerX - w / 2,
                y,
                w,
                20,
                worldStructureFrequencyPercent,
                this::onStructureFrequencyChanged
        ));
        y += 24;

        surfaceCavesButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(surfaceCaveLabel()),
                        b -> {
                            if (!newSeedEachGame) return;
                            surfaceCaveBiomes = !surfaceCaveBiomes;
                            refreshLabels();
                        })
                .pos(centerX - w / 2, y)
                .size(w, 20)
                .build());
        y += 24;

        prelitLeftButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("<"),
                        b -> {
                            if (!newSeedEachGame) return;
                            prelitPortalsMode = prevPrelitPortalsMode(prelitPortalsMode);
                            refreshLabels();
                        })
                .pos(centerX - w / 2, y)
                .size(22, 20)
                .build());
        prelitCenterButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(prelitPortalsLabel()),
                        b -> {})
                .pos(centerX - w / 2 + 24, y)
                .size(w - 48, 20)
                .build());
        prelitRightButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal(">"),
                        b -> {
                            if (!newSeedEachGame) return;
                            prelitPortalsMode = nextPrelitPortalsMode(prelitPortalsMode);
                            refreshLabels();
                        })
                .pos(centerX + w / 2 - 22, y)
                .size(22, 20)
                .build());
        y += 24;

        int pasteW = 44;
        int seedBoxW = 72;
        int generateW = 136;
        int x0 = centerX - w / 2;
        pasteSeedButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Paste"),
                        b -> {
                            net.minecraft.client.Minecraft mc = com.jamie.jamiebingo.client.ClientMinecraftUtil.getMinecraft();
                            String clip = "";
                            if (mc != null && mc.keyboardHandler != null) {
                                clip = mc.keyboardHandler.getClipboard();
                            }
                            if (seedBox != null) {
                                seedBox.setValue(clip == null ? "" : clip.trim());
                            }
                        })
                .pos(x0, y)
                .size(pasteW, 20)
                .build());

        seedBox = new EditBox(this.font, x0 + pasteW + 4, y, seedBoxW, 20, com.jamie.jamiebingo.util.ComponentUtil.literal("Set Seed"));
        seedBox.setMaxLength(128);
        seedBox.setValue(setSeedText == null ? "" : setSeedText);
        addRenderableWidget(seedBox);
        setFocused(seedBox);
        setInitialFocus(seedBox);
        generateFromSeedButton = addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Generate From Set Seed"),
                        b -> {
                            setSeedText = seedBox == null ? "" : seedBox.getValue().trim();
                            NetworkHandler.sendToServer(new PacketSaveWorldSettings(
                                    newSeedEachGame,
                                    worldTypeMode,
                                    worldCustomBiomeSizeBlocks,
                                    worldTerrainHillinessPercent,
                                    worldStructureFrequencyPercent,
                                    singleBiomeId,
                                    surfaceCaveBiomes,
                                    setSeedText,
                                    true,
                                    adventureMode,
                                    prelitPortalsMode
                            ));
                            onClose();
                        })
                .pos(x0 + pasteW + 4 + seedBoxW + 4, y)
                .size(generateW, 20)
                .build());
        y += 28;

        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Generate New Seed"),
                        b -> {
                            NetworkHandler.sendToServer(new PacketSaveWorldSettings(
                                    newSeedEachGame,
                                    worldTypeMode,
                                    worldCustomBiomeSizeBlocks,
                                    worldTerrainHillinessPercent,
                                    worldStructureFrequencyPercent,
                                    singleBiomeId,
                                    surfaceCaveBiomes,
                                    "",
                                    true,
                                    adventureMode,
                                    prelitPortalsMode
                            ));
                            onClose();
                        })
                .pos(centerX - 154, y)
                .size(96, 20)
                .build());

        addRenderableWidget(com.jamie.jamiebingo.util.ButtonUtil.builder(
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Cancel"),
                        b -> onClose())
                .pos(centerX + 58, y)
                .size(96, 20)
                .build());

        refreshLabels();
        syncDynamicControls();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        syncScreenSizeFromWindow();
        if (this.children().isEmpty()) {
            this.init(this.width, this.height);
        }
        syncDynamicControls();
        renderBlurredBackground(graphics);
        graphics.fill(0, 0, this.width, this.height, 0xB0101216);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, this.height / 2 - 124, 0xFFFFFF);
        graphics.drawCenteredString(
                this.font,
                "Set Seed: leave empty for random seed generation.",
                centerX,
                this.height / 2 + 40,
                0xA0A0A0
        );
        graphics.drawCenteredString(
                this.font,
                "Generate regenerates dimensions immediately using these settings.",
                centerX,
                this.height / 2 + 52,
                0x7A7A7A
        );
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Avoid double-blur from Screen.render calling renderBackground again.
    }

    @Override
    public void tick() {
        super.tick();
        syncDynamicControls();
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

    private void syncScreenSizeFromWindow() {
        if (this.minecraft == null) return;
        Object window = com.jamie.jamiebingo.client.ClientMinecraftUtil.getWindow(this.minecraft);
        int w = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledWidth(window);
        int h = com.jamie.jamiebingo.util.WindowUtil.getGuiScaledHeight(window);
        if (w > 0) this.width = w;
        if (h > 0) this.height = h;
    }

    private void syncDynamicControls() {
        boolean singleBiomeActive = newSeedEachGame && worldTypeMode == BingoGameData.WORLD_TYPE_SINGLE_BIOME;
        if (biomeLeftButton != null) {
            biomeLeftButton.visible = singleBiomeActive;
            biomeLeftButton.active = singleBiomeActive;
        }
        if (biomeCenterButton != null) {
            biomeCenterButton.visible = singleBiomeActive;
            biomeCenterButton.active = false;
        }
        if (biomeRightButton != null) {
            biomeRightButton.visible = singleBiomeActive;
            biomeRightButton.active = singleBiomeActive;
        }

        boolean customBiomeSizeActive = newSeedEachGame && worldTypeMode == BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE;
        if (biomeSizeSlider != null) {
            biomeSizeSlider.setInteractiveState(customBiomeSizeActive);
            if (customBiomeSizeActive) {
                biomeSizeSlider.setX(biomeSizeSliderBaseX);
                biomeSizeSlider.setY(biomeSizeSliderBaseY);
                biomeSizeSlider.refreshFromValue(worldCustomBiomeSizeBlocks);
            } else {
                biomeSizeSlider.setX(-10000);
                biomeSizeSlider.setY(-10000);
            }
        }
        if (terrainHillinessSlider != null) {
            terrainHillinessSlider.setInteractiveState(customBiomeSizeActive);
            if (customBiomeSizeActive) {
                terrainHillinessSlider.setX(terrainHillinessSliderBaseX);
                terrainHillinessSlider.setY(terrainHillinessSliderBaseY);
                terrainHillinessSlider.refreshFromValue(worldTerrainHillinessPercent);
            } else {
                terrainHillinessSlider.setX(-10000);
                terrainHillinessSlider.setY(-10000);
            }
        }
        if (structureFrequencySlider != null) {
            structureFrequencySlider.setInteractiveState(newSeedEachGame);
            structureFrequencySlider.refreshFromValue(worldStructureFrequencyPercent);
        }
    }

    private void refreshLabels() {
        if (modeButton != null) {
            modeButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(modeLabel()));
        }
        if (worldTypeCenterButton != null) {
            worldTypeCenterButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(worldTypeLabel()));
            worldTypeCenterButton.active = newSeedEachGame;
        }
        if (worldTypeLeftButton != null) {
            worldTypeLeftButton.active = newSeedEachGame;
        }
        if (worldTypeRightButton != null) {
            worldTypeRightButton.active = newSeedEachGame;
        }
        if (biomeCenterButton != null) {
            biomeCenterButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(singleBiomeLabel()));
            biomeCenterButton.active = newSeedEachGame;
        }

        if (surfaceCavesButton != null) {
            surfaceCavesButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(surfaceCaveLabel()));
            surfaceCavesButton.active = newSeedEachGame;
        }
        if (prelitCenterButton != null) {
            prelitCenterButton.setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal(prelitPortalsLabel()));
            prelitCenterButton.active = newSeedEachGame;
        }
        if (prelitLeftButton != null) {
            prelitLeftButton.active = newSeedEachGame;
        }
        if (prelitRightButton != null) {
            prelitRightButton.active = newSeedEachGame;
        }
        if (seedBox != null) {
            seedBox.setEditable(true);
        }
        if (generateFromSeedButton != null) {
            generateFromSeedButton.active = newSeedEachGame;
        }
        syncDynamicControls();
    }

    private String modeLabel() {
        return "Seed Mode: " + (newSeedEachGame ? "New Seed Every Game" : "Same Seed + Teleport Far");
    }

    private String worldTypeLabel() {
        return "World Type: " + worldTypeName(worldTypeMode) + (newSeedEachGame ? "" : " (new-seed only)");
    }

    private String singleBiomeLabel() {
        String pretty = biomeDisplayName(singleBiomeId);
        if (worldTypeMode != BingoGameData.WORLD_TYPE_SINGLE_BIOME) {
            return "Single Biome: " + pretty + " (single-biome only)";
        }
        return "Single Biome: " + pretty + (newSeedEachGame ? "" : " (new-seed only)");
    }

    private static int clampWorldType(int mode) {
        if (mode == BingoGameData.WORLD_TYPE_SMALL_BIOMES) {
            return BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE;
        }
        for (int type : WORLD_TYPE_CYCLE) {
            if (type == mode) return type;
        }
        return BingoGameData.WORLD_TYPE_NORMAL;
    }

    private static int nextWorldType(int current) {
        int cur = clampWorldType(current);
        for (int i = 0; i < WORLD_TYPE_CYCLE.length; i++) {
            if (WORLD_TYPE_CYCLE[i] == cur) {
                return WORLD_TYPE_CYCLE[(i + 1) % WORLD_TYPE_CYCLE.length];
            }
        }
        return WORLD_TYPE_CYCLE[0];
    }

    private static int prevWorldType(int current) {
        int cur = clampWorldType(current);
        for (int i = 0; i < WORLD_TYPE_CYCLE.length; i++) {
            if (WORLD_TYPE_CYCLE[i] == cur) {
                return WORLD_TYPE_CYCLE[(i - 1 + WORLD_TYPE_CYCLE.length) % WORLD_TYPE_CYCLE.length];
            }
        }
        return WORLD_TYPE_CYCLE[0];
    }

    private static String worldTypeName(int mode) {
        return switch (clampWorldType(mode)) {
            case BingoGameData.WORLD_TYPE_AMPLIFIED -> "Amplified";
            case BingoGameData.WORLD_TYPE_SUPERFLAT -> "Superflat";
            case BingoGameData.WORLD_TYPE_SINGLE_BIOME -> "Single Biome";
            case BingoGameData.WORLD_TYPE_CUSTOM_BIOME_SIZE -> "Custom Biome Size";
            default -> "Normal";
        };
    }

    private static int clampBiomeSize(int blocks) {
        return BingoGameData.clampWorldBiomeSize(blocks);
    }

    private static int clampTerrainHilliness(int percent) {
        return BingoGameData.clampWorldTerrainHilliness(percent);
    }

    private static int clampStructureFrequency(int percent) {
        return BingoGameData.clampWorldStructureFrequency(percent);
    }

    private String surfaceCaveLabel() {
        return "Surface Cave Biomes: " + (surfaceCaveBiomes ? "Enabled" : "Disabled") + (newSeedEachGame ? "" : " (new-seed only)");
    }

    private String prelitPortalsLabel() {
        String value = switch (BingoGameData.clampPrelitPortalsMode(prelitPortalsMode)) {
            case BingoGameData.PRELIT_PORTALS_NETHER -> "Nether";
            case BingoGameData.PRELIT_PORTALS_END -> "End";
            case BingoGameData.PRELIT_PORTALS_BOTH -> "Both";
            default -> "Off";
        };
        return "Prelit Portals: " + value + (newSeedEachGame ? "" : " (new-seed only)");
    }

    private static int nextPrelitPortalsMode(int mode) {
        int clamped = BingoGameData.clampPrelitPortalsMode(mode);
        return clamped >= BingoGameData.PRELIT_PORTALS_BOTH ? BingoGameData.PRELIT_PORTALS_OFF : clamped + 1;
    }

    private static int prevPrelitPortalsMode(int mode) {
        int clamped = BingoGameData.clampPrelitPortalsMode(mode);
        return clamped <= BingoGameData.PRELIT_PORTALS_OFF ? BingoGameData.PRELIT_PORTALS_BOTH : clamped - 1;
    }

    private void cycleBiome(int direction) {
        if (cachedBiomes == null || cachedBiomes.isEmpty()) return;
        int idx = 0;
        Identifier current;
        try {
            current = Identifier.parse(singleBiomeId);
        } catch (Throwable t) {
            current = cachedBiomes.get(0);
        }
        int found = cachedBiomes.indexOf(current);
        if (found >= 0) idx = found;
        int next = (idx + (direction >= 0 ? 1 : -1) + cachedBiomes.size()) % cachedBiomes.size();
        singleBiomeId = cachedBiomes.get(next).toString();
    }

    private void onBiomeSizeChanged(int value) {
        worldCustomBiomeSizeBlocks = clampBiomeSize(value);
    }

    private void onTerrainHillinessChanged(int value) {
        worldTerrainHillinessPercent = clampTerrainHilliness(value);
    }

    private void onStructureFrequencyChanged(int value) {
        worldStructureFrequencyPercent = clampStructureFrequency(value);
    }

    private List<Identifier> listOverworldBiomeIds() {
        List<Identifier> out = new ArrayList<>();
        String[] ids = new String[] {
                "minecraft:badlands", "minecraft:bamboo_jungle", "minecraft:beach",
                "minecraft:birch_forest", "minecraft:cherry_grove", "minecraft:cold_ocean",
                "minecraft:dark_forest", "minecraft:deep_cold_ocean", "minecraft:deep_dark", "minecraft:deep_frozen_ocean",
                "minecraft:deep_lukewarm_ocean", "minecraft:deep_ocean", "minecraft:deep_sparse_jungle",
                "minecraft:deep_warm_ocean", "minecraft:desert", "minecraft:dripstone_caves", "minecraft:eroded_badlands", "minecraft:flower_forest",
                "minecraft:forest", "minecraft:frozen_ocean", "minecraft:frozen_peaks", "minecraft:frozen_river",
                "minecraft:grove", "minecraft:ice_spikes", "minecraft:jagged_peaks", "minecraft:jungle",
                "minecraft:lukewarm_ocean", "minecraft:lush_caves", "minecraft:mangrove_swamp", "minecraft:meadow",
                "minecraft:mushroom_fields", "minecraft:ocean", "minecraft:old_growth_birch_forest",
                "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:plains",
                "minecraft:river", "minecraft:savanna", "minecraft:savanna_plateau",
                "minecraft:snowy_beach", "minecraft:snowy_plains", "minecraft:snowy_slopes", "minecraft:snowy_taiga",
                "minecraft:sparse_jungle", "minecraft:stony_peaks", "minecraft:stony_shore",
                "minecraft:sunflower_plains", "minecraft:swamp", "minecraft:taiga",
                "minecraft:warm_ocean", "minecraft:windswept_forest",
                "minecraft:windswept_gravelly_hills", "minecraft:windswept_hills", "minecraft:windswept_savanna",
                "minecraft:wooded_badlands"
        };
        for (String id : ids) {
            try {
                if (id.contains("nether") || id.contains("end_") || id.equals("minecraft:the_end")) continue;
                out.add(Identifier.parse(id));
            } catch (Throwable ignored) {
            }
        }
        out.sort(Comparator.comparing(Identifier::toString));
        return out;
    }

    private boolean containsBiome(String id) {
        if (id == null || id.isBlank()) return false;
        try {
            Identifier parsed = Identifier.parse(id);
            return cachedBiomes.contains(parsed);
        } catch (Throwable t) {
            return false;
        }
    }

    private static String biomeDisplayName(String id) {
        if (id == null || id.isBlank()) return "minecraft:plains";
        int sep = id.indexOf(':');
        String path = sep >= 0 ? id.substring(sep + 1) : id;
        return path.replace('_', ' ');
    }

    private static final class BiomeSizeSlider extends AbstractWidget {
        private final java.util.function.IntConsumer onChange;
        private int value;
        private boolean dragging;

        private BiomeSizeSlider(int x, int y, int width, int height, int value, java.util.function.IntConsumer onChange) {
            super(x, y, width, height, com.jamie.jamiebingo.util.ComponentUtil.empty());
            this.onChange = onChange;
            refreshFromValue(value);
        }

        private void refreshFromValue(int value) {
            int clamped = clampBiomeSize(value);
            this.value = clamped;
            updateMessage();
        }

        private void setInteractiveState(boolean enabled) {
            this.visible = enabled;
            this.active = enabled;
            if (!enabled) {
                this.dragging = false;
            }
        }

        private int biomeSize() {
            return clampBiomeSize(value);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            int bg = this.active ? 0xFF2A2E35 : 0xFF1A1D22;
            graphics.fill(x, y, x + w, y + h, bg);
            graphics.fill(x, y, x + w, y + 1, 0x66FFFFFF);
            graphics.fill(x, y + h - 1, x + w, y + h, 0x44000000);

            int pad = 6;
            int trackX = x + pad;
            int trackW = Math.max(1, w - pad * 2);
            int trackY = y + h - 7;
            graphics.fill(trackX, trackY, trackX + trackW, trackY + 3, 0xFF101318);

            double t = (biomeSize() - MIN_BIOME_SIZE) / (double) BIOME_SIZE_RANGE;
            int fillW = (int) Math.round(trackW * t);
            graphics.fill(trackX, trackY, trackX + fillW, trackY + 3, 0xFF5AA9E6);

            int knobX = trackX + (int) Math.round(trackW * t);
            graphics.fill(knobX - 2, trackY - 3, knobX + 2, trackY + 6, 0xFFE8EEF6);

            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    x + w / 2,
                    y + 6,
                    0xFFFFFFFF
            );
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean isNew) {
            super.onClick(event, isNew);
            if (event.button() != 0 || !this.active) return;
            dragging = true;
            setFromMouse(event.x());
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
            super.onDrag(event, dragX, dragY);
            if (!dragging || !this.active || !this.visible) return;
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
            int next = clampBiomeSize(MIN_BIOME_SIZE + (int) Math.round(BIOME_SIZE_RANGE * t));
            if (next != value) {
                value = next;
                onChange.accept(next);
            }
            updateMessage();
        }

        private void updateMessage() {
            setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Biome Size: " + biomePercent() + "%"));
        }

        private int biomePercent() {
            double t = (biomeSize() - MIN_BIOME_SIZE) / (double) BIOME_SIZE_RANGE;
            t = Math.max(0.0D, Math.min(1.0D, t));
            return 1 + (int) Math.round(99.0D * t);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    private static final class TerrainHillinessSlider extends AbstractWidget {
        private final java.util.function.IntConsumer onChange;
        private int value;
        private boolean dragging;

        private TerrainHillinessSlider(int x, int y, int width, int height, int value, java.util.function.IntConsumer onChange) {
            super(x, y, width, height, com.jamie.jamiebingo.util.ComponentUtil.empty());
            this.onChange = onChange;
            refreshFromValue(value);
        }

        private void refreshFromValue(int value) {
            this.value = clampTerrainHilliness(value);
            updateMessage();
        }

        private void setInteractiveState(boolean enabled) {
            this.visible = enabled;
            this.active = enabled;
            if (!enabled) {
                this.dragging = false;
            }
        }

        private int hillinessPercent() {
            return clampTerrainHilliness(value);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            int bg = this.active ? 0xFF2A2E35 : 0xFF1A1D22;
            graphics.fill(x, y, x + w, y + h, bg);
            graphics.fill(x, y, x + w, y + 1, 0x66FFFFFF);
            graphics.fill(x, y + h - 1, x + w, y + h, 0x44000000);

            int pad = 6;
            int trackX = x + pad;
            int trackW = Math.max(1, w - pad * 2);
            int trackY = y + h - 7;
            graphics.fill(trackX, trackY, trackX + trackW, trackY + 3, 0xFF101318);

            double t = (hillinessPercent() - MIN_TERRAIN_HILLYNESS) / (double) TERRAIN_HILLYNESS_RANGE;
            int fillW = (int) Math.round(trackW * t);
            graphics.fill(trackX, trackY, trackX + fillW, trackY + 3, 0xFFE6A05A);

            int knobX = trackX + (int) Math.round(trackW * t);
            graphics.fill(knobX - 2, trackY - 3, knobX + 2, trackY + 6, 0xFFE8EEF6);

            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    x + w / 2,
                    y + 6,
                    0xFFFFFFFF
            );
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean isNew) {
            super.onClick(event, isNew);
            if (event.button() != 0 || !this.active) return;
            dragging = true;
            setFromMouse(event.x());
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
            super.onDrag(event, dragX, dragY);
            if (!dragging || !this.active || !this.visible) return;
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
            int next = clampTerrainHilliness(MIN_TERRAIN_HILLYNESS + (int) Math.round(TERRAIN_HILLYNESS_RANGE * t));
            if (next != value) {
                value = next;
                onChange.accept(next);
            }
            updateMessage();
        }

        private void updateMessage() {
            setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Hilliness: " + hillinessPercent() + "%"));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }

    private static final class StructureFrequencySlider extends AbstractWidget {
        private final java.util.function.IntConsumer onChange;
        private int value;
        private boolean dragging;

        private StructureFrequencySlider(int x, int y, int width, int height, int value, java.util.function.IntConsumer onChange) {
            super(x, y, width, height, com.jamie.jamiebingo.util.ComponentUtil.empty());
            this.onChange = onChange;
            refreshFromValue(value);
        }

        private void refreshFromValue(int value) {
            this.value = clampStructureFrequency(value);
            updateMessage();
        }

        private void setInteractiveState(boolean enabled) {
            this.visible = true;
            this.active = enabled;
            if (!enabled) {
                this.dragging = false;
            }
        }

        private int frequencyPercent() {
            return clampStructureFrequency(value);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!this.visible) return;
            int x = getX();
            int y = getY();
            int w = getWidth();
            int h = getHeight();

            int bg = this.active ? 0xFF2A2E35 : 0xFF1A1D22;
            graphics.fill(x, y, x + w, y + h, bg);
            graphics.fill(x, y, x + w, y + 1, 0x66FFFFFF);
            graphics.fill(x, y + h - 1, x + w, y + h, 0x44000000);

            int pad = 6;
            int trackX = x + pad;
            int trackW = Math.max(1, w - pad * 2);
            int trackY = y + h - 7;
            graphics.fill(trackX, trackY, trackX + trackW, trackY + 3, 0xFF101318);

            double t = (frequencyPercent() - MIN_STRUCTURE_FREQUENCY) / (double) STRUCTURE_FREQUENCY_RANGE;
            int fillW = (int) Math.round(trackW * t);
            graphics.fill(trackX, trackY, trackX + fillW, trackY + 3, 0xFF67C587);

            int knobX = trackX + (int) Math.round(trackW * t);
            graphics.fill(knobX - 2, trackY - 3, knobX + 2, trackY + 6, 0xFFE8EEF6);

            graphics.drawCenteredString(
                    Minecraft.getInstance().font,
                    getMessage(),
                    x + w / 2,
                    y + 6,
                    0xFFFFFFFF
            );
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean isNew) {
            super.onClick(event, isNew);
            if (event.button() != 0 || !this.active) return;
            dragging = true;
            setFromMouse(event.x());
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
            super.onDrag(event, dragX, dragY);
            if (!dragging || !this.active || !this.visible) return;
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
            int next = clampStructureFrequency(MIN_STRUCTURE_FREQUENCY + (int) Math.round(STRUCTURE_FREQUENCY_RANGE * t));
            if (next != value) {
                value = next;
                onChange.accept(next);
            }
            updateMessage();
        }

        private void updateMessage() {
            setMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Structure Frequency: " + frequencyPercent() + "%"));
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            defaultButtonNarrationText(narration);
        }
    }
}
