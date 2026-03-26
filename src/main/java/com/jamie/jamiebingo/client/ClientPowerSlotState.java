package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.SlotResolver;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;

public final class ClientPowerSlotState {
    private static boolean active = false;
    private static String slotId = "";
    private static String displayName = "";
    private static int secondsRemaining = -1;
    private static BingoSlot slot = null;
    private static int lastClientTick = -1;
    private static int tickRemainder = 0;

    private ClientPowerSlotState() {
    }

    public static void clear() {
        active = false;
        slotId = "";
        displayName = "";
        secondsRemaining = -1;
        slot = null;
        lastClientTick = -1;
        tickRemainder = 0;
    }

    public static void set(boolean isActive, String id, String shownName, int remainingSeconds) {
        active = isActive;
        slotId = id == null ? "" : id;
        displayName = shownName == null ? "" : shownName;
        secondsRemaining = remainingSeconds;
        slot = resolveSlotForId(slotId, displayName);
        lastClientTick = -1;
        tickRemainder = 0;
    }

    public static boolean isActive() {
        return active && slot != null && slotId != null && !slotId.isBlank();
    }

    public static String slotId() {
        return slotId;
    }

    public static String displayName() {
        return displayName;
    }

    public static int remainingSeconds() {
        return secondsRemaining;
    }

    public static boolean isClaimed() {
        return secondsRemaining == -2;
    }

    public static BingoSlot slot() {
        return slot;
    }

    public static void tickVisual() {
        if (!isActive() || secondsRemaining < 0) {
            lastClientTick = -1;
            tickRemainder = 0;
            return;
        }
        var player = com.jamie.jamiebingo.client.ClientMinecraftUtil.getPlayer();
        if (player == null) {
            lastClientTick = -1;
            tickRemainder = 0;
            return;
        }
        int now = com.jamie.jamiebingo.util.EntityTickUtil.getTickCount(player);
        if (lastClientTick < 0) {
            lastClientTick = now;
            return;
        }
        int deltaTicks = now - lastClientTick;
        if (deltaTicks <= 0) return;
        lastClientTick = now;
        int total = tickRemainder + deltaTicks;
        int wholeSeconds = total / 20;
        tickRemainder = total % 20;
        if (wholeSeconds <= 0) return;
        secondsRemaining = Math.max(0, secondsRemaining - wholeSeconds);
    }

    private static BingoSlot resolveSlotForId(String id, String fallbackName) {
        if (id == null || id.isBlank()) return null;
        BingoSlot resolved = SlotResolver.resolveSlot(id);
        if (resolved != null) return resolved;
        QuestDefinition q = QuestDatabase.getQuestById(id);
        if (q != null) {
            return new BingoSlot(
                    id,
                    q.name != null && !q.name.isBlank() ? q.name : (fallbackName == null ? id : fallbackName),
                    q.category != null ? q.category : "power",
                    q.rarity != null ? q.rarity : "rare"
            );
        }
        return new BingoSlot(id, fallbackName == null || fallbackName.isBlank() ? id : fallbackName, "power", "rare");
    }
}
