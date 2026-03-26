package com.jamie.jamiebingo.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GameModeTracking {
    private static final Set<UUID> SUPPRESSED_PLAYERS = ConcurrentHashMap.newKeySet();

    private GameModeTracking() {
    }

    public static void runWithoutCommandTracking(ServerPlayer player, Runnable action) {
        if (player == null || action == null) return;
        UUID playerId = EntityUtil.getUUID(player);
        if (playerId == null) {
            action.run();
            return;
        }
        SUPPRESSED_PLAYERS.add(playerId);
        try {
            action.run();
        } finally {
            SUPPRESSED_PLAYERS.remove(playerId);
        }
    }

    public static boolean isSuppressed(ServerPlayer player) {
        if (player == null) return false;
        UUID playerId = EntityUtil.getUUID(player);
        return playerId != null && SUPPRESSED_PLAYERS.contains(playerId);
    }
}
