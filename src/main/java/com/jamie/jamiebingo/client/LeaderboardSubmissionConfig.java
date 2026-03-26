package com.jamie.jamiebingo.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LeaderboardSubmissionConfig {
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("jamiebingo_leaderboard.json");
    private static final String DEFAULT_SUBMIT_URL = "https://jamiebingo-api.jamie-lee-thompson.workers.dev/submit";
    private static final String DEFAULT_LEADERBOARD_URL = "https://jamiet9.github.io/jamiebingo-database-and-leaderboard/";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private LeaderboardSubmissionConfig() {
    }

    public static ConfigData load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                ConfigData created = new ConfigData().normalized();
                save(created);
                return created;
            }
            String text = Files.readString(CONFIG_PATH);
            if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
                text = text.substring(1);
            }
            if (text.isBlank()) {
                ConfigData created = new ConfigData().normalized();
                save(created);
                return created;
            }
            ConfigData data = GSON.fromJson(text, ConfigData.class);
            ConfigData normalized = data == null ? new ConfigData().normalized() : data.normalized();
            save(normalized);
            return normalized;
        } catch (Exception ignored) {
            ConfigData fallback = new ConfigData().normalized();
            save(fallback);
            return fallback;
        }
    }

    private static void save(ConfigData data) {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(data == null ? new ConfigData().normalized() : data.normalized()));
        } catch (Exception ignored) {
        }
    }

    public static final class ConfigData {
        public String submitUrl = DEFAULT_SUBMIT_URL;
        public String leaderboardUrl = DEFAULT_LEADERBOARD_URL;
        public String apiKey = "";
        public int timeoutSeconds = 12;

        ConfigData normalized() {
            if (submitUrl == null || submitUrl.isBlank()) submitUrl = DEFAULT_SUBMIT_URL;
            if (leaderboardUrl == null || leaderboardUrl.isBlank()) leaderboardUrl = DEFAULT_LEADERBOARD_URL;
            if (apiKey == null) apiKey = "";
            timeoutSeconds = Math.max(3, Math.min(60, timeoutSeconds));
            return this;
        }

        public boolean hasSubmitUrl() {
            return submitUrl != null && !submitUrl.isBlank();
        }

        public boolean hasLeaderboardUrl() {
            return leaderboardUrl != null && !leaderboardUrl.isBlank();
        }
    }
}