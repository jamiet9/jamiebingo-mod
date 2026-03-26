package com.jamie.jamiebingo.bingo;

import java.util.Map;

public final class RarityScoreCalculator {

    private RarityScoreCalculator() {}

    // Default values (configurable later)
    public static final Map<String, Integer> BASE_POINTS = Map.of(
            "common", 1,
            "uncommon", 2,
            "rare", 4,
            "epic", 8,
            "legendary", 10,
            "mythic", 15
    );

    public static int base(BingoSlot slot) {
        return BASE_POINTS.getOrDefault(
                slot.getRarity().toLowerCase(),
                1
        );
    }

    public static int withLineBonus(BingoSlot slot, boolean doubled) {
        int base = base(slot);
        return doubled ? base * 2 : base;
    }
}
