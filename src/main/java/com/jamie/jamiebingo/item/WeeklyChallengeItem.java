package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketOpenWeeklyChallenge;
import com.jamie.jamiebingo.world.PregameBoxManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class WeeklyChallengeItem extends Item {
    public WeeklyChallengeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(level)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(serverPlayer);
        if (server == null) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }

        BingoGameData data = BingoGameData.get(server);
        if (data.isActive()) {
            serverPlayer.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Weekly challenge can only be started from the lobby."));
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }
        if (!data.pregameBoxActive || !PregameBoxManager.isInsideBox(serverPlayer, data)) {
            serverPlayer.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Use this in the spawnbox."));
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }

        NetworkHandler.sendToPlayer(serverPlayer, new PacketOpenWeeklyChallenge());
        return com.jamie.jamiebingo.util.InteractionResultUtil.success();
    }
}
