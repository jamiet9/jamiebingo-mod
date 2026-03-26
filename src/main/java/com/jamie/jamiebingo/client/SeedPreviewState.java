package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.bingo.BingoCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SeedPreviewState {

    private static BingoCard card;
    private static List<String> settings = new ArrayList<>();
    private static String error = "";
    private static boolean hidePreviewSlots = false;

    private SeedPreviewState() {
    }

    public static void update(BingoCard newCard, List<String> newSettings, String errorMessage, boolean hideSlots) {
        card = newCard;
        settings = newSettings == null ? new ArrayList<>() : new ArrayList<>(newSettings);
        error = errorMessage == null ? "" : errorMessage;
        hidePreviewSlots = hideSlots;
    }

    public static void clear() {
        card = null;
        settings = new ArrayList<>();
        error = "";
        hidePreviewSlots = false;
    }

    public static boolean hasPreview() {
        return card != null || !settings.isEmpty() || !error.isBlank();
    }

    public static BingoCard getCard() {
        return card;
    }

    public static List<String> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public static String getError() {
        return error == null ? "" : error;
    }

    public static boolean shouldHidePreviewSlots() {
        return hidePreviewSlots;
    }
}
