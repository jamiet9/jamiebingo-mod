package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.casino.CasinoModeManager;
import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import com.jamie.jamiebingo.menu.PlayerControllerSettingsStore;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketOpenBingoController;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BingoControllerItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);

    public BingoControllerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(
            Level level,
            Player player,
            InteractionHand hand
    ) {
        // Client: allow animation, but DO NOT block server use packet
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(level)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(serverPlayer);
        if (server == null) {
            try {
                server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            } catch (Throwable ignored) {
            }
        }
        if (server == null) {
            LOGGER.warn("[JamieBingo] BingoControllerItem.use server==null (level={}, isClientSide={})",
                    level == null ? "null" : level.getClass().getSimpleName(),
                    com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(level));
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }

        BingoGameData data = BingoGameData.get(server);

        String worldDifficulty = "normal";
        try {
            worldDifficulty = level.getDifficulty().getKey();
        } catch (Throwable ignored) {
        }
        int effectsInterval = data.randomEffectsArmed ? data.randomEffectsIntervalSeconds : 0;
        ControllerSettingsSnapshot playerSnapshot = data.isActive()
                ? PlayerControllerSettingsStore.load(serverPlayer)
                : null;
        PacketOpenBingoController packet;
        if (playerSnapshot != null) {
            boolean randomCardDifficulty = "random".equalsIgnoreCase(playerSnapshot.cardDifficulty());
            boolean randomGameDifficulty = "random".equalsIgnoreCase(playerSnapshot.gameDifficulty());
            boolean randomEffectsInterval = playerSnapshot.effectsInterval() < 0;
            packet = new PacketOpenBingoController(
                    playerSnapshot.win(),
                    playerSnapshot.questMode() == 1 ? com.jamie.jamiebingo.bingo.CardComposition.HYBRID_CATEGORY
                            : playerSnapshot.questMode() == 2 ? com.jamie.jamiebingo.bingo.CardComposition.HYBRID_PERCENT
                            : com.jamie.jamiebingo.bingo.CardComposition.CLASSIC_ONLY,
                    playerSnapshot.questPercent(),
                    playerSnapshot.categoryLogicEnabled(),
                    playerSnapshot.rarityLogicEnabled(),
                    playerSnapshot.itemColorVariantsSeparate(),
                    playerSnapshot.casino(),
                    playerSnapshot.casinoMode(),
                    playerSnapshot.rerollsMode(),
                    playerSnapshot.rerollsCount(),
                    playerSnapshot.gunRounds(),
                    playerSnapshot.hangmanRounds(),
                    playerSnapshot.hangmanBaseSeconds(),
                    playerSnapshot.hangmanPenaltySeconds(),
                    randomCardDifficulty ? "normal" : playerSnapshot.cardDifficulty(),
                    randomCardDifficulty,
                    randomGameDifficulty ? worldDifficulty : playerSnapshot.gameDifficulty(),
                    randomGameDifficulty,
                    randomEffectsInterval ? 0 : Math.max(0, playerSnapshot.effectsInterval()),
                    randomEffectsInterval,
                    playerSnapshot.rtpEnabled(),
                    playerSnapshot.randomRtp(),
                    playerSnapshot.hostileMobsEnabled(),
                    playerSnapshot.randomHostileMobs(),
                    playerSnapshot.hungerEnabled(),
                    playerSnapshot.naturalRegenEnabled(),
                    playerSnapshot.randomNaturalRegen(),
                    playerSnapshot.randomHunger(),
                    playerSnapshot.cardSize(),
                    playerSnapshot.randomCardSize(),
                    playerSnapshot.keepInventoryEnabled(),
                    playerSnapshot.randomKeepInventory(),
                    playerSnapshot.hardcoreEnabled(),
                    playerSnapshot.randomHardcore(),
                    playerSnapshot.daylightMode(),
                    playerSnapshot.randomDaylight(),
                    playerSnapshot.startDelaySeconds(),
                    playerSnapshot.countdownEnabled(),
                    playerSnapshot.countdownMinutes(),
                    playerSnapshot.rushEnabled(),
                    playerSnapshot.rushSeconds(),
                    playerSnapshot.allowLateJoin(),
                    playerSnapshot.pvpEnabled(),
                    playerSnapshot.adventureMode(),
                    playerSnapshot.prelitPortalsMode(),
                    playerSnapshot.randomPvp(),
                    playerSnapshot.registerMode(),
                    playerSnapshot.randomRegister(),
                    playerSnapshot.teamSyncEnabled(),
                    playerSnapshot.teamChestEnabled(),
                    playerSnapshot.shuffleMode(),
                    playerSnapshot.starterKitMode(),
                    playerSnapshot.hideGoalDetailsInChat(),
                    playerSnapshot.minesEnabled(),
                    playerSnapshot.mineAmount(),
                    playerSnapshot.mineTimeSeconds(),
                    playerSnapshot.powerSlotEnabled(),
                    playerSnapshot.powerSlotIntervalSeconds()
            );
        } else {
            packet = new PacketOpenBingoController(
                    data.winCondition,
                    data.composition,
                    data.questPercent,
                    data.categoryLogicEnabled,
                    data.rarityLogicEnabled,
                    data.itemColorVariantsSeparate,
                    CasinoModeManager.isCasinoEnabled(),
                    data.casinoMode,
                    data.randomRerollsIntent ? 2 : (data.rerollsPerPlayer > 0 ? 1 : 0),
                    data.rerollsPerPlayer,
                    data.gunGameLength,
                    data.hangmanRounds,
                    data.hangmanBaseSeconds,
                    data.hangmanPenaltySeconds,
                    data.difficulty,
                    data.randomDifficultyIntent,
                    worldDifficulty,
                    false,
                    effectsInterval,
                    data.randomEffectsIntervalIntent,
                    data.rtpEnabled,
                    data.randomRtpIntent,
                    data.hostileMobsEnabled,
                    data.randomHostileMobsIntent,
                    data.hungerEnabled,
                    data.naturalRegenEnabled,
                    false,
                    data.randomHungerIntent,
                    data.size,
                    data.randomSizeIntent,
                    data.keepInventoryEnabled,
                    data.randomKeepInventoryIntent,
                    data.hardcoreEnabled,
                    data.randomHardcoreIntent,
                    data.daylightMode,
                    data.randomDaylightIntent,
                    data.bingoStartDelaySeconds,
                    data.countdownEnabled,
                    data.countdownMinutes,
                    data.rushEnabled,
                    data.rushSeconds,
                    data.allowLateJoin,
                    data.pvpEnabled,
                    data.adventureMode,
                    data.prelitPortalsMode,
                    data.randomPvpIntent,
                    data.registerMode,
                    false,
                    data.teamSyncEnabled,
                    data.teamChestEnabled,
                    data.randomShuffleIntent ? BingoGameData.SHUFFLE_RANDOM
                            : (data.shuffleEnabled ? BingoGameData.SHUFFLE_ENABLED : BingoGameData.SHUFFLE_DISABLED),
                    data.starterKitMode,
                    data.hideGoalDetailsInChat,
                    data.minesEnabled,
                    data.mineAmount,
                    data.mineTimeSeconds,
                    data.powerSlotEnabled,
                    data.powerSlotIntervalSeconds
            );
        }
        // Send to client; screen opens there.
        NetworkHandler.sendToPlayer(serverPlayer, packet);

        return com.jamie.jamiebingo.util.InteractionResultUtil.success();
    }
}




