package com.jamie.jamiebingo.client;

public final class ClientHangmanState {

    public static boolean active = false;
    public static boolean slotRevealed = false;
    public static String line1 = "";
    public static String line2 = "";

    private ClientHangmanState() {
    }

    public static void update(boolean activeValue, boolean revealed, String l1, String l2) {
        active = activeValue;
        slotRevealed = revealed;
        line1 = l1 == null ? "" : l1;
        line2 = l2 == null ? "" : l2;
    }

    public static void clear() {
        active = false;
        slotRevealed = false;
        line1 = "";
        line2 = "";
    }
}
