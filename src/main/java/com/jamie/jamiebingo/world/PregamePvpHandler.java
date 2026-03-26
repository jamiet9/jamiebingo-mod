package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class PregamePvpHandler {

    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer target)) return;
        if (!(com.jamie.jamiebingo.util.DamageSourceUtil.getEntity(event.getSource()) instanceof ServerPlayer attacker)) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.pregameBoxActive) return;

        if (PregameBoxManager.isInsideBox(attacker, data)
                && PregameBoxManager.isInsideBox(target, data)) {
            event.setAmount(0.0f);
        }
    }
}




