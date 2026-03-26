package com.jamie.jamiebingo.bingo;

import java.util.Locale;

public final class BingoRarityValues {

    private BingoRarityValues() {}

    public static int baseValue(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "common" -> 1;
            case "uncommon" -> 2;
            case "rare" -> 4;
            case "epic" -> 8;
            case "legendary" -> 10;
            case "mythic" -> 15;
            default -> 1;
        };
    }
}
