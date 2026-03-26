package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;

import java.util.*;

public class QuestCardGenerator {

    private static final Random RANDOM = new Random();

    /* =========================================================
       PUBLIC API
       ========================================================= */

    public static BingoCard generate(
            int size,
            String difficulty,
            net.minecraft.server.MinecraftServer server,
            com.jamie.jamiebingo.data.BingoGameData data
    ) {

        List<QuestDefinition> all =
                new ArrayList<>(QuestDatabase.getEligibleQuests(server, data));

        if (all.isEmpty()) {
            System.out.println("[JamieBingo] WARNING: No quests available. Empty quest card generated.");
            return new BingoCard(size);
        }

        Set<String> used = new HashSet<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        BingoCard card = new BingoCard(size);

        boolean blindMode = data != null && data.winCondition == WinCondition.BLIND;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {

                ResolvedSlot resolved;

                // ===============================
                // TOP-LEFT: MUST BE COMMON
                // ===============================
                if (blindMode && x == 0 && y == 0) {
                    resolved = rollForcedCommon(all, used, categoryCounts);
                }

                // ===============================
                // BOTTOM-RIGHT: NOT EPIC+
                // ===============================
                else if (blindMode && x == size - 1 && y == size - 1) {
                    resolved = rollNoHighRarity(all, used, categoryCounts, difficulty);
                }

                // ===============================
                // NORMAL SLOT
                // ===============================
                else {
                    resolved = rollResolvedSlot(all, used, categoryCounts, difficulty);
                }

                if (resolved == null) {
                    resolved = new ResolvedSlot(
                            "minecraft:dirt",
                            "Dirt",
                            "Misc",
                            "Common",
                            true
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
                " QUEST card (forced start/end rules)");

        return card;
    }

    /* =========================================================
       NORMAL ROLL
       ========================================================= */

    public static ResolvedSlot rollResolvedSlot(
            List<QuestDefinition> all,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty
    ) {
        String rarity = DifficultyManager.rollRarity(difficulty);

        List<String> categories = all.stream()
                .filter(q -> q.rarity.equalsIgnoreCase(rarity))
                .map(q -> q.category)
                .distinct()
                .toList();

        String category = pickWeightedCategory(categories, categoryCounts);

        List<QuestDefinition> candidates = all.stream()
                .filter(q -> q.rarity.equalsIgnoreCase(rarity))
                .filter(q -> category == null || q.category.equalsIgnoreCase(category))
                .filter(q -> !used.contains(q.id))
                .toList();

        if (candidates.isEmpty()) {
            candidates = all.stream()
                    .filter(q -> !used.contains(q.id))
                    .toList();
        }

        if (candidates.isEmpty()) return null;

        QuestDefinition chosen =
                candidates.get(RANDOM.nextInt(candidates.size()));

        return new ResolvedSlot(
                chosen.id,
                chosen.name,
                chosen.category,
                chosen.rarity,
                true
        );
    }

    /* =========================================================
       FORCED COMMON (TOP-LEFT)
       ========================================================= */

    private static ResolvedSlot rollForcedCommon(
            List<QuestDefinition> all,
            Set<String> used,
            Map<String, Integer> categoryCounts
    ) {
        List<String> categories = all.stream()
                .filter(q -> q.rarity.equalsIgnoreCase("common"))
                .map(q -> q.category)
                .distinct()
                .toList();

        String category = pickWeightedCategory(categories, categoryCounts);

        List<QuestDefinition> commons = all.stream()
                .filter(q -> q.rarity.equalsIgnoreCase("common"))
                .filter(q -> category == null || q.category.equalsIgnoreCase(category))
                .filter(q -> !used.contains(q.id))
                .toList();

        if (!commons.isEmpty()) {
            QuestDefinition q =
                    commons.get(RANDOM.nextInt(commons.size()));

            return new ResolvedSlot(
                    q.id,
                    q.name,
                    q.category,
                    q.rarity,
                    true
            );
        }

        return rollResolvedSlot(all, used, categoryCounts, "easy");
    }

    /* =========================================================
       NO EPIC / LEGENDARY / MYTHIC (BOTTOM-RIGHT)
       ========================================================= */

    private static ResolvedSlot rollNoHighRarity(
            List<QuestDefinition> all,
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty
    ) {
        for (int attempts = 0; attempts < 60; attempts++) {

            String rarity = DifficultyManager.rollRarity(difficulty);

            if (rarity.equalsIgnoreCase("epic")
                    || rarity.equalsIgnoreCase("legendary")
                    || rarity.equalsIgnoreCase("mythic")) {
                continue;
            }

            List<String> categories = all.stream()
                    .filter(q -> q.rarity.equalsIgnoreCase(rarity))
                    .map(q -> q.category)
                    .distinct()
                    .toList();

            String category = pickWeightedCategory(categories, categoryCounts);

            List<QuestDefinition> candidates = all.stream()
                    .filter(q -> q.rarity.equalsIgnoreCase(rarity))
                    .filter(q -> category == null || q.category.equalsIgnoreCase(category))
                    .filter(q -> !used.contains(q.id))
                    .toList();

            if (candidates.isEmpty()) continue;

            QuestDefinition q =
                    candidates.get(RANDOM.nextInt(candidates.size()));

            return new ResolvedSlot(
                    q.id,
                    q.name,
                    q.category,
                    q.rarity,
                    true
            );
        }

        return rollForcedCommon(all, used, categoryCounts);
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
