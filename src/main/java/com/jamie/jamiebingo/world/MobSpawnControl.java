package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.util.GameRulesUtil;
import com.jamie.jamiebingo.util.ServerLevelUtil;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class MobSpawnControl {

    @SubscribeEvent
    public static void onCheckSpawn(MobSpawnEvent.PositionCheck event) {
        Mob mob = event.getEntity();

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);

        // Respect vanilla mob spawning rule for all mobs.
        var overworld = ServerLevelUtil.getOverworld(server);
        if (overworld != null) {
            var rules = GameRulesUtil.getGameRules(overworld);
            if (!GameRulesUtil.getBooleanByName(rules, true, "doMobSpawning", "spawnMobs", "spawn_mobs")) {
            event.setResult(Result.DENY);
            return;
            }
        }

        // While pregame box is active, block hostile mob spawning.
        if (data.pregameBoxActive && mob.getType().getCategory() == MobCategory.MONSTER) {
            event.setResult(Result.DENY);
            return;
        }

        // Outside pregame, block hostile mobs when disabled by settings.
        if (!data.hostileMobsEnabled && mob.getType().getCategory() == MobCategory.MONSTER) {
            event.setResult(Result.DENY);
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(event.getLevel())) return;
        if (!(event.getEntity() instanceof Mob mob)) return;

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (data.pregameBoxActive && mob.getType().getCategory() == MobCategory.MONSTER) {
            event.getEntity().discard();
            return;
        }
        if (!data.hostileMobsEnabled && mob.getType().getCategory() == MobCategory.MONSTER) {
            event.getEntity().discard();
        }
    }
}





