package com.jamie.jamiebingo.client;

public class ClientStartCountdown {

    private static boolean active = false;
    private static long endTimeMillis = 0L;
    private static boolean autoOpenedFullscreenCard = false;

    public static void start(int seconds) {
        if (seconds <= 0) {
            clear();
            return;
        }
        active = true;
        endTimeMillis = System.currentTimeMillis() + (long) seconds * 1000L;
    }

    public static void clear() {
        active = false;
        endTimeMillis = 0L;
        autoOpenedFullscreenCard = false;
    }

    public static boolean isActive() {
        return active;
    }

    public static void setAutoOpenedFullscreenCard(boolean value) {
        autoOpenedFullscreenCard = value;
    }

    public static boolean wasAutoOpenedFullscreenCard() {
        return autoOpenedFullscreenCard;
    }

    public static int getSecondsRemaining() {
        if (!active) return 0;
        long remaining = endTimeMillis - System.currentTimeMillis();
        int seconds = (int) Math.ceil(remaining / 1000.0);
        if (seconds <= 0) {
            clear();
            return 0;
        }
        return seconds;
    }
}
