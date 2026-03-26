package com.jamie.jamiebingo.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ClientBlacklistSettings {

    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("jamiebingo_blacklist_presets.json");
    private static final Path LEGACY_CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("jamiebingo_blacklist.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SEED_PREFIX = "JB-LIST-1:";
    private static boolean loaded = false;

    private static final List<Preset> presets = new ArrayList<>();

    private ClientBlacklistSettings() {
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

        // Legacy fallback: import old single blacklist into one enabled preset.
        Set<String> legacyIds = new LinkedHashSet<>();
        if (Files.exists(LEGACY_CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(LEGACY_CONFIG_PATH)) {
                LegacySaveData legacy = GSON.fromJson(reader, LegacySaveData.class);
                if (legacy != null && legacy.ids != null) {
                    for (String id : legacy.ids) {
                        if (id != null && !id.isBlank()) {
                            legacyIds.add(id);
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }

        presets.clear();
        presets.add(new Preset("Default", true, Preset.MODE_BLACKLIST, legacyIds));
        save();
    }

    public static void save() {
        load();
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException ignored) {
        }

        SaveData data = new SaveData();
        data.presets = new ArrayList<>();
        for (Preset preset : presets) {
            if (preset == null) continue;
            SavePreset out = new SavePreset();
            out.name = preset.name == null || preset.name.isBlank() ? "Preset" : preset.name;
            out.enabled = preset.enabled;
            out.mode = preset.mode;
            out.ids = new ArrayList<>(preset.ids);
            data.presets.add(out);
        }
        if (data.presets.isEmpty()) {
            SavePreset fallback = new SavePreset();
            fallback.name = "Default";
            fallback.enabled = true;
            fallback.mode = Preset.MODE_BLACKLIST;
            fallback.ids = new ArrayList<>();
            data.presets.add(fallback);
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
            out.add(new Preset(preset.name, preset.enabled, preset.mode, preset.ids));
        }
        return out;
    }

    public static void setPresets(List<Preset> updated) {
        load();
        presets.clear();
        if (updated != null) {
            for (Preset preset : updated) {
                if (preset == null) continue;
                String name = preset.name == null || preset.name.isBlank() ? "Preset" : preset.name.trim();
                Set<String> ids = new LinkedHashSet<>();
                if (preset.ids != null) {
                    for (String id : preset.ids) {
                        if (id != null && !id.isBlank()) ids.add(id);
                    }
                }
                presets.add(new Preset(name, preset.enabled, preset.mode, ids));
            }
        }
        if (presets.isEmpty()) {
            presets.add(new Preset("Default", true, Preset.MODE_BLACKLIST, Set.of()));
        }
    }

    public static Set<String> getActiveBlacklistUnion() {
        load();
        Set<String> out = new LinkedHashSet<>();
        for (Preset preset : presets) {
            if (preset == null || !preset.enabled || preset.ids == null || !Preset.MODE_BLACKLIST.equals(preset.mode)) continue;
            out.addAll(preset.ids);
        }
        return out;
    }

    public static Set<String> getActiveWhitelistUnion() {
        load();
        Set<String> out = new LinkedHashSet<>();
        for (Preset preset : presets) {
            if (preset == null || !preset.enabled || preset.ids == null || !Preset.MODE_WHITELIST.equals(preset.mode)) continue;
            out.addAll(preset.ids);
        }
        return out;
    }

    public static String exportSeed() {
        load();
        SaveData data = new SaveData();
        for (Preset preset : presets) {
            if (preset == null) continue;
            SavePreset out = new SavePreset();
            out.name = preset.name == null || preset.name.isBlank() ? "Preset" : preset.name;
            out.enabled = preset.enabled;
            out.mode = preset.mode;
            out.ids = new ArrayList<>(preset.ids);
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
            for (SavePreset in : data.presets) {
                if (in == null) continue;
                Set<String> ids = new LinkedHashSet<>();
                if (in.ids != null) {
                    for (String id : in.ids) {
                        if (id != null && !id.isBlank()) ids.add(id);
                    }
                }
                imported.add(new Preset(in.name, in.enabled, in.mode, ids));
            }
            if (imported.isEmpty()) return false;
            for (Preset preset : imported) {
                presets.add(new Preset(uniquePresetName(preset.name), preset.enabled, preset.mode, preset.ids));
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
            for (SavePreset in : data.presets) {
                if (in == null) continue;
                String name = in.name == null || in.name.isBlank() ? "Preset" : in.name.trim();
                Set<String> ids = new LinkedHashSet<>();
                if (in.ids != null) {
                    for (String id : in.ids) {
                        if (id != null && !id.isBlank()) ids.add(id);
                    }
                }
                presets.add(new Preset(name, in.enabled, in.mode, ids));
            }
        }
        if (presets.isEmpty()) {
            presets.add(new Preset("Default", true, Preset.MODE_BLACKLIST, Set.of()));
        }
    }

    public static final class Preset {
        public static final String MODE_BLACKLIST = "blacklist";
        public static final String MODE_WHITELIST = "whitelist";

        public final String name;
        public final boolean enabled;
        public final String mode;
        public final Set<String> ids;

        public Preset(String name, boolean enabled, Set<String> ids) {
            this(name, enabled, MODE_BLACKLIST, ids);
        }

        public Preset(String name, boolean enabled, String mode, Set<String> ids) {
            this.name = name == null ? "" : name;
            this.enabled = enabled;
            this.mode = MODE_WHITELIST.equalsIgnoreCase(mode) ? MODE_WHITELIST : MODE_BLACKLIST;
            this.ids = new LinkedHashSet<>();
            if (ids != null) {
                for (String id : ids) {
                    if (id != null && !id.isBlank()) this.ids.add(id);
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
        String mode = Preset.MODE_BLACKLIST;
        List<String> ids = new ArrayList<>();
    }

    private static final class LegacySaveData {
        List<String> ids = new ArrayList<>();
    }
}
