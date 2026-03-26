package com.jamie.jamiebingo.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jamie.jamiebingo.bingo.BingoCard;
import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.quest.icon.QuestIconData;
import com.jamie.jamiebingo.quest.icon.QuestIconProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class WeeklyChallengePublisher {
    private static final Gson GSON = new GsonBuilder().create();
    private static final org.apache.logging.log4j.Logger LOGGER =
            org.apache.logging.log4j.LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);
    private static String lastPublishedChallengeId = "";
    private static String lastPublishedCardSeed = "";

    private WeeklyChallengePublisher() {
    }

    public static void publish(BingoCard card,
                               List<String> settingsLines,
                               String cardSeed,
                               String worldSeed,
                               String settingsSeed,
                               String challengeId,
                               long baseSeed,
                               long nextResetEpochSeconds) {
        if (card == null || challengeId == null || challengeId.isBlank()) {
            return;
        }
        if (challengeId.equals(lastPublishedChallengeId) && String.valueOf(cardSeed).equals(lastPublishedCardSeed)) {
            return;
        }

        LeaderboardSubmissionConfig.ConfigData config = LeaderboardSubmissionConfig.load();
        if (!config.hasSubmitUrl()) {
            return;
        }

        String publishUrl = config.submitUrl.trim().replace("/submit", "/weekly-challenge-publish");
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(config.timeoutSeconds))
                .build();

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(publishUrl))
                .timeout(Duration.ofSeconds(config.timeoutSeconds))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(
                        new WeeklyPayload(
                                baseSeed,
                                challengeId,
                                nextResetEpochSeconds,
                                settingsSeed == null ? "" : settingsSeed,
                                worldSeed == null ? "" : worldSeed,
                                cardSeed == null ? "" : cardSeed,
                                card.getSize(),
                                buildPreviewSlots(card),
                                settingsLines == null ? List.of() : List.copyOf(settingsLines)
                        )
                )));

        if (!config.apiKey.isBlank()) {
            requestBuilder.header("X-API-Key", config.apiKey);
        }

        client.sendAsync(requestBuilder.build(), HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        lastPublishedChallengeId = challengeId;
                        lastPublishedCardSeed = cardSeed == null ? "" : cardSeed;
                    } else {
                        LOGGER.warn("[JamieBingo] Weekly challenge publish failed: HTTP {}", response.statusCode());
                    }
                })
                .exceptionally(error -> {
                    LOGGER.warn("[JamieBingo] Weekly challenge publish request failed", error);
                    return null;
                });
    }

    private static List<WebsitePreviewSlot> buildPreviewSlots(BingoCard card) {
        List<WebsitePreviewSlot> out = new ArrayList<>();
        if (card == null) return out;
        int size = Math.max(0, card.getSize());
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                BingoSlot slot = card.getSlot(x, y);
                if (slot == null) {
                    out.add(WebsitePreviewSlot.empty());
                    continue;
                }
                QuestIconMeta iconMeta = null;
                if (slot.getId() != null && slot.getId().startsWith("quest.")) {
                    QuestIconData icon = QuestIconProvider.iconFor(slot);
                    iconMeta = QuestIconMeta.from(icon);
                }
                out.add(new WebsitePreviewSlot(
                        safe(slot.getId()),
                        safe(slot.getName()),
                        safe(slot.getCategory()),
                        safe(slot.getRarity()),
                        iconMeta
                ));
            }
        }
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record WeeklyPayload(
            long baseSeed,
            String challengeId,
            long nextResetEpochSeconds,
            String settingsSeed,
            String worldSeed,
            String cardSeed,
            int previewSize,
            List<WebsitePreviewSlot> previewSlots,
            List<String> settingsLines
    ) {
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
