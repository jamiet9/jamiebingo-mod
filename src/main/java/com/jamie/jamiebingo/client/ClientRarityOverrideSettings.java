package com.jamie.jamiebingo.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jamie.jamiebingo.bingo.BingoRarityUtil;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientRarityOverrideSettings {
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("jamiebingo_rarity_override_presets.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SEED_PREFIX = "JB-RARITY-1:";
    private static boolean loaded = false;

    private static final List<Preset> presets = new ArrayList<>();

    private ClientRarityOverrideSettings() {
    }

    public static void load() {
        if (loaded) return;
        loaded = true;

        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                SaveData data = GSON.fromJson(reader, SaveData.class);
                applySaveData(data);
                return;
            } catch (IOException ignored) {
            }
        }

        presets.clear();
        presets.add(new Preset("Default", true, Map.of()));
        save();
    }

    public static void save() {
        load();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException ignored) {
        }

        SaveData data = new SaveData();
        for (Preset preset : presets) {
            if (preset == null) continue;
            SavePreset out = new SavePreset();
            out.name = preset.name;
            out.enabled = preset.enabled;
            out.overrides = new LinkedHashMap<>(preset.overrides);
            data.presets.add(out);
        }

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(data, writer);
        } catch (IOException ignored) {
        }
    }

    public static List<Preset> getPresets() {
        load();
        List<Preset> out = new ArrayList<>();
        for (Preset preset : presets) {
            out.add(new Preset(preset.name, preset.enabled, preset.overrides));
        }
        return out;
    }

    public static void setPresets(List<Preset> updated) {
        load();
        presets.clear();
        if (updated != null) {
            for (Preset preset : updated) {
                if (preset != null) {
                    presets.add(new Preset(preset.name, preset.enabled, preset.overrides));
                }
            }
        }
        if (presets.isEmpty()) {
            presets.add(new Preset("Default", true, Map.of()));
        }
    }

    public static Map<String, String> getActiveOverrides() {
        load();
        Map<String, String> out = new LinkedHashMap<>();
        for (Preset preset : presets) {
            if (preset == null || !preset.enabled) continue;
            for (Map.Entry<String, String> entry : preset.overrides.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) continue;
                String rarity = BingoRarityUtil.normalize(entry.getValue());
                if (!BingoRarityUtil.isKnown(rarity)) continue;
                out.put(entry.getKey(), rarity);
            }
        }
        return out;
    }

    public static String exportSeed() {
        load();
        SaveData data = new SaveData();
        for (Preset preset : presets) {
            if (preset == null) continue;
            SavePreset out = new SavePreset();
            out.name = preset.name;
            out.enabled = preset.enabled;
            out.overrides = new LinkedHashMap<>(preset.overrides);
            data.presets.add(out);
        }
        String json = GSON.toJson(data);
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return SEED_PREFIX + payload;
    }

    public static boolean importSeed(String seed) {
        load();
        String normalized = seed == null ? "" : seed.trim();
        if (normalized.startsWith("\"") && normalized.endsWith("\"") && normalized.length() > 1) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (!normalized.startsWith(SEED_PREFIX)) return false;
        String payload = normalized.substring(SEED_PREFIX.length());
        if (payload.isBlank()) return false;

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            String json = new String(decoded, StandardCharsets.UTF_8);
            SaveData data = GSON.fromJson(json, SaveData.class);
            if (data == null || data.presets == null || data.presets.isEmpty()) return false;
            List<Preset> imported = new ArrayList<>();
            for (SavePreset preset : data.presets) {
                if (preset == null) continue;
                imported.add(new Preset(preset.name, preset.enabled, preset.overrides));
            }
            if (imported.isEmpty()) return false;
            for (Preset preset : imported) {
                presets.add(new Preset(uniquePresetName(preset.name), preset.enabled, preset.overrides));
            }
            save();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static String uniquePresetName(String baseName) {
        String base = (baseName == null || baseName.isBlank()) ? "Preset" : baseName.trim();
        String candidate = base;
        int suffix = 2;
        while (hasPresetName(candidate)) {
            candidate = base + " (" + suffix + ")";
            suffix++;
        }
        return candidate;
    }

    private static boolean hasPresetName(String name) {
        for (Preset preset : presets) {
            if (preset == null || preset.name == null) continue;
            if (preset.name.equalsIgnoreCase(name)) return true;
        }
        return false;
    }

    private static void applySaveData(SaveData data) {
        presets.clear();
        if (data != null && data.presets != null) {
            for (SavePreset preset : data.presets) {
                if (preset != null) {
                    presets.add(new Preset(preset.name, preset.enabled, preset.overrides));
                }
            }
        }
        if (presets.isEmpty()) {
            presets.add(new Preset("Default", true, Map.of()));
        }
    }

    public static final class Preset {
        public final String name;
        public final boolean enabled;
        public final Map<String, String> overrides;

        public Preset(String name, boolean enabled, Map<String, String> overrides) {
            this.name = name == null ? "" : name;
            this.enabled = enabled;
            this.overrides = new LinkedHashMap<>();
            if (overrides != null) {
                for (Map.Entry<String, String> entry : overrides.entrySet()) {
                    if (entry.getKey() == null || entry.getKey().isBlank()) continue;
                    String rarity = BingoRarityUtil.normalize(entry.getValue());
                    if (!BingoRarityUtil.isKnown(rarity)) continue;
                    this.overrides.put(entry.getKey(), rarity);
                }
            }
        }
    }

    private static final class SaveData {
        List<SavePreset> presets = new ArrayList<>();
    }

    private static final class SavePreset {
        String name = "Preset";
        boolean enabled = true;
        Map<String, String> overrides = new LinkedHashMap<>();
    }
}
