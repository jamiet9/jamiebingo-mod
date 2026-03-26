package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.ItemDefinition;
import com.jamie.jamiebingo.data.ItemDatabase;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;

import java.util.*;

public class HybridCategoryCardGenerator {

    private static final Random RANDOM = new Random();
    private static final String QUEST_CATEGORY = "QUEST";

    /* =========================================================
       PUBLIC API — FULL FILE
       ========================================================= */

    public static BingoCard generate(
            int size,
            String difficulty,
            net.minecraft.server.MinecraftServer server,
            com.jamie.jamiebingo.data.BingoGameData data
    ) {

        ItemDatabase.load();
        QuestDatabase.load();

        BingoCard card = new BingoCard(size);
        Set<String> used = new HashSet<>();
        Map<String, Integer> categoryCounts = new HashMap<>();

        List<QuestDefinition> allQuests =
                new ArrayList<>(QuestDatabase.getEligibleQuests(server, data));
        Set<String> blacklistedIds = data == null ? Collections.emptySet() : data.getBlacklistedSlotIds();

        boolean blindMode = data != null && data.winCondition == WinCondition.BLIND;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {

                ResolvedSlot resolved;

                // ===============================
                // TOP-LEFT: ALWAYS COMMON
                // ===============================
                if (blindMode && x == 0 && y == 0) {
                    resolved = rollForcedCommon(allQuests, used, categoryCounts, blacklistedIds);
                }

                // ===============================
                // BOTTOM-RIGHT: NEVER EPIC+
                // ===============================
                else if (blindMode && x == size - 1 && y == size - 1) {
                    resolved = rollNoHighRarity(
                            allQuests,
                            used,
                            categoryCounts,
                            difficulty,
                            blacklistedIds
                    );
                }

                // ===============================
                // NORMAL SLOT
                // ===============================
                else {
                    resolved = rollResolvedSlot(
                            allQuests,
                            used,
                            categoryCounts,
                            difficulty,
                            blacklistedIds
                    );
                }

                if (resolved == null) {
                    resolved = new ResolvedSlot(
                            "minecraft:dirt",
                            "Dirt",
                            "Misc",
                            "Common",
                            false
                    );
                }

                used.add(resolved.id);
                if (resolved.isQuest) {
                    incrementCategory(categoryCounts, QUEST_CATEGORY);
                }
                incrementCategory(categoryCounts, resolved.category);

                card.setSlot(
                        x,
                        y,
                        new BingoSlot(
                                resolved.id,
                                resolved.name,
                                resolved.category,
                                resolved.rarity
                        )
                );
            }
        }

        System.out.println("[JamieBingo] Generated " + size + "x" + size +
                " HYBRID (category) card with forced start/end rules");

        return card;
    }

    /* =========================================================
       NORMAL ROLL
       ========================================================= */

    public static ResolvedSlot rollResolvedSlot(
            List<QuestDefinition> allQuests,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty
    ) {
        return rollResolvedSlot(allQuests, used, categoryCounts, difficulty, Collections.emptySet());
    }

    public static ResolvedSlot rollResolvedSlot(
            List<QuestDefinition> allQuests,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty,
            Set<String> blacklistedIds
    ) {
        for (int attempts = 0; attempts < 40; attempts++) {

            String rarity = DifficultyManager.rollRarity(difficulty);
            List<String> categories = getCategoriesForRarity(allQuests, rarity);
            if (categories.isEmpty()) continue;
            String category = pickWeightedCategory(categories, categoryCounts);

            if (QUEST_CATEGORY.equals(category)) {

                ResolvedSlot quest =
                        rollResolvedQuestWithRarityRoll(allQuests, used, categoryCounts, difficulty, false);

                if (quest != null) return quest;

            } else {

                ResolvedSlot item =
                        rollResolvedItem(category, used, rarity, blacklistedIds);

                if (item != null) return item;
            }
        }

        return null;
    }

    /* =========================================================
       FORCED COMMON (TOP-LEFT)
       ========================================================= */

    private static ResolvedSlot rollForcedCommon(
            List<QuestDefinition> allQuests,
            Set<String> used,
            Map<String, Integer> categoryCounts
    ) {
        return rollForcedCommon(allQuests, used, categoryCounts, Collections.emptySet());
    }

    private static ResolvedSlot rollForcedCommon(
            List<QuestDefinition> allQuests,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            Set<String> blacklistedIds
    ) {
        for (int attempts = 0; attempts < 60; attempts++) {

            List<String> categories = getCategoriesForRarity(allQuests, "common");
            if (categories.isEmpty()) continue;
            String category = pickWeightedCategory(categories, categoryCounts);

            if (QUEST_CATEGORY.equals(category)) {

                ResolvedSlot quest =
                        rollResolvedQuest(allQuests, used, categoryCounts, "common");

                if (quest != null) return quest;

            } else {

                ResolvedSlot item =
                        rollResolvedItem(category, used, "common", blacklistedIds);

                if (item != null) return item;
            }
        }

        return null;
    }

    /* =========================================================
       NO EPIC / LEGENDARY / MYTHIC (BOTTOM-RIGHT)
       ========================================================= */

    private static ResolvedSlot rollNoHighRarity(
            List<QuestDefinition> allQuests,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty
    ) {
        return rollNoHighRarity(allQuests, used, categoryCounts, difficulty, Collections.emptySet());
    }

    private static ResolvedSlot rollNoHighRarity(
            List<QuestDefinition> allQuests,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty,
            Set<String> blacklistedIds
    ) {
        for (int attempts = 0; attempts < 60; attempts++) {

            String rarity = rollNoHighRarity(difficulty);
            List<String> categories = getCategoriesForRarity(allQuests, rarity);
            if (categories.isEmpty()) continue;
            String category = pickWeightedCategory(categories, categoryCounts);

            if (QUEST_CATEGORY.equals(category)) {

                ResolvedSlot quest =
                        rollResolvedQuestWithRarityRoll(allQuests, used, categoryCounts, difficulty, true);

                if (quest != null) return quest;

            } else {

                ResolvedSlot item =
                        rollResolvedItem(category, used, rarity, blacklistedIds);

                if (item != null) return item;
            }
        }

        return rollForcedCommon(allQuests, used, categoryCounts, blacklistedIds);
    }

    /* =========================================================
       INTERNAL HELPERS
       ========================================================= */

    private static ResolvedSlot rollResolvedQuest(
            List<QuestDefinition> all,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String rarity
    ) {
        List<String> categories = all.stream()
                .filter(q -> q.rarity.equalsIgnoreCase(rarity))
                .map(q -> q.category)
                .distinct()
                .toList();

        if (categories.isEmpty()) return null;

        String category = pickWeightedCategory(categories, categoryCounts);

        List<QuestDefinition> candidates = all.stream()
                .filter(q -> q.rarity.equalsIgnoreCase(rarity))
                .filter(q -> category == null || q.category.equalsIgnoreCase(category))
                .filter(q -> !used.contains(q.id))
                .toList();

        if (candidates.isEmpty()) return null;

        QuestDefinition q = candidates.get(RANDOM.nextInt(candidates.size()));

        return new ResolvedSlot(
                q.id,
                q.name,
                q.category,
                q.rarity,
                true
        );
    }

    private static ResolvedSlot rollResolvedItem(
            String category,
            Set<String> used,
            String rarity,
            Set<String> blacklistedIds
    ) {
        for (int attempts = 0; attempts < 40; attempts++) {

            ItemDefinition def =
                    ItemDatabase.getRandomItem(rarity, category, RANDOM);

            if (def == null) continue;
            if (used.contains(def.id())) continue;
            if (blacklistedIds != null && blacklistedIds.contains(def.id())) continue;

            return new ResolvedSlot(
                    def.id(),
                    def.name(),
                    def.category(),
                    def.rarity(),
                    false
            );
        }

        return null;
    }

    private static List<String> getCategoriesForRarity(
            List<QuestDefinition> allQuests,
            String rarity
    ) {
        List<String> categories = new ArrayList<>(ItemDatabase.getCategoriesForRarity(rarity));

        boolean hasQuest = allQuests.stream()
                .anyMatch(q -> q.rarity.equalsIgnoreCase(rarity));
        if (hasQuest) {
            categories.add(QUEST_CATEGORY);
        }

        return categories;
    }

    private static String rollNoHighRarity(String difficulty) {
        for (int i = 0; i < 20; i++) {
            String rarity = DifficultyManager.rollRarity(difficulty);
            if (!rarity.equalsIgnoreCase("epic")
                    && !rarity.equalsIgnoreCase("legendary")
                    && !rarity.equalsIgnoreCase("mythic")) {
                return rarity;
            }
        }
        return "common";
    }

    private static ResolvedSlot rollResolvedQuestWithRarityRoll(
            List<QuestDefinition> allQuests,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty,
            boolean noHigh
    ) {
        for (int attempts = 0; attempts < 40; attempts++) {
            String rarity = noHigh ? rollNoHighRarity(difficulty) : DifficultyManager.rollRarity(difficulty);
            ResolvedSlot quest = rollResolvedQuest(allQuests, used, categoryCounts, rarity);
            if (quest != null) return quest;
        }
        return null;
    }

    private static String pickWeightedCategory(List<String> categories, Map<String, Integer> counts) {
        if (categories == null || categories.isEmpty()) return null;
        double total = 0;
        double[] weights = new double[categories.size()];

        for (int i = 0; i < categories.size(); i++) {
            String cat = categories.get(i);
            int count = counts.getOrDefault(cat.toLowerCase(Locale.ROOT), 0);
            double weight = 1.0 / (1.0 + count * 10.0);
            weights[i] = weight;
            total += weight;
        }

        double r = RANDOM.nextDouble() * total;
        for (int i = 0; i < categories.size(); i++) {
            r -= weights[i];
            if (r <= 0) return categories.get(i);
        }
        return categories.get(0);
    }

    private static void incrementCategory(Map<String, Integer> counts, String category) {
        if (category == null) return;
        String key = category.toLowerCase(Locale.ROOT);
        counts.merge(key, 1, Integer::sum);
    }
}
