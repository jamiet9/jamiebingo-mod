package com.jamie.jamiebingo.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.network.packet.PacketOpenGameHistory;
import com.jamie.jamiebingo.quest.icon.QuestIconData;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class LeaderboardSubmissionClient {
    private static final Gson GSON = new GsonBuilder().create();

    private LeaderboardSubmissionClient() {
    }

    public static CompletableFuture<SubmissionResult> submit(PacketOpenGameHistory.EntryData entry) {
        LeaderboardSubmissionConfig.ConfigData config = LeaderboardSubmissionConfig.load();
        if (entry == null) {
            return CompletableFuture.completedFuture(SubmissionResult.failure("Invalid: no run selected"));
        }
        if (!config.hasSubmitUrl()) {
            return CompletableFuture.completedFuture(SubmissionResult.failure("Invalid: leaderboard submit is not configured"));
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds))
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(config.submitUrl.trim()))
                .timeout(Duration.ofSeconds(config.timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(SubmissionPayload.from(entry))));

        if (!config.apiKey.isBlank()) {
            requestBuilder.header("X-API-Key", config.apiKey);
        }

        return client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        return SubmissionResult.success("Valid: submitted successfully");
                    }
                    return SubmissionResult.failure("Valid but submit failed: HTTP " + response.statusCode()
                            + suffixResponse(response.body()));
                })
                .exceptionally(error -> SubmissionResult.failure("Valid but submit failed: " + rootMessage(error)));
    }

    private static String suffixResponse(String body) {
        if (body == null) return "";
        String trimmed = body.trim().replace('\n', ' ');
        if (trimmed.isBlank()) return "";
        if (trimmed.length() > 80) {
            trimmed = trimmed.substring(0, 77) + "...";
        }
        return " (" + trimmed + ")";
    }

    private static String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        if (message == null || message.isBlank()) {
            return current instanceof IOException ? "network error" : current.getClass().getSimpleName();
        }
        return message;
    }

    public record SubmissionResult(boolean success, String message) {
        public static SubmissionResult success(String message) {
            return new SubmissionResult(true, message == null ? "Success" : message);
        }

        public static SubmissionResult failure(String message) {
            return new SubmissionResult(false, message == null ? "Failed" : message);
        }
    }

    private static final class SubmissionPayload {
        private final String playerName;
        private final String cardSeed;
        private final String worldSeed;
        private final String settingsSeed;
        private final long durationSeconds;
        private final long finishedAtEpochSeconds;
        private final boolean completed;
        private final int participantCount;
        private final boolean commandsUsed;
        private final boolean voteRerollUsed;
        private final int rerollsUsedCount;
        private final int fakeRerollsUsedCount;
        private final int previewSize;
        private final int teamColorId;
        private final List<WebsitePreviewSlot> previewSlots;
        private final List<String> previewSlotIds;
        private final List<String> completedSlotIds;
        private final List<String> opponentCompletedSlotIds;
        private final List<String> settingsLines;
        private final long submittedAtEpochSeconds;
        private final boolean weeklyChallenge;
        private final String weeklyChallengeId;
        private final String leaderboardCategory;
        private final String leaderboardCategoryReason;

        private SubmissionPayload(
                String playerName,
                String cardSeed,
                String worldSeed,
                String settingsSeed,
                long durationSeconds,
                long finishedAtEpochSeconds,
                boolean completed,
                int participantCount,
                boolean commandsUsed,
                boolean voteRerollUsed,
                int rerollsUsedCount,
                int fakeRerollsUsedCount,
                int previewSize,
                int teamColorId,
                List<WebsitePreviewSlot> previewSlots,
                List<String> previewSlotIds,
                List<String> completedSlotIds,
                List<String> opponentCompletedSlotIds,
                List<String> settingsLines,
                long submittedAtEpochSeconds,
                boolean weeklyChallenge,
                String weeklyChallengeId,
                String leaderboardCategory,
                String leaderboardCategoryReason
        ) {
            this.playerName = playerName;
            this.cardSeed = cardSeed;
            this.worldSeed = worldSeed;
            this.settingsSeed = settingsSeed;
            this.durationSeconds = durationSeconds;
            this.finishedAtEpochSeconds = finishedAtEpochSeconds;
            this.completed = completed;
            this.participantCount = participantCount;
            this.commandsUsed = commandsUsed;
            this.voteRerollUsed = voteRerollUsed;
            this.rerollsUsedCount = rerollsUsedCount;
            this.fakeRerollsUsedCount = fakeRerollsUsedCount;
            this.previewSize = previewSize;
            this.teamColorId = teamColorId;
            this.previewSlots = previewSlots;
            this.previewSlotIds = previewSlotIds;
            this.completedSlotIds = completedSlotIds;
            this.opponentCompletedSlotIds = opponentCompletedSlotIds;
            this.settingsLines = settingsLines;
            this.submittedAtEpochSeconds = submittedAtEpochSeconds;
            this.weeklyChallenge = weeklyChallenge;
            this.weeklyChallengeId = weeklyChallengeId;
            this.leaderboardCategory = leaderboardCategory;
            this.leaderboardCategoryReason = leaderboardCategoryReason;
        }

        private static SubmissionPayload from(PacketOpenGameHistory.EntryData entry) {
            Minecraft minecraft = Minecraft.getInstance();
            String playerName = minecraft == null || minecraft.getUser() == null ? "" : minecraft.getUser().getName();
            List<String> settingsLines = new ArrayList<>(entry.settingsLines);
            return new SubmissionPayload(
                    playerName == null ? "" : playerName,
                    entry.cardSeed,
                    entry.worldSeed,
                    entry.settingsSeed,
                    entry.durationSeconds,
                    entry.finishedAtEpochSeconds,
                    entry.completed,
                    entry.participantCount,
                    entry.commandsUsed,
                    entry.voteRerollUsed,
                    entry.rerollsUsedCount,
                    entry.fakeRerollsUsedCount,
                    entry.previewSize,
                    entry.teamColorId,
                    buildPreviewSlots(entry),
                    List.copyOf(entry.previewSlotIds),
                    List.copyOf(entry.completedSlotIds),
                    List.copyOf(entry.opponentCompletedSlotIds),
                    List.copyOf(settingsLines),
                    System.currentTimeMillis() / 1000L,
                    entry.weeklyChallenge,
                    entry.weeklyChallengeId,
                    entry.leaderboardCategory,
                    entry.leaderboardCategoryReason
            );
        }

        private static List<WebsitePreviewSlot> buildPreviewSlots(PacketOpenGameHistory.EntryData entry) {
            List<WebsitePreviewSlot> out = new ArrayList<>();
            if (entry == null || entry.previewSlots == null) return out;
            for (PacketOpenGameHistory.PreviewSlotData slot : entry.previewSlots) {
                if (slot == null) {
                    out.add(WebsitePreviewSlot.empty());
                    continue;
                }
                out.add(WebsitePreviewSlot.from(slot));
            }
            return out;
        }
    }

    private record WebsitePreviewSlot(
            String id,
            String name,
            String category,
            String rarity,
            QuestIconMeta questIcon
    ) {
        private static WebsitePreviewSlot empty() {
            return new WebsitePreviewSlot("", "", "", "", null);
        }

        private static WebsitePreviewSlot from(PacketOpenGameHistory.PreviewSlotData slot) {
            QuestIconMeta iconMeta = null;
            if (slot.id() != null && slot.id().startsWith("quest.")) {
                QuestIconData icon = QuestIconProvider.iconFor(new BingoSlot(
                        slot.id(),
                        slot.name(),
                        slot.category(),
                        slot.rarity()
                ));
                iconMeta = QuestIconMeta.from(icon);
            }
            return new WebsitePreviewSlot(
                    slot.id(),
                    slot.name(),
                    slot.category(),
                    slot.rarity(),
                    iconMeta
            );
        }
    }

    private record QuestIconMeta(
            String mainItemId,
            String mainTexture,
            RegionMeta mainRegion,
            String mainEntityId,
            boolean mainEntityBaby,
            String cornerItemId,
            String cornerTexture,
            RegionMeta cornerRegion,
            String cornerEntityId,
            boolean cornerEntityBaby,
            String numberText
    ) {
        private static QuestIconMeta from(QuestIconData icon) {
            if (icon == null) return null;
            return new QuestIconMeta(
                    itemId(icon.mainIcon),
                    textureId(icon.mainTexture),
                    RegionMeta.from(icon.mainRegion),
                    entityId(icon.mainEntityType),
                    icon.mainEntityColor != null && icon.mainEntityColor == -1,
                    itemId(icon.cornerIcon),
                    textureId(icon.cornerTexture),
                    RegionMeta.from(icon.cornerRegion),
                    entityId(icon.cornerEntityType),
                    icon.cornerEntityColor != null && icon.cornerEntityColor == -1,
                    icon.numberText == null ? "" : icon.numberText
            );
        }

        private static String itemId(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return "";
            Identifier id = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return id == null ? "" : id.toString();
        }

        private static String textureId(Identifier texture) {
            return texture == null ? "" : texture.toString();
        }

        private static String entityId(net.minecraft.world.entity.EntityType<?> entityType) {
            Identifier id = entityType == null ? null : ForgeRegistries.ENTITY_TYPES.getKey(entityType);
            return id == null ? "" : id.toString();
        }
    }

    private record RegionMeta(
            String texture,
            int u,
            int v,
            int width,
            int height,
            int textureWidth,
            int textureHeight
    ) {
        private static RegionMeta from(QuestIconData.TextureRegion region) {
            if (region == null) return null;
            return new RegionMeta(
                    region.texture == null ? "" : region.texture.toString(),
                    region.u,
                    region.v,
                    region.width,
                    region.height,
                    region.textureWidth,
                    region.textureHeight
            );
        }
    }
}
