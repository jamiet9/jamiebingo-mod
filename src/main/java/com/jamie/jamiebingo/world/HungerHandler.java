package com.jamie.jamiebingo.world;

import com.jamie.jamiebingo.JamieBingo;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID)
public final class HungerHandler {

    /**
     * Cancel hunger entirely when disabled
     */
    @SubscribeEvent
    public static void onHungerTick(TickEvent.PlayerTickEvent.Post event) {

        if (!(event.player() instanceof ServerPlayer player)) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);

        net.minecraft.world.food.FoodData food = com.jamie.jamiebingo.util.PlayerFoodUtil.getFoodData(player);
        if (food == null) return;

        // Pregame box -> no hunger drain while waiting to start
        if (PregameBoxManager.isInsideBox(player, data)) {
            com.jamie.jamiebingo.util.FoodDataUtil.setFoodLevel(food, 20);
            com.jamie.jamiebingo.util.FoodDataUtil.setSaturation(food, 5.0F);
            return;
        }

        if (!data.isActive()) return;

        if (!data.hungerEnabled) {
            com.jamie.jamiebingo.util.FoodDataUtil.setFoodLevel(food, 20);
            com.jamie.jamiebingo.util.FoodDataUtil.setSaturation(food, 0.0F);
            return;
        }

        // Hunger enabled in PEACEFUL -> manually apply normal drain
        net.minecraft.world.level.Level level = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(player);
        if (level != null && com.jamie.jamiebingo.util.LevelDifficultyUtil.getDifficulty(level) == Difficulty.PEACEFUL) {

            // Only drain if player is moving / alive
            if (!player.isAlive()) return;

            // Simulate NORMAL difficulty exhaustion
            com.jamie.jamiebingo.util.FoodDataUtil.addExhaustion(food, 0.005F);
        }
    }

    /**
     * Prevent vanilla peaceful from refilling hunger
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {

        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        BingoGameData data = BingoGameData.get(server);
        if (!data.isActive()) return;

        if (!data.hungerEnabled) {
            net.minecraft.world.food.FoodData food = com.jamie.jamiebingo.util.PlayerFoodUtil.getFoodData(newPlayer);
            if (food != null) {
                com.jamie.jamiebingo.util.FoodDataUtil.setFoodLevel(food, 20);
                com.jamie.jamiebingo.util.FoodDataUtil.setSaturation(food, 0.0F);
            }
        }
    }
}
