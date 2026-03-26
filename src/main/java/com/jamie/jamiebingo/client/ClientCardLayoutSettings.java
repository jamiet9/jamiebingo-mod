package com.jamie.jamiebingo.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class ClientCardLayoutSettings {
    public static final float MIN_SCALE = 0.1f;

    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("jamiebingo_card_layout.json");

    private static final Gson GSON =
            new GsonBuilder().setPrettyPrinting().create();

    private static boolean loaded = false;

    public static final TargetConfig overlay = new TargetConfig();
    public static final TargetConfig fullscreen = new TargetConfig();
    public static final WidgetConfig scoreboard = new WidgetConfig();
    public static final WidgetConfig settingsOverlay = new WidgetConfig();
    static {
        applyBuiltInDefaults();
    }

    private ClientCardLayoutSettings() {
    }

    public static void load() {
        if (loaded) return;
        loaded = true;

        if (!Files.exists(CONFIG_PATH)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root == null) return;

            if (root.has("overlay") && root.get("overlay").isJsonObject()) {
                hydrateTargetConfig(root.getAsJsonObject("overlay"), overlay);
            }
            if (root.has("fullscreen") && root.get("fullscreen").isJsonObject()) {
                hydrateTargetConfig(root.getAsJsonObject("fullscreen"), fullscreen);
            }
            if (root.has("scoreboard") && root.get("scoreboard").isJsonObject()) {
                WidgetConfig parsed = GSON.fromJson(root.getAsJsonObject("scoreboard"), WidgetConfig.class);
                if (parsed != null) {
                    scoreboard.copyFrom(parsed);
                }
            }
            if (root.has("settingsOverlay") && root.get("settingsOverlay").isJsonObject()) {
                WidgetConfig parsed = GSON.fromJson(root.getAsJsonObject("settingsOverlay"), WidgetConfig.class);
                if (parsed != null) {
                    settingsOverlay.copyFrom(parsed);
                }
            }
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException ignored) {
        }

        SaveData data = new SaveData();
        data.overlay = overlay.snapshot();
        data.fullscreen = fullscreen.snapshot();
        data.scoreboard = scoreboard.snapshot();
        data.settingsOverlay = settingsOverlay.snapshot();

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(data, writer);
        } catch (IOException ignored) {
        }
    }

    public static LayoutResult computeOverlayLayout(
            int screenWidth,
            int screenHeight,
            int cardSize,
            boolean hangman
    ) {
        load();
        return computeOverlayLayout(screenWidth, screenHeight, cardSize, hangman, overlay.resolveLayout(cardSize));
    }

    public static LayoutResult computeFullscreenLayout(
            int screenWidth,
            int screenHeight,
            int cardSize,
            boolean hangman
    ) {
        load();
        return computeFullscreenLayout(screenWidth, screenHeight, cardSize, hangman, fullscreen.resolveLayout(cardSize));
    }

    public static LayoutResult computeOverlayLayout(
            int screenWidth,
            int screenHeight,
            int cardSize,
            boolean hangman,
            LayoutConfig config
    ) {
        float scale = Math.max(MIN_SCALE, config.scale);
        int padding = 10;
        // Overlay keeps a dedicated lane under each slot for rarity labels.
        int spacing = Math.max(12, 14 - cardSize);
        int baseBoxSize = Math.max(
                22,
                Math.min(
                        (screenWidth / 2 - spacing * (cardSize - 1)) / cardSize,
                        (screenHeight / 2 - spacing * (cardSize - 1)) / cardSize
                )
        );

        if (hangman) {
            baseBoxSize = Math.max(16, Math.round(baseBoxSize * 0.55f));
        }

        int boxSize = Math.max(8, Math.round(baseBoxSize * scale));
        int totalWidth = boxSize * cardSize + spacing * (cardSize - 1);
        int totalHeight = boxSize * cardSize + spacing * (cardSize - 1);

        int defaultX = screenWidth - totalWidth - padding;
        int defaultY = padding;

        int startX = config.customPosition
                ? clamp(config.x, 0, Math.max(0, screenWidth - totalWidth))
                : defaultX;
        int startY = config.customPosition
                ? clamp(config.y, 0, Math.max(0, screenHeight - totalHeight))
                : defaultY;

        return new LayoutResult(startX, startY, boxSize, spacing, totalWidth, totalHeight);
    }

    public static LayoutResult computeFullscreenLayout(
            int screenWidth,
            int screenHeight,
            int cardSize,
            boolean hangman,
            LayoutConfig config
    ) {
        float scale = Math.max(MIN_SCALE, config.scale);
        int padding = 40;
        int spacing = Math.max(6, 18 - cardSize);
        int usableWidth = screenWidth - padding * 2;
        int usableHeight = screenHeight - padding * 2;

        int baseBoxSize = Math.min(
                (usableWidth - spacing * (cardSize - 1)) / cardSize,
                (usableHeight - spacing * (cardSize - 1)) / cardSize
        );
        if (baseBoxSize < 28) baseBoxSize = 28;

        if (hangman) {
            baseBoxSize = Math.max(18, Math.round(baseBoxSize * 0.55f));
        }

        int boxSize = Math.max(8, Math.round(baseBoxSize * scale));
        int totalWidth = boxSize * cardSize + spacing * (cardSize - 1);
        int totalHeight = boxSize * cardSize + spacing * (cardSize - 1);

        int defaultX = (screenWidth - totalWidth) / 2;
        int defaultY = (screenHeight - totalHeight) / 2;

        int startX = config.customPosition
                ? clamp(config.x, 0, Math.max(0, screenWidth - totalWidth))
                : defaultX;
        int startY = config.customPosition
                ? clamp(config.y, 0, Math.max(0, screenHeight - totalHeight))
                : defaultY;

        return new LayoutResult(startX, startY, boxSize, spacing, totalWidth, totalHeight);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String sizeKey(int cardSize) {
        return Integer.toString(Math.max(1, cardSize));
    }

    private static void hydrateTargetConfig(JsonObject source, TargetConfig target) {
        if (source == null || target == null) return;

        // Legacy format: overlay/fullscreen directly held a layout config object.
        if (source.has("scale") || source.has("customPosition") || source.has("x") || source.has("y")) {
            LayoutConfig legacy = GSON.fromJson(source, LayoutConfig.class);
            if (legacy != null) {
                target.allSizes.copyFrom(legacy);
            }
            return;
        }

        TargetConfig parsed = GSON.fromJson(source, TargetConfig.class);
        if (parsed != null) {
            target.copyFrom(parsed);
        }
    }

    private static void applyBuiltInDefaults() {
        overlay.allSizes.copyFrom(layout(0.9f, true, 478, 5));
        overlay.perSize.clear();
        overlay.perSize.put("1", layout(0.23f, false, 0, 0));
        overlay.perSize.put("2", layout(0.5f, false, 0, 0));
        overlay.perSize.put("3", layout(0.5f, false, 0, 0));
        overlay.perSize.put("4", layout(1.0f, true, 455, 10));
        overlay.perSize.put("5", layout(1.0f, true, 455, 19));
        overlay.perSize.put("6", layout(1.0f, true, 457, 10));
        overlay.perSize.put("7", layout(0.9f, false, 0, 0));
        overlay.perSize.put("8", layout(0.9f, false, 0, 0));
        overlay.perSize.put("9", layout(0.9f, false, 0, 0));
        overlay.perSize.put("10", layout(0.9f, false, 0, 0));
        overlay.skin = CardSkin.DEFAULT.id();

        fullscreen.allSizes.copyFrom(layout(1.0f, true, 184, 41));
        fullscreen.perSize.clear();
        fullscreen.perSize.put("1", layout(0.22f, false, 0, 0));
        fullscreen.perSize.put("2", layout(0.5f, false, 0, 0));
        fullscreen.perSize.put("3", layout(0.5f, false, 0, 0));
        fullscreen.perSize.put("4", layout(1.0f, false, 0, 0));
        fullscreen.perSize.put("5", layout(1.0f, true, 165, 61));
        fullscreen.perSize.put("6", layout(1.0f, false, 0, 0));
        fullscreen.perSize.put("7", layout(1.0f, false, 0, 0));
        fullscreen.perSize.put("8", layout(1.0f, false, 0, 0));
        fullscreen.perSize.put("9", layout(1.0f, false, 0, 0));
        fullscreen.perSize.put("10", layout(1.0f, false, 0, 0));
        fullscreen.skin = CardSkin.DEFAULT.id();

        scoreboard.scale = 0.7f;
        scoreboard.customPosition = true;
        scoreboard.x = 3;
        scoreboard.y = 207;

        settingsOverlay.scale = 0.4f;
        settingsOverlay.customPosition = true;
        settingsOverlay.x = 0;
        settingsOverlay.y = 61;
    }

    private static LayoutConfig layout(float scale, boolean customPosition, int x, int y) {
        LayoutConfig config = new LayoutConfig();
        config.scale = scale;
        config.customPosition = customPosition;
        config.x = x;
        config.y = y;
        return config;
    }

    public static final class LayoutResult {
        public final int startX;
        public final int startY;
        public final int boxSize;
        public final int spacing;
        public final int totalWidth;
        public final int totalHeight;

        public LayoutResult(int startX, int startY, int boxSize, int spacing, int totalWidth, int totalHeight) {
            this.startX = startX;
            this.startY = startY;
            this.boxSize = boxSize;
            this.spacing = spacing;
            this.totalWidth = totalWidth;
            this.totalHeight = totalHeight;
        }
    }

    public static final class LayoutConfig {
        public float scale = 1.0f;
        public boolean customPosition = false;
        public int x = 0;
        public int y = 0;

        public void reset() {
            scale = 1.0f;
            customPosition = false;
            x = 0;
            y = 0;
        }

        public void copyFrom(LayoutConfig other) {
            if (other == null) return;
            scale = other.scale;
            customPosition = other.customPosition;
            x = other.x;
            y = other.y;
        }

        public static LayoutConfig snapshotOf(LayoutConfig config) {
            LayoutConfig copy = new LayoutConfig();
            copy.copyFrom(config);
            return copy;
        }
    }

    public enum CardSkin {
        DEFAULT("default", "Default"),
        BLOOM("bloom", "Bloom Frame"),
        RUNIC("runic", "Runic Frame"),
        CELESTIAL("celestial", "Celestial Frame"),
        GLASS("glass", "Glass"),
        MINIMAL("minimal", "Minimal"),
        NEON("neon", "Neon Pulse"),
        LAVA("lava", "Lava Core"),
        CANDY("candy", "Candy Pop"),
        TERMINAL("terminal", "Green Terminal"),
        ROYAL("royal", "Royal Gold"),
        VOID("void", "Void Rift"),
        PRISM("prism", "Prism Shift"),
        TOXIC("toxic", "Toxic Slime"),
        ICE("ice", "Frostbite"),
        SUNSET("sunset", "Neon Sunset"),
        GLITCH("glitch", "Glitch Matrix"),
        CHROME("chrome", "Chrome Storm");

        private final String id;
        private final String displayName;

        CardSkin(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public CardSkin next() {
            CardSkin[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public CardSkin previous() {
            CardSkin[] values = values();
            return values[(ordinal() - 1 + values.length) % values.length];
        }

        public static CardSkin fromId(String id) {
            if (id != null) {
                if (id.equalsIgnoreCase("inventory")) {
                    return BLOOM;
                }
                if (id.equalsIgnoreCase("aqua")) {
                    return CELESTIAL;
                }
                for (CardSkin skin : values()) {
                    if (skin.id.equalsIgnoreCase(id)) {
                        return skin;
                    }
                }
            }
            return DEFAULT;
        }
    }

    public enum OverlaySkin {
        DEFAULT("default", "Minimal"),
        PANEL("panel", "Panel"),
        GLASS("glass", "Glass Plate"),
        TERMINAL("terminal", "Terminal");

        private final String id;
        private final String displayName;

        OverlaySkin(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public OverlaySkin next() {
            OverlaySkin[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        public OverlaySkin previous() {
            OverlaySkin[] values = values();
            return values[(ordinal() - 1 + values.length) % values.length];
        }

        public static OverlaySkin fromId(String id) {
            if (id != null) {
                for (OverlaySkin skin : values()) {
                    if (skin.id.equalsIgnoreCase(id)) {
                        return skin;
                    }
                }
            }
            return DEFAULT;
        }
    }

    public static final class TargetConfig {
        public LayoutConfig allSizes = new LayoutConfig();
        public Map<String, LayoutConfig> perSize = new HashMap<>();
        public String skin = CardSkin.DEFAULT.id();

        public LayoutConfig resolveLayout(int cardSize) {
            LayoutConfig specific = perSize.get(sizeKey(cardSize));
            return specific != null ? specific : allSizes;
        }

        public LayoutConfig mutableLayoutForSize(int cardSize) {
            String key = sizeKey(cardSize);
            return perSize.computeIfAbsent(key, ignored -> LayoutConfig.snapshotOf(allSizes));
        }

        public void applyLayoutToAllSizes(LayoutConfig source) {
            if (source == null) return;
            allSizes.copyFrom(source);
            for (LayoutConfig config : perSize.values()) {
                config.copyFrom(source);
            }
        }

        public CardSkin getSkin() {
            return CardSkin.fromId(skin);
        }

        public void setSkin(CardSkin skin) {
            this.skin = (skin == null ? CardSkin.DEFAULT : skin).id();
        }

        public void cycleSkin() {
            setSkin(getSkin().next());
        }

        public TargetConfig snapshot() {
            TargetConfig copy = new TargetConfig();
            copy.copyFrom(this);
            return copy;
        }

        public void copyFrom(TargetConfig other) {
            if (other == null) return;
            allSizes.copyFrom(other.allSizes);
            perSize.clear();
            if (other.perSize != null) {
                for (Map.Entry<String, LayoutConfig> entry : other.perSize.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) continue;
                    perSize.put(entry.getKey(), LayoutConfig.snapshotOf(entry.getValue()));
                }
            }
            skin = CardSkin.fromId(other.skin).id();
        }
    }

    public static final class WidgetConfig {
        public float scale = 1.0f;
        public boolean customPosition = false;
        public int x = 0;
        public int y = 0;
        public String skin = OverlaySkin.DEFAULT.id();

        public void resetScale() {
            scale = 1.0f;
        }

        public void resetPosition() {
            customPosition = false;
            x = 0;
            y = 0;
        }

        public void copyFrom(WidgetConfig other) {
            if (other == null) return;
            scale = other.scale;
            customPosition = other.customPosition;
            x = other.x;
            y = other.y;
            skin = OverlaySkin.fromId(other.skin).id();
        }

        public WidgetConfig snapshot() {
            WidgetConfig copy = new WidgetConfig();
            copy.copyFrom(this);
            return copy;
        }

        public OverlaySkin getSkin() {
            return OverlaySkin.fromId(skin);
        }

        public void setSkin(OverlaySkin skin) {
            this.skin = (skin == null ? OverlaySkin.DEFAULT : skin).id();
        }
    }

    public static LayoutResult resolveWidgetBounds(
            int screenWidth,
            int screenHeight,
            int contentWidth,
            int contentHeight,
            int defaultX,
            int defaultY,
            WidgetConfig config
    ) {
        float scale = Math.max(MIN_SCALE, config.scale);
        int scaledWidth = Math.max(1, Math.round(contentWidth * scale));
        int scaledHeight = Math.max(1, Math.round(contentHeight * scale));
        int startX = config.customPosition
                ? clamp(config.x, 0, Math.max(0, screenWidth - scaledWidth))
                : clamp(defaultX, 0, Math.max(0, screenWidth - scaledWidth));
        int startY = config.customPosition
                ? clamp(config.y, 0, Math.max(0, screenHeight - scaledHeight))
                : clamp(defaultY, 0, Math.max(0, screenHeight - scaledHeight));
        return new LayoutResult(startX, startY, 0, 0, scaledWidth, scaledHeight);
    }

    private static final class SaveData {
        TargetConfig overlay;
        TargetConfig fullscreen;
        WidgetConfig scoreboard;
        WidgetConfig settingsOverlay;
    }
}
