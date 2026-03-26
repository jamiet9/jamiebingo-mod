package com.jamie.jamiebingo.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class ClientTeamPreferenceSettings {
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("jamiebingo_team_preferences.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static boolean loaded = false;
    private static SaveData data = new SaveData();

    private ClientTeamPreferenceSettings() {
    }

    public static void load() {
        if (loaded) return;
        loaded = true;

        if (!Files.exists(CONFIG_PATH)) {
            data = new SaveData();
            return;
        }

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            SaveData loadedData = GSON.fromJson(reader, SaveData.class);
            data = loadedData == null ? new SaveData() : loadedData;
            if (data.accountToColorId == null) data.accountToColorId = new HashMap<>();
        } catch (IOException ignored) {
            data = new SaveData();
        }
    }

    public static int getPreferredTeamColorId() {
        load();
        return data.accountToColorId.getOrDefault(resolveAccountKey(), -1);
    }

    public static void setPreferredTeamColorId(int colorId) {
        load();
        if (colorId < 0) return;
        data.accountToColorId.put(resolveAccountKey(), colorId);
        save();
    }

    private static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
        } catch (IOException ignored) {
        }

        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(data, writer);
        } catch (IOException ignored) {
        }
    }

    private static String resolveAccountKey() {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc != null && mc.getUser() != null && mc.getUser().getName() != null && !mc.getUser().getName().isBlank()) {
            return "name:" + mc.getUser().getName().trim().toLowerCase(Locale.ROOT);
        }
        if (mc != null && mc.player != null) {
            return "uuid:" + com.jamie.jamiebingo.util.EntityUtil.getUUID(mc.player);
        }
        return "default";
    }

    private static final class SaveData {
        Map<String, Integer> accountToColorId = new HashMap<>();
    }
}
