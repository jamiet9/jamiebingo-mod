package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.util.NbtUtil;
import com.jamie.jamiebingo.util.ServerTickUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class GameStartSpawnSafetyHandler {
    private GameStartSpawnSafetyHandler() {}

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;
        int now = ServerTickUtil.getTickCount(server);
        int safeUntil = NbtUtil.getInt(player.getPersistentData(), PregameBoxManager.GAME_START_SAFE_UNTIL_TICK, -1);
        if (safeUntil >= now) {
            // Temporary invulnerability for game-start handoff (fall, void, or delayed chunk/teleport damage).
            event.setAmount(0.0F);
            player.resetFallDistance();
        }
    }
}
