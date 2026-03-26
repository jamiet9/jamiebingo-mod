package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.bingo.BingoSlot;
import com.jamie.jamiebingo.bingo.SlotResolver;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.quest.QuestDefinition;

public final class ClientMineState {

    private static boolean active = false;
    private static final java.util.List<String> sourceQuestIds = new java.util.ArrayList<>();
    private static final java.util.Map<String, String> displayNameById = new java.util.HashMap<>();
    private static final java.util.Map<String, BingoSlot> slotById = new java.util.HashMap<>();
    private static String triggeredQuestId = "";
    private static int remainingSeconds = -1;
    private static String progressQuestId = "";
    private static int progress = 0;
    private static int progressMax = 0;
    private static String defuseQuestId = "";
    private static String defuseDisplayName = "";
    private static int lastClientTick = -1;
    private static int tickRemainder = 0;

    private ClientMineState() {
    }

    public static void clear() {
        active = false;
        sourceQuestIds.clear();
        displayNameById.clear();
        slotById.clear();
        triggeredQuestId = "";
        remainingSeconds = -1;
        progressQuestId = "";
        progress = 0;
        progressMax = 0;
        defuseQuestId = "";
        defuseDisplayName = "";
        lastClientTick = -1;
        tickRemainder = 0;
    }

    public static void set(
            boolean isActive,
            java.util.List<String> questIds,
            java.util.List<String> names,
            String triggeredId,
            int secondsLeft,
            String progressId,
            int currentProgress,
            int maxProgress,
            String defuseId,
            String defuseName
    ) {
        active = isActive;
        sourceQuestIds.clear();
        displayNameById.clear();
        slotById.clear();
        if (questIds != null) {
            sourceQuestIds.addAll(questIds);
        }
        if (names != null) {
            for (int i = 0; i < Math.min(sourceQuestIds.size(), names.size()); i++) {
                String id = sourceQuestIds.get(i);
                String name = names.get(i);
                if (id != null && !id.isBlank()) {
                    displayNameById.put(id, name == null ? id : name);
                }
            }
        }
        for (String id : sourceQuestIds) {
            if (id == null || id.isBlank()) continue;
            BingoSlot resolved = resolveSlotForId(id);
            slotById.put(id, resolved == null ? new BingoSlot(id, id, "mine", "rare") : resolved);
        }
        triggeredQuestId = triggeredId == null ? "" : triggeredId;
        remainingSeconds = secondsLeft;
        progressQuestId = progressId == null ? "" : progressId;
        progress = Math.max(0, currentProgress);
        progressMax = Math.max(0, maxProgress);
        defuseQuestId = defuseId == null ? "" : defuseId;
        defuseDisplayName = defuseName == null ? "" : defuseName;
        if (!defuseQuestId.isBlank()) {
            BingoSlot defuseSlot = resolveSlotForId(defuseQuestId);
            slotById.put(defuseQuestId, defuseSlot == null
                    ? new BingoSlot(defuseQuestId, defuseDisplayName.isBlank() ? defuseQuestId : defuseDisplayName, "mine", "rare")
                    : defuseSlot);
        }
        lastClientTick = -1;
        tickRemainder = 0;
    }

    public static boolean isActive() {
        return active;
    }

    public static java.util.List<String> sourceQuestIds() {
        return java.util.List.copyOf(sourceQuestIds);
    }

    public static String displayNameFor(String questId) {
        if (questId == null || questId.isBlank()) return "";
        return displayNameById.getOrDefault(questId, questId);
    }

    public static BingoSlot slotFor(String questId) {
        if (questId == null || questId.isBlank()) return null;
        return slotById.get(questId);
    }

    public static boolean isTriggered() {
        return triggeredQuestId != null && !triggeredQuestId.isBlank();
    }

    public static String triggeredQuestId() {
        return triggeredQuestId;
    }

    public static int remainingSeconds() {
        return remainingSeconds;
    }

    public static String progressQuestId() {
        return progressQuestId;
    }

    public static int progress() {
        return progress;
    }

    public static int progressMax() {
        return progressMax;
    }

    public static String defuseQuestId() {
        return defuseQuestId;
    }

    public static String defuseDisplayName() {
        return defuseDisplayName;
    }

    public static void tickVisual() {
        if (!active || remainingSeconds < 0) {
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
        remainingSeconds = Math.max(0, remainingSeconds - wholeSeconds);
    }

    private static BingoSlot resolveSlotForId(String id) {
        if (id == null || id.isBlank()) return null;
        BingoSlot resolved = SlotResolver.resolveSlot(id);
        if (resolved != null) return resolved;
        QuestDefinition q = QuestDatabase.getQuestById(id);
        if (q == null) return null;
        return new BingoSlot(
                id,
                q.name != null ? q.name : id,
                q.category != null ? q.category : "mine",
                q.rarity != null ? q.rarity : "rare"
        );
    }
}
