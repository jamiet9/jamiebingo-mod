package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class MobAggroControl {

    private MobAggroControl() {
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getNewTarget() instanceof ServerPlayer)) return;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(mob);
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data.hostileMobsEnabled) return;

        event.setNewTarget(null);
        mob.setTarget(null);
        clearNeutralAggro(mob);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(com.jamie.jamiebingo.util.DamageSourceUtil.getEntity(event.getSource()) instanceof Mob mob)) return;

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data.hostileMobsEnabled) return;

        event.setAmount(0.0f);
        mob.setTarget(null);
        clearNeutralAggro(mob);
    }

    private static void clearNeutralAggro(Mob mob) {
        if (mob instanceof NeutralMob neutral) {
            neutral.setPersistentAngerEndTime(0L);
            neutral.setPersistentAngerTarget(null);
        }
    }
}





