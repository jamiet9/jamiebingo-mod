package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;

import java.util.ArrayList;
import java.util.List;

public final class ClientCustomCardState {

    public static ControllerSettingsSnapshot settings;
    public static boolean customCardEnabled = false;
    public static boolean customPoolEnabled = false;
    public static final List<String> customCardSlots = new ArrayList<>();
    public static final List<String> customPoolIds = new ArrayList<>();
    public static final List<String> customMineIds = new ArrayList<>();

    private ClientCustomCardState() {
    }

    public static void setState(
            ControllerSettingsSnapshot snapshot,
            boolean cardEnabled,
            boolean poolEnabled,
            List<String> cardSlots,
            List<String> poolIds,
            List<String> mineIds
    ) {
        settings = snapshot;
        customCardEnabled = cardEnabled;
        customPoolEnabled = poolEnabled;
        customCardSlots.clear();
        if (cardSlots != null) {
            customCardSlots.addAll(cardSlots);
        }
        customPoolIds.clear();
        if (poolIds != null) {
            customPoolIds.addAll(poolIds);
        }
        customMineIds.clear();
        if (mineIds != null) {
            customMineIds.addAll(mineIds);
        }
    }

    public static void ensureCardSlots(int size) {
        while (customCardSlots.size() < size) {
            customCardSlots.add("");
        }
    }
}
