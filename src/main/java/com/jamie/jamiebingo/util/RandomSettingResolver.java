package com.jamie.jamiebingo.util;

import com.jamie.jamiebingo.bingo.WinCondition;

import java.util.List;
import java.util.Random;

public final class RandomSettingResolver {

    private static final Random RANDOM = new Random();

    private RandomSettingResolver() {}

    /* ===============================
       CARD SIZE (1–10, center = 5)
       =============================== */
    public static int rollCardSize() {
        return rollExponentialInt(1, 10, 5);
    }

    /* ===============================
       EFFECT TIMER (20–300, center = 60)
       =============================== */
    public static int rollEffectInterval() {
        return rollExponentialInt(20, 300, 60);
    }

    public static int pickPowerSlotInterval(Random rng) {
        Random source = rng == null ? RANDOM : rng;
        int min = 10;
        int max = 300;
        int center = 60;
        double total = 0.0D;
        double[] weights = new double[max - min + 1];
        for (int v = min; v <= max; v++) {
            double d = v - center;
            double w = Math.exp(-0.0045D * d * d);
            weights[v - min] = w;
            total += w;
        }
        double pick = source.nextDouble() * total;
        for (int i = 0; i < weights.length; i++) {
            pick -= weights[i];
            if (pick <= 0.0D) {
                return min + i;
            }
        }
        return center;
    }

    /* ===============================
       REROLLS (1–5, 1 most common)
       =============================== */
    public static int rollRerolls() {
        int roll = RANDOM.nextInt(100);
        if (roll < 40) return 1;
        if (roll < 65) return 2;
        if (roll < 82) return 3;
        if (roll < 93) return 4;
        return 5;
    }

    /* ===============================
       50 / 50 TOGGLES
       =============================== */
    public static boolean rollBoolean() {
        return RANDOM.nextBoolean();
    }

    /* ===============================
       HARDCORE (10% TRUE)
       =============================== */
    public static boolean rollHardcore() {
        return RANDOM.nextInt(100) < 10;
    }

    /* ===============================
       DIFFICULTY (EVEN)
       =============================== */
    public static Difficulty rollDifficulty() {
        return Difficulty.values()[RANDOM.nextInt(Difficulty.values().length)];
    }

    /* ===============================
       WIN MODE (EVEN)
       =============================== */
    public static WinCondition rollWinCondition(List<WinCondition> pool) {
        return pool.get(RANDOM.nextInt(pool.size()));
    }

    /* ===============================
       EXPONENTIAL INT ROLLER
       =============================== */
    private static int rollExponentialInt(int min, int max, int center) {
        double lambda = 0.35; // tweakable steepness
        double r = RANDOM.nextDouble();
        double sign = RANDOM.nextBoolean() ? 1 : -1;
        double offset = Math.log(1 - r) / -lambda;
        int value = (int) Math.round(center + sign * offset);

        return Math.max(min, Math.min(max, value));
    }

    /* ===============================
       DIFFICULTY ENUM
       =============================== */
    public enum Difficulty {
        EASY,
        NORMAL,
        HARD
    }
}
