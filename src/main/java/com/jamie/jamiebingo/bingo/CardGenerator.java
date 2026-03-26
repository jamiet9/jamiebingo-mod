package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.ItemDefinition;
import com.jamie.jamiebingo.data.ItemDatabase;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;

import java.util.*;

public class CardGenerator {

    private static final Random RANDOM = new Random();

    /* =========================================================
   PUBLIC API
   ========================================================= */

public static BingoCard generate(int size, String difficulty, WinCondition winCondition) {
    return generate(size, difficulty, winCondition, Collections.emptySet());
}

public static BingoCard generate(int size, String difficulty, WinCondition winCondition, Set<String> blacklistedIds) {

    ItemDatabase.load();

    BingoCard card = new BingoCard(size);
    Set<String> used = new HashSet<>();
    Map<String, Integer> categoryCounts = new HashMap<>();

    boolean blindMode = (winCondition == WinCondition.BLIND);

    for (int y = 0; y < size; y++) {
        for (int x = 0; x < size; x++) {

            ResolvedSlot resolved;

            // ===============================
            // BLIND MODE SPECIAL RULES
            // ===============================
            if (blindMode && x == 0 && y == 0) {
                // TOP-LEFT: ALWAYS COMMON (BLIND ONLY)
                resolved = rollForcedCommon(used, categoryCounts, blacklistedIds);
            }
            else if (blindMode && x == size - 1 && y == size - 1) {
                // BOTTOM-RIGHT: NEVER EPIC+ (BLIND ONLY)
                resolved = rollNoHighRarity(used, categoryCounts, difficulty, blacklistedIds);
            }

            // ===============================
            // NORMAL SLOT (ALL OTHER CASES)
            // ===============================
            else {
                resolved = rollResolvedSlot(used, categoryCounts, difficulty, blacklistedIds);
            }

            // safety fallback
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
            " card" + (blindMode ? " (BLIND rules applied)" : ""));

    return card;
}

    /* =========================================================
       NORMAL ROLL
       ========================================================= */

    public static ResolvedSlot rollResolvedSlot(
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty
    ) {
        return rollResolvedSlot(used, categoryCounts, difficulty, Collections.emptySet());
    }

    public static ResolvedSlot rollResolvedSlot(
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty,
            Set<String> blacklistedIds
    ) {
        ItemDefinition chosen = null;
        String chosenRarity = "common";

        for (int attempts = 0; attempts < 40; attempts++) {

            String rarity = DifficultyManager.rollRarity(difficulty);
            String category = pickWeightedCategory(ItemDatabase.getCategories(), categoryCounts);

            ItemDefinition def =
                    ItemDatabase.getRandomItem(rarity, category, RANDOM);

            if (def == null) continue;
            if (used.contains(def.id())) continue;
            if (blacklistedIds != null && blacklistedIds.contains(def.id())) continue;

            chosen = def;
            chosenRarity = rarity.toLowerCase();
            break;
        }

        if (chosen == null) {
            chosen = ItemDatabase.getRandomAny(RANDOM);
            if (chosen != null && blacklistedIds != null && blacklistedIds.contains(chosen.id())) {
                chosen = null;
            }
            if (chosen != null) {
                chosenRarity = chosen.rarity().toLowerCase();
            }
        }

        if (chosen == null) return null;

        return new ResolvedSlot(
                chosen.id(),
                chosen.name(),
                chosen.category(),
                chosen.rarity(),
                false
        );
    }

    /* =========================================================
       FORCED COMMON (TOP-LEFT)
       ========================================================= */

    private static ResolvedSlot rollForcedCommon(Set<String> used, Map<String, Integer> categoryCounts) {
        return rollForcedCommon(used, categoryCounts, Collections.emptySet());
    }

    private static ResolvedSlot rollForcedCommon(Set<String> used, Map<String, Integer> categoryCounts, Set<String> blacklistedIds) {

        for (int attempts = 0; attempts < 60; attempts++) {

            String category = pickWeightedCategory(ItemDatabase.getCategories(), categoryCounts);
            ItemDefinition def =
                    ItemDatabase.getRandomItem("common", category, RANDOM);

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

        return rollResolvedSlot(used, categoryCounts, "easy", blacklistedIds);
    }

    /* =========================================================
       NO EPIC / LEGENDARY / MYTHIC (BOTTOM-RIGHT)
       ========================================================= */

    private static ResolvedSlot rollNoHighRarity(
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty
    ) {
        return rollNoHighRarity(used, categoryCounts, difficulty, Collections.emptySet());
    }

    private static ResolvedSlot rollNoHighRarity(
            Set<String> used,
            Map<String, Integer> categoryCounts,
            String difficulty,
            Set<String> blacklistedIds
    ) {
        for (int attempts = 0; attempts < 60; attempts++) {

            String rarity = DifficultyManager.rollRarity(difficulty);

            if (rarity.equalsIgnoreCase("epic")
                    || rarity.equalsIgnoreCase("legendary")
                    || rarity.equalsIgnoreCase("mythic")) {
                continue;
            }

            String category = pickWeightedCategory(ItemDatabase.getCategories(), categoryCounts);
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

        return rollForcedCommon(used, categoryCounts, blacklistedIds);
    }

    private static String pickWeightedCategory(List<String> categories, Map<String, Integer> counts) {
        if (categories == null || categories.isEmpty()) return "misc";
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
