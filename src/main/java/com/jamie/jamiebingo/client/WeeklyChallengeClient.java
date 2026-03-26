package com.jamie.jamiebingo.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSeedHelper;
import com.jamie.jamiebingo.bingo.CardSeedCodec;
import com.jamie.jamiebingo.bingo.SettingsSeedCodec;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class WeeklyChallengeClient {
    private static final Gson GSON = new Gson();
    private static final String DEFAULT_WEEKLY_URL = "https://jamiebingo-api.jamie-lee-thompson.workers.dev/weekly-challenge";

    private WeeklyChallengeClient() {
    }

    public static CompletableFuture<Void> refresh() {
        WeeklyChallengeClientState.beginLoading();
        String url = resolveWeeklyUrl();
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() < 200 || response.statusCode() >= 300) {
                        WeeklyChallengeClientState.setFetchResult(0L, 0L, "", "HTTP " + response.statusCode());
                        return;
                    }
                    JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
                    long baseSeed = readLong(root, "baseSeed");
                    long nextReset = readLong(root, "nextResetEpochSeconds");
                    String challengeId = readString(root, "challengeId");
                    String cardSeed = readString(root, "cardSeed");
                    String worldSeed = readString(root, "worldSeed");
                    String settingsSeed = readString(root, "settingsSeed");

                    if (baseSeed <= 0L || cardSeed.isBlank()) {
                        WeeklyChallengeClientState.setFetchResult(0L, nextReset, challengeId, "Weekly challenge unavailable");
                        return;
                    }

                    CardSeedCodec.SeedData seedData = CardSeedCodec.decode(cardSeed);
                    BingoCard card = seedData != null && seedData.cards() != null && !seedData.cards().isEmpty()
                            ? seedData.cards().getFirst()
                            : null;
                    if (card == null) {
                        WeeklyChallengeClientState.setFetchResult(baseSeed, nextReset, challengeId, "Weekly challenge preview is invalid");
                        return;
                    }

                    List<String> settingsLines = mergeSettings(
                            readStringArray(root, "settingsLines"),
                            BingoSeedHelper.buildSettingsLinesFromSeed(null, seedData, worldSeed)
                    );
                    String resolvedSettingsSeed = settingsSeed == null || settingsSeed.isBlank()
                            ? SettingsSeedCodec.fromCardSeed(cardSeed)
                            : settingsSeed;

                    WeeklyChallengeClientState.setFetchResult(baseSeed, nextReset, challengeId, "");
                    WeeklyChallengeClientState.setPreview(
                            card,
                            settingsLines,
                            cardSeed,
                            worldSeed,
                            resolvedSettingsSeed,
                            challengeId,
                            nextReset,
                            ""
                    );
                })
                .exceptionally(error -> {
                    String message = error.getCause() != null && error.getCause().getMessage() != null
                            ? error.getCause().getMessage()
                            : (error.getMessage() == null ? "network error" : error.getMessage());
                    WeeklyChallengeClientState.setFetchResult(0L, 0L, "", message);
                    return null;
                });
    }

    private static long readLong(JsonObject root, String key) {
        return root != null && root.has(key) ? root.get(key).getAsLong() : 0L;
    }

    private static String readString(JsonObject root, String key) {
        return root != null && root.has(key) ? root.get(key).getAsString() : "";
    }

    private static List<String> readStringArray(JsonObject root, String key) {
        List<String> out = new ArrayList<>();
        if (root == null || !root.has(key) || !root.get(key).isJsonArray()) return out;
        JsonArray array = root.getAsJsonArray(key);
        for (JsonElement element : array) {
            if (element == null || element.isJsonNull()) continue;
            out.add(element.getAsString());
        }
        return out;
    }

    private static List<String> mergeSettings(List<String> preferred, List<String> fallback) {
        List<String> merged = new ArrayList<>();
        for (String line : preferred == null ? List.<String>of() : preferred) {
            if (line == null || line.isBlank() || merged.contains(line)) continue;
            merged.add(line);
        }
        for (String line : fallback == null ? List.<String>of() : fallback) {
            if (line == null || line.isBlank()) continue;
            String key = settingKey(line);
            boolean exists = merged.stream().anyMatch(existing -> settingKey(existing).equals(key));
            if (!exists) {
                merged.add(line);
            }
        }
        return merged;
    }

    private static String settingKey(String line) {
        if (line == null) return "";
        int idx = line.indexOf(':');
        return idx < 0 ? line.trim() : line.substring(0, idx + 1).trim();
    }

    private static String resolveWeeklyUrl() {
        LeaderboardSubmissionConfig.ConfigData config = LeaderboardSubmissionConfig.load();
        if (config.submitUrl != null && !config.submitUrl.isBlank()) {
            return config.submitUrl.trim().replace("/submit", "/weekly-challenge");
        }
        if (config.leaderboardUrl != null && !config.leaderboardUrl.isBlank()) {
            try {
                URI leaderboard = URI.create(config.leaderboardUrl.trim());
                String query = leaderboard.getQuery();
                if (query != null) {
                    for (String part : query.split("&")) {
                        if (!part.startsWith("source=")) continue;
                        String source = java.net.URLDecoder.decode(part.substring("source=".length()), java.nio.charset.StandardCharsets.UTF_8);
                        if (source.endsWith("/submissions")) {
                            return source.substring(0, source.length() - "/submissions".length()) + "/weekly-challenge";
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return DEFAULT_WEEKLY_URL;
    }
}