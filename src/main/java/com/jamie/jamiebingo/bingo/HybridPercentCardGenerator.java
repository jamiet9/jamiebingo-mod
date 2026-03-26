package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.ItemDefinition;
import com.jamie.jamiebingo.data.ItemDatabase;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;

import java.util.*;

public class HybridPercentCardGenerator {

    private static final Random RANDOM = new Random();

    /* =========================================================
       PUBLIC API — FULL FILE
       ========================================================= */

    public static BingoCard generate(
            int size,
            String difficulty,
            int questPercent,
            net.minecraft.server.MinecraftServer server,
            com.jamie.jamiebingo.data.BingoGameData data
    ) {

        ItemDatabase.load();
        QuestDatabase.load();

        int clampedQuestPercent = Math.max(0, Math.min(100, questPercent));

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
                    resolved = rollForcedCommon(
                            allQuests,
                            used,
                            categoryCounts,
                            clampedQuestPercent,
                            blacklistedIds
                    );
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
                            clampedQuestPercent,
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
                            clampedQuestPercent,
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
                " HYBRID (percent=" + clampedQuestPercent + "%) card with forced start/end rules");

        return card;
    }

    /* =========================================================
       NORMAL ROLL
       ========================================================= */

    public static ResolvedSlot rollResolvedSlot(
            List<QuestDefinition> allQuests,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty,
            int questPercent
    ) {
        return rollResolvedSlot(allQuests, used, categoryCounts, difficulty, questPercent, Collections.emptySet());
    }

    public static ResolvedSlot rollResolvedSlot(
            List<QuestDefinition> allQuests,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty,
            int questPercent,
            Set<String> blacklistedIds
    ) {
        for (int attempts = 0; attempts < 40; attempts++) {

            boolean useQuest = questPercent >= 100
                    || (questPercent > 0 && RANDOM.nextInt(100) < questPercent);
            if (allQuests.isEmpty()) {
                useQuest = false;
            }
            String rarity = DifficultyManager.rollRarity(difficulty);

            if (useQuest && !allQuests.isEmpty()) {

                ResolvedSlot quest =
                        rollResolvedQuest(allQuests, used, categoryCounts, rarity);

                if (quest != null) return quest;

            } else {

                ResolvedSlot item =
                        rollResolvedItem(used, categoryCounts, rarity, blacklistedIds);

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
            Map<String, Integer> categoryCounts,
            int questPercent,
            Set<String> blacklistedIds
    ) {
        for (int attempts = 0; attempts < 60; attempts++) {

            boolean useQuest = questPercent >= 100
                    || (questPercent > 0 && RANDOM.nextInt(100) < questPercent);
            if (allQuests.isEmpty()) {
                useQuest = false;
            }

            if (useQuest && !allQuests.isEmpty()) {

                ResolvedSlot quest =
                        rollResolvedQuest(allQuests, used, categoryCounts, "common");

                if (quest != null) return quest;

            } else {

                ResolvedSlot item =
                        rollResolvedItem(used, categoryCounts, "common", blacklistedIds);

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
            String difficulty,
            int questPercent,
            Set<String> blacklistedIds
    ) {
        for (int attempts = 0; attempts < 60; attempts++) {

            boolean useQuest = questPercent >= 100
                    || (questPercent > 0 && RANDOM.nextInt(100) < questPercent);
            if (allQuests.isEmpty()) {
                useQuest = false;
            }
            String rarity = DifficultyManager.rollRarity(difficulty);

            if (rarity.equalsIgnoreCase("epic")
                    || rarity.equalsIgnoreCase("legendary")
                    || rarity.equalsIgnoreCase("mythic")) {
                continue;
            }

            if (useQuest && !allQuests.isEmpty()) {

                ResolvedSlot quest =
                        rollResolvedQuest(allQuests, used, categoryCounts, rarity);

                if (quest != null) return quest;

            } else {

                ResolvedSlot item =
                        rollResolvedItem(used, categoryCounts, rarity, blacklistedIds);

                if (item != null) return item;
            }
        }

        return rollForcedCommon(allQuests, used, categoryCounts, questPercent, blacklistedIds);
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
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String rarity,
            Set<String> blacklistedIds
    ) {
        for (int attempts = 0; attempts < 40; attempts++) {

            String category =
                    pickWeightedCategory(ItemDatabase.getCategoriesForRarity(rarity), categoryCounts);

            if (category == null || category.isBlank()) {
                return null;
            }

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
