package com.jamie.jamiebingo.bingo;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class DifficultyManager {

    private static final Random RANDOM = new Random();

    public static String rollRarity(String difficulty) {
        return rollRarity(difficulty, RANDOM);
    }

    public static String rollRarity(String difficulty, Random rng) {
        Random random = rng == null ? RANDOM : rng;

        Map<String, Double> table = new LinkedHashMap<>();

        String diffKey = difficulty == null ? "normal" : difficulty.toLowerCase();
        if (diffKey.startsWith("custom:")) {
            double[] weights = parseCustomWeights(difficulty);
            if (weights != null) {
                table.put("common", weights[0]);
                table.put("uncommon", weights[1]);
                table.put("rare", weights[2]);
                table.put("epic", weights[3]);
                table.put("legendary", weights[4]);
                table.put("mythic", weights[5]);
            }
        }

        if (table.isEmpty()) switch (diffKey) {

            case "easy":
                // Very relaxed: Mythic basically never shows.
                table.put("common",     85.0);
                table.put("uncommon",   12.0);
                table.put("rare",        2.3);
                table.put("epic",        0.5);
                table.put("legendary",   0.19);
                table.put("mythic",      0.01);
                break;

            case "hard":
                // Still challenging; more high-tier but Mythic remains rare.
                table.put("common",     55.0);
                table.put("uncommon",   25.0);
                table.put("rare",       11.0);
                table.put("epic",        6.0);
                table.put("legendary",   2.5);
                table.put("mythic",      0.5);
                break;

            case "extreme":
                // Very punishing: common almost never, high-tier much more likely.
                table.put("common",      2.0);
                table.put("uncommon",    6.0);
                table.put("rare",       18.0);
                table.put("epic",       28.0);
                table.put("legendary",  26.0);
                table.put("mythic",     20.0);
                break;

            default: // normal
                table.put("common",     75.0);
                table.put("uncommon",   17.0);
                table.put("rare",        5.5);
                table.put("epic",        1.8);
                table.put("legendary",   0.6);
                table.put("mythic",      0.1);
                break;
        }

        double total = 0;
        for (double v : table.values()) total += v;

        double roll = random.nextDouble() * total;
        double cumulative = 0;

        for (var entry : table.entrySet()) {
            cumulative += entry.getValue();
            if (roll <= cumulative) {
                return entry.getKey();
            }
        }

        // Failsafe
        return "common";
    }

    private static double[] parseCustomWeights(String difficulty) {
        if (difficulty == null) return null;
        String raw = difficulty.toLowerCase();
        int idx = raw.indexOf("custom:");
        if (idx < 0) return null;
        String values = raw.substring(idx + "custom:".length());
        String[] parts = values.split(",");
        if (parts.length < 6) return null;
        double[] out = new double[6];
        try {
            for (int i = 0; i < 6; i++) {
                out[i] = Math.max(0.0, Double.parseDouble(parts[i].trim()));
            }
        } catch (Exception e) {
            return null;
        }
        return out;
    }
}
