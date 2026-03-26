package com.jamie.jamiebingo.bingo;

import com.jamie.jamiebingo.ItemDefinition;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.ItemDatabase;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConfigurableCardGenerator {

    private static final Random RANDOM = new Random();
    private static final String QUEST_CATEGORY = "QUEST";

    private ConfigurableCardGenerator() {
    }

    public static BingoCard generate(
            int size,
            String difficulty,
            CardComposition composition,
            int questPercent,
            net.minecraft.server.MinecraftServer server,
            BingoGameData data,
            Set<String> blacklistedIds
    ) {
        return generate(size, difficulty, composition, questPercent, server, data, blacklistedIds, RANDOM);
    }

    public static BingoCard generate(
            int size,
            String difficulty,
            CardComposition composition,
            int questPercent,
            net.minecraft.server.MinecraftServer server,
            BingoGameData data,
            Set<String> blacklistedIds,
            Random rng
    ) {
        Random random = rng == null ? RANDOM : rng;
        ItemDatabase.load();
        QuestDatabase.load();

        List<QuestDefinition> allQuests = new ArrayList<>(QuestDatabase.getEligibleQuests(server, data));
        List<ItemDefinition> allItems = new ArrayList<>(ItemDatabase.getAllowedItems());
        Set<String> blocked = blacklistedIds == null ? Collections.emptySet() : blacklistedIds;

        BingoCard card = new BingoCard(size);
        Map<String, Integer> categoryCounts = new HashMap<>();
        Set<String> usedKeys = new java.util.HashSet<>();

        boolean blindMode = data != null && data.winCondition == WinCondition.BLIND;
        boolean categoryLogic = data == null || data.categoryLogicEnabled;
        boolean rarityLogic = data == null || data.rarityLogicEnabled;
        boolean separateItemColors = data != null && data.itemColorVariantsSeparate;

        int effectiveQuestPercent = clamp(questPercent, 0, 100);
        if (composition == CardComposition.HYBRID_CATEGORY && !categoryLogic) {
            effectiveQuestPercent = 35;
        }

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean forceCommon = blindMode && x == 0 && y == 0;
                boolean noHighRarity = blindMode && x == size - 1 && y == size - 1;

                ResolvedSlot resolved = rollResolvedSlot(
                        allQuests,
                        allItems,
                        usedKeys,
                        categoryCounts,
                        difficulty,
                        composition,
                        effectiveQuestPercent,
                        categoryLogic,
                        rarityLogic,
                        separateItemColors,
                        forceCommon,
                        noHighRarity,
                        blocked,
                        random
                );

                if (resolved == null) {
                    resolved = new ResolvedSlot("minecraft:dirt", "Dirt", "Misc", "Common", false);
                }

                usedKeys.add(groupKeyForResolved(resolved, separateItemColors));
                if (categoryLogic) {
                    if (resolved.isQuest && composition == CardComposition.HYBRID_CATEGORY) {
                        incrementCategory(categoryCounts, QUEST_CATEGORY);
                    }
                    incrementCategory(categoryCounts, resolved.category);
                }

                card.setSlot(x, y, new BingoSlot(resolved.id, resolved.name, resolved.category, resolved.rarity));
            }
        }

        return card;
    }

    public static ResolvedSlot rollResolvedSlot(
            BingoGameData data,
            net.minecraft.server.MinecraftServer server,
            Set<String> usedKeys,
            Map<String, Integer> categoryCounts,
            String difficulty,
            CardComposition composition,
            int questPercent,
            Set<String> blacklistedIds
    ) {
        return rollResolvedSlot(data, server, usedKeys, categoryCounts, difficulty, composition, questPercent, blacklistedIds, RANDOM);
    }

    public static ResolvedSlot rollResolvedSlot(
            BingoGameData data,
            net.minecraft.server.MinecraftServer server,
            Set<String> usedKeys,
            Map<String, Integer> categoryCounts,
            String difficulty,
            CardComposition composition,
            int questPercent,
            Set<String> blacklistedIds,
            Random rng
    ) {
        Random random = rng == null ? RANDOM : rng;
        ItemDatabase.load();
        QuestDatabase.load();
        List<QuestDefinition> allQuests = new ArrayList<>(QuestDatabase.getEligibleQuests(server, data));
        List<ItemDefinition> allItems = new ArrayList<>(ItemDatabase.getAllowedItems());

        boolean categoryLogic = data == null || data.categoryLogicEnabled;
        boolean rarityLogic = data == null || data.rarityLogicEnabled;
        boolean separateItemColors = data != null && data.itemColorVariantsSeparate;
        int effectiveQuestPercent = clamp(questPercent, 0, 100);
        if (composition == CardComposition.HYBRID_CATEGORY && !categoryLogic) {
            effectiveQuestPercent = 35;
        }

        return rollResolvedSlot(
                allQuests,
                allItems,
                usedKeys,
                categoryCounts,
                difficulty,
                composition,
                effectiveQuestPercent,
                categoryLogic,
                rarityLogic,
                separateItemColors,
                false,
                false,
                blacklistedIds == null ? Collections.emptySet() : blacklistedIds,
                random
        );
    }

    private static ResolvedSlot rollResolvedSlot(
            List<QuestDefinition> allQuests,
            List<ItemDefinition> allItems,
            Set<String> usedKeys,
            Map<String, Integer> categoryCounts,
            String difficulty,
            CardComposition composition,
            int questPercent,
            boolean categoryLogic,
            boolean rarityLogic,
            boolean separateItemColors,
            boolean forceCommon,
            boolean noHighRarity,
            Set<String> blacklistedIds,
            Random rng
    ) {
        Random random = rng == null ? RANDOM : rng;
        for (int attempts = 0; attempts < 80; attempts++) {
            String targetRarity = pickTargetRarity(difficulty, rarityLogic, forceCommon, noHighRarity, random);
            Choice choice = chooseTypeAndCategory(
                    composition,
                    questPercent,
                    categoryLogic,
                    targetRarity,
                    allQuests,
                    allItems,
                    categoryCounts,
                    random
            );

            if (choice.useQuest) {
                ResolvedSlot quest = rollQuest(
                        allQuests,
                        usedKeys,
                        categoryCounts,
                        choice.forcedCategory,
                        targetRarity,
                        categoryLogic,
                        blacklistedIds,
                        random
                );
                if (quest != null) return quest;
            } else {
                ResolvedSlot item = rollItem(
                        allItems,
                        usedKeys,
                        categoryCounts,
                        choice.forcedCategory,
                        targetRarity,
                        categoryLogic,
                        separateItemColors,
                        blacklistedIds,
                        random
                );
                if (item != null) return item;
            }
        }
        return null;
    }

    private static String pickTargetRarity(String difficulty, boolean rarityLogic, boolean forceCommon, boolean noHighRarity, Random rng) {
        if (forceCommon) return "common";
        if (noHighRarity) {
            for (int i = 0; i < 20; i++) {
                String r = rarityLogic ? DifficultyManager.rollRarity(difficulty, rng) : randomRarity(rng);
                if (!isHighRarity(r)) return r;
            }
            return "common";
        }
        return rarityLogic ? DifficultyManager.rollRarity(difficulty, rng) : randomRarity(rng);
    }

    private static ResolvedSlot rollQuest(
            List<QuestDefinition> allQuests,
            Set<String> usedKeys,
            Map<String, Integer> categoryCounts,
            String forcedCategory,
            String targetRarity,
            boolean categoryLogic,
            Set<String> blacklistedIds,
            Random rng
    ) {
        if (allQuests.isEmpty()) return null;

        List<QuestDefinition> base = allQuests.stream()
                .filter(q -> q != null && q.id != null && !q.id.isBlank())
                .filter(q -> blacklistedIds == null || !blacklistedIds.contains(q.id))
                .filter(q -> !usedKeys.contains(ColorVariantUtil.questGroupKey(q)))
                .filter(q -> targetRarity == null || q.rarity.equalsIgnoreCase(targetRarity))
                .toList();
        if (base.isEmpty()) return null;

        List<QuestDefinition> candidates = base;
        if (categoryLogic) {
                String category = forcedCategory;
                if (category == null || category.isBlank()) {
                    List<String> categories = base.stream().map(q -> q.category).distinct().toList();
                    category = pickWeightedCategory(categories, categoryCounts, rng);
                }
            if (category != null && !category.isBlank()) {
                String finalCategory = category;
                candidates = base.stream()
                        .filter(q -> q.category != null && q.category.equalsIgnoreCase(finalCategory))
                        .toList();
                if (candidates.isEmpty()) return null;
            }
        }

        QuestDefinition chosen = chooseGrouped(candidates, ColorVariantUtil::questGroupKey, rng);
        if (chosen == null) return null;

        return new ResolvedSlot(chosen.id, chosen.name, chosen.category, chosen.rarity, true);
    }

    private static ResolvedSlot rollItem(
            List<ItemDefinition> allItems,
            Set<String> usedKeys,
            Map<String, Integer> categoryCounts,
            String forcedCategory,
            String targetRarity,
            boolean categoryLogic,
            boolean separateItemColors,
            Set<String> blacklistedIds,
            Random rng
    ) {
        if (allItems.isEmpty()) return null;

        List<ItemDefinition> base = allItems.stream()
                .filter(i -> i != null && i.id() != null && !i.id().isBlank())
                .filter(i -> blacklistedIds == null || !blacklistedIds.contains(i.id()))
                .filter(i -> !usedKeys.contains(ColorVariantUtil.itemGroupKey(i, separateItemColors)))
                .filter(i -> targetRarity == null || i.rarity().equalsIgnoreCase(targetRarity))
                .toList();
        if (base.isEmpty()) return null;

        List<ItemDefinition> candidates = base;
        if (categoryLogic) {
                String category = forcedCategory;
                if (category == null || category.isBlank()) {
                    List<String> categories = base.stream().map(ItemDefinition::category).distinct().toList();
                    category = pickWeightedCategory(categories, categoryCounts, rng);
                }
            if (category != null && !category.isBlank()) {
                String finalCategory = category;
                candidates = base.stream()
                        .filter(i -> i.category() != null && i.category().equalsIgnoreCase(finalCategory))
                        .toList();
                if (candidates.isEmpty()) return null;
            }
        }

        Random random = rng == null ? RANDOM : rng;
        ItemDefinition chosen = separateItemColors
                ? candidates.get(random.nextInt(candidates.size()))
                : chooseGrouped(candidates, i -> ColorVariantUtil.itemGroupKey(i, false), random);

        if (chosen == null) return null;

        return new ResolvedSlot(chosen.id(), chosen.name(), chosen.category(), chosen.rarity(), false);
    }

    private static Choice chooseTypeAndCategory(
            CardComposition composition,
            int questPercent,
            boolean categoryLogic,
            String targetRarity,
            List<QuestDefinition> allQuests,
            List<ItemDefinition> allItems,
            Map<String, Integer> categoryCounts,
            Random rng
    ) {
        Random random = rng == null ? RANDOM : rng;
        if (composition == CardComposition.QUEST_ONLY) {
            return new Choice(true, null);
        }
        if (composition == CardComposition.CLASSIC_ONLY) {
            return new Choice(false, null);
        }
        if (composition == CardComposition.HYBRID_PERCENT || !categoryLogic) {
            boolean quest = !allQuests.isEmpty() && (questPercent >= 100
                    || (questPercent > 0 && random.nextInt(100) < questPercent));
            if (allItems.isEmpty()) quest = true;
            return new Choice(quest, null);
        }

        Map<String, Integer> categoryToType = new LinkedHashMap<>();
        List<String> itemCats = itemCategoriesForRarity(allItems, targetRarity);
        for (String cat : itemCats) {
            if (cat != null && !cat.isBlank()) {
                categoryToType.put(cat, 0);
            }
        }

        boolean hasQuest = allQuests.stream()
                .anyMatch(q -> q != null && q.rarity != null && q.rarity.equalsIgnoreCase(targetRarity));
        if (hasQuest) {
            categoryToType.put(QUEST_CATEGORY, 1);
        }
        if (categoryToType.isEmpty()) {
            return new Choice(!allQuests.isEmpty() && allItems.isEmpty(), null);
        }

        String picked = pickWeightedCategory(new ArrayList<>(categoryToType.keySet()), categoryCounts, random);
        if (picked == null) {
            return new Choice(!allQuests.isEmpty() && allItems.isEmpty(), null);
        }
        if (QUEST_CATEGORY.equals(picked)) {
            return new Choice(true, null);
        }
        return new Choice(false, picked);
    }

    private static List<String> itemCategoriesForRarity(List<ItemDefinition> allItems, String rarity) {
        return allItems.stream()
                .filter(i -> i != null && i.rarity() != null && i.rarity().equalsIgnoreCase(rarity))
                .map(ItemDefinition::category)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private static <T> T chooseGrouped(List<T> entries, java.util.function.Function<T, String> keyFn, Random rng) {
        if (entries == null || entries.isEmpty()) return null;
        Random random = rng == null ? RANDOM : rng;
        Map<String, List<T>> grouped = new LinkedHashMap<>();
        for (T entry : entries) {
            String key = keyFn.apply(entry);
            if (key == null || key.isBlank()) continue;
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(entry);
        }
        if (grouped.isEmpty()) return null;
        List<String> keys = new ArrayList<>(grouped.keySet());
        String chosenKey = keys.get(random.nextInt(keys.size()));
        List<T> group = grouped.get(chosenKey);
        return group.get(random.nextInt(group.size()));
    }

    private static String groupKeyForResolved(ResolvedSlot resolved, boolean separateItemColors) {
        if (resolved == null) return "";
        if (resolved.isQuest) {
            QuestDefinition q = new QuestDefinition(
                    resolved.id,
                    resolved.name,
                    "",
                    resolved.rarity,
                    resolved.category,
                    "",
                    "",
                    null
            );
            return ColorVariantUtil.questGroupKey(q);
        }
        return separateItemColors ? resolved.id : ColorVariantUtil.itemGroupKey(resolved.id);
    }

    public static String uniqueKeyForSlot(ResolvedSlot resolved, boolean separateItemColors) {
        return groupKeyForResolved(resolved, separateItemColors);
    }

    private static String randomRarity(Random rng) {
        Random random = rng == null ? RANDOM : rng;
        return switch (random.nextInt(6)) {
            case 0 -> "common";
            case 1 -> "uncommon";
            case 2 -> "rare";
            case 3 -> "epic";
            case 4 -> "legendary";
            default -> "mythic";
        };
    }

    private static boolean isHighRarity(String rarity) {
        if (rarity == null) return false;
        String r = rarity.toLowerCase(Locale.ROOT);
        return r.equals("epic") || r.equals("legendary") || r.equals("mythic");
    }

    private static String pickWeightedCategory(List<String> categories, Map<String, Integer> counts, Random rng) {
        if (categories == null || categories.isEmpty()) return null;
        Random random = rng == null ? RANDOM : rng;
        double total = 0.0;
        double[] weights = new double[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            String cat = categories.get(i);
            int count = counts.getOrDefault(cat.toLowerCase(Locale.ROOT), 0);
            double weight = 1.0 / (1.0 + count * 10.0);
            weights[i] = weight;
            total += weight;
        }
        double r = random.nextDouble() * total;
        for (int i = 0; i < categories.size(); i++) {
            r -= weights[i];
            if (r <= 0) return categories.get(i);
        }
        return categories.get(0);
    }

    private static void incrementCategory(Map<String, Integer> counts, String category) {
        if (category == null || category.isBlank()) return;
        counts.merge(category.toLowerCase(Locale.ROOT), 1, Integer::sum);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private record Choice(boolean useQuest, String forcedCategory) {
    }
}
