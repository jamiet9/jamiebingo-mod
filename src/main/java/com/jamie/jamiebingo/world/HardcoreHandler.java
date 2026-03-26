package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public class HardcoreHandler {

    public static void onPlayerDeath(LivingDeathEvent event) {

        // Only care about players
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);

        // Hardcore not enabled → do nothing
        if (!data.hardcoreEnabled || !data.isActive()) return;

        // Force spectator mode (vanilla hardcore behaviour)
    com.jamie.jamiebingo.util.GameModeTracking.runWithoutCommandTracking(player, () -> player.setGameMode(GameType.SPECTATOR));

        // Mark elimination + check team disqualification
        data.markPlayerEliminated(server, com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
    }
}

