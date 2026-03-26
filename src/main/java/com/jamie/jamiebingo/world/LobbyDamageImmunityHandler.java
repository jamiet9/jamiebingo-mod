package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.JamieBingo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class LobbyDamageImmunityHandler {
    private LobbyDamageImmunityHandler() {}

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isLobbyPlayer(player)) return;
        event.setAmount(0.0F);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isLobbyPlayer(player)) return;
        event.setAmount(0.0F);
    }

    private static boolean isLobbyPlayer(ServerPlayer player) {
        if (player == null) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;
        return level.dimension() == LobbyWorldManager.LOBBY_DIMENSION;
    }
}
