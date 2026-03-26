package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.bingo.BingoCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WeeklyChallengeClientState {
    private static boolean loading;
    private static long baseSeed;
    private static long nextResetEpochSeconds;
    private static String challengeId = "";
    private static String error = "";
    private static BingoCard card;
    private static List<String> settingsLines = new ArrayList<>();
    private static String cardSeed = "";
    private static String worldSeed = "";
    private static String settingsSeed = "";

    private WeeklyChallengeClientState() {
    }

    public static void beginLoading() {
        loading = true;
        error = "";
    }

    public static void setFetchResult(long newBaseSeed, long newNextResetEpochSeconds, String newChallengeId, String errorMessage) {
        loading = false;
        baseSeed = newBaseSeed;
        nextResetEpochSeconds = newNextResetEpochSeconds;
        challengeId = newChallengeId == null ? "" : newChallengeId;
        error = errorMessage == null ? "" : errorMessage;
        if (!error.isBlank()) {
            card = null;
            settingsLines = new ArrayList<>();
            cardSeed = "";
            worldSeed = "";
            settingsSeed = "";
        }
    }

    public static void setPreview(BingoCard newCard, List<String> newSettingsLines, String newCardSeed, String newWorldSeed, String newSettingsSeed, String newChallengeId, long newNextResetEpochSeconds, String errorMessage) {
        loading = false;
        card = newCard;
        settingsLines = newSettingsLines == null ? new ArrayList<>() : new ArrayList<>(newSettingsLines);
        cardSeed = newCardSeed == null ? "" : newCardSeed;
        worldSeed = newWorldSeed == null ? "" : newWorldSeed;
        settingsSeed = newSettingsSeed == null ? "" : newSettingsSeed;
        challengeId = newChallengeId == null ? "" : newChallengeId;
        nextResetEpochSeconds = newNextResetEpochSeconds;
        error = errorMessage == null ? "" : errorMessage;
    }

    public static boolean isLoading() {
        return loading;
    }

    public static long getBaseSeed() {
        return baseSeed;
    }

    public static long getNextResetEpochSeconds() {
        return nextResetEpochSeconds;
    }

    public static String getChallengeId() {
        return challengeId;
    }

    public static String getError() {
        return error;
    }

    public static BingoCard getCard() {
        return card;
    }

    public static List<String> getSettingsLines() {
        return Collections.unmodifiableList(settingsLines);
    }

    public static String getCardSeed() {
        return cardSeed;
    }

    public static String getWorldSeed() {
        return worldSeed;
    }

    public static String getSettingsSeed() {
        return settingsSeed;
    }
}