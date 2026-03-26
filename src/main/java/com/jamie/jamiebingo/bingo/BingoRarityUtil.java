package com.jamie.jamiebingo.bingo;

import java.util.List;
import java.util.Locale;

public final class BingoRarityUtil {
    public static final List<String> ORDERED_RARITIES = List.of(
            "common",
            "uncommon",
            "rare",
            "epic",
            "legendary",
            "mythic",
            "impossible"
    );

    private BingoRarityUtil() {
    }

    public static String normalize(String rarity) {
        if (rarity == null || rarity.isBlank()) {
            return "";
        }
        return rarity.trim().toLowerCase(Locale.ROOT);
    }

    public static int rarityIndex(String rarity) {
        String normalized = normalize(rarity);
        int index = ORDERED_RARITIES.indexOf(normalized);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }

    public static String lessRare(String first, String second) {
        if (normalize(first).isBlank()) return normalize(second);
        if (normalize(second).isBlank()) return normalize(first);
        return rarityIndex(first) <= rarityIndex(second) ? normalize(first) : normalize(second);
    }

    public static boolean isKnown(String rarity) {
        return ORDERED_RARITIES.contains(normalize(rarity));
    }
}
