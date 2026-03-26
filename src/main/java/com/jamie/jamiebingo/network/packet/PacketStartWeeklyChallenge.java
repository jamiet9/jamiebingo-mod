package com.jamie.jamiebingo.network.packet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jamie.jamiebingo.client.LeaderboardSubmissionConfig;
import com.jamie.jamiebingo.bingo.WeeklyChallengeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class PacketStartWeeklyChallenge {
    private static final Gson GSON = new Gson();
    private static final String DEFAULT_WEEKLY_URL = "https://jamiebingo-api.jamie-lee-thompson.workers.dev/weekly-challenge";

    private final long baseSeed;

    public PacketStartWeeklyChallenge(long baseSeed) {
        this.baseSeed = baseSeed;
    }

    public static void encode(PacketStartWeeklyChallenge msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.baseSeed);
    }

    public static PacketStartWeeklyChallenge decode(FriendlyByteBuf buf) {
        return new PacketStartWeeklyChallenge(buf.readLong());
    }

    public static void handle(PacketStartWeeklyChallenge msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            if (server == null) return;

            String weeklyUrl = resolveWeeklyUrl();
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(8))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(weeklyUrl))
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build();

            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> server.execute(() -> {
                        if (response.statusCode() < 200 || response.statusCode() >= 300) {
                            player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Unable to load weekly challenge: HTTP " + response.statusCode()));
                            return;
                        }

                        JsonObject root = GSON.fromJson(response.body(), JsonObject.class);
                        String challengeId = readString(root, "challengeId");
                        String cardSeed = readString(root, "cardSeed");
                        String worldSeed = readString(root, "worldSeed");
                        String settingsSeed = readString(root, "settingsSeed");
                        if (cardSeed.isBlank() || worldSeed.isBlank()) {
                            player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Weekly challenge is not published yet."));
                            return;
                        }

                        if (!WeeklyChallengeManager.startPublishedWeeklyChallenge(server, challengeId, settingsSeed, worldSeed, cardSeed)) {
                            player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Unable to start weekly challenge right now."));
                        }
                    }))
                    .exceptionally(error -> {
                        server.execute(() -> {
                            String message = error.getCause() != null && error.getCause().getMessage() != null
                                    ? error.getCause().getMessage()
                                    : (error.getMessage() == null ? "network error" : error.getMessage());
                            player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Unable to load weekly challenge: " + message));
                        });
                        return null;
                    });
        });
        ctx.setPacketHandled(true);
    }

    private static String readString(JsonObject root, String key) {
        return root != null && root.has(key) ? root.get(key).getAsString() : "";
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