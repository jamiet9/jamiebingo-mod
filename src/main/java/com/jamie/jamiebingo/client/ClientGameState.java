package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.bingo.WinCondition;

public class ClientGameState {

    /* =========================
       GAME STATE
       ========================= */

    private static boolean casinoActive = false;
    private static boolean gameActive = false;
    public static boolean teamChestEnabled = true;

    // REQUIRED by BingoCardScreen & OverlayRenderer
    public static WinCondition winCondition = WinCondition.LINE;

    // REQUIRED by OverlayRenderer
    public static boolean overlayEnabled = true;

    /* =========================
       CASINO
       ========================= */

    public static void enterCasino() {
        casinoActive = true;
        gameActive = false;
    }

    public static void exitCasino() {
        casinoActive = false;
        gameActive = true;
    }

    public static boolean isCasinoActive() {
        return casinoActive;
    }

    /* =========================
       NORMAL BINGO
       ========================= */

    public static void enterGame() {
        gameActive = true;
        casinoActive = false;
    }

    public static void exitGame() {
        gameActive = false;
    }

    public static boolean isGameActive() {
        return gameActive;
    }
}
