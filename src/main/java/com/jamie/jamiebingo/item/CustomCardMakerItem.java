package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketOpenCustomCardMaker;
import com.jamie.jamiebingo.bingo.CardComposition;
import com.jamie.jamiebingo.bingo.WinCondition;
import com.jamie.jamiebingo.casino.CasinoModeManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class CustomCardMakerItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);
    public CustomCardMakerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        // Client: allow animation, but DO NOT block server use packet
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(level)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }
        if (!(player instanceof ServerPlayer sp)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }
        var server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sp);
        if (server == null) {
            try {
                server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
            } catch (Throwable ignored) {
            }
        }
        if (server == null) {
            LOGGER.warn("[JamieBingo] CustomCardMakerItem.use server==null (level={}, isClientSide={})",
                    level == null ? "null" : level.getClass().getSimpleName(),
                    com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(level));
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }
        BingoGameData data = BingoGameData.get(server);

        WinCondition win = data.winCondition;
        CardComposition composition = data.composition;

        boolean casino = CasinoModeManager.isCasinoEnabled();

        List<String> cardSlots = new ArrayList<>(data.customCardSlots);
        List<String> poolIds = new ArrayList<>(data.customPoolIds);
        List<String> mineIds = new ArrayList<>(data.customMineIds);

        String worldDifficulty = "normal";
        try {
            var lvl = com.jamie.jamiebingo.util.EntityLevelUtil.getLevel(sp);
            if (lvl != null) {
                worldDifficulty = lvl.getDifficulty().getKey();
            }
        } catch (Throwable ignored) {
        }
        int effectsInterval = data.randomEffectsArmed ? data.randomEffectsIntervalSeconds : 0;
        PacketOpenCustomCardMaker packet = new PacketOpenCustomCardMaker(
                win,
                composition,
                data.questPercent,
                data.categoryLogicEnabled,
                data.rarityLogicEnabled,
                data.itemColorVariantsSeparate,
                casino,
                data.casinoMode,
                data.rerollsPerPlayer > 0 ? 1 : 0,
                data.rerollsPerPlayer,
                data.gunGameLength,
                data.hangmanRounds,
                data.hangmanBaseSeconds,
                data.hangmanPenaltySeconds,
                data.difficulty,
                false,
                worldDifficulty,
                false,
                effectsInterval,
                false,
                data.rtpEnabled,
                data.randomRtpIntent,
                data.hostileMobsEnabled,
                data.randomHostileMobsIntent,
                data.hungerEnabled,
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
                data.teamSyncEnabled,
                data.randomShuffleIntent ? BingoGameData.SHUFFLE_RANDOM
                        : (data.shuffleEnabled ? BingoGameData.SHUFFLE_ENABLED : BingoGameData.SHUFFLE_DISABLED),
                data.minesEnabled,
                data.mineAmount,
                data.mineTimeSeconds,
                data.customCardEnabled,
                data.customPoolEnabled,
                cardSlots,
                poolIds,
                mineIds
        );

        NetworkHandler.sendToPlayer(sp, packet);
        return com.jamie.jamiebingo.util.InteractionResultUtil.success();
    }
}


