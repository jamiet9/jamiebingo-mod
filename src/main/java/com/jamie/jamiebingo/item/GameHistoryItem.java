package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketOpenGameHistory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class GameHistoryItem extends Item {
    public GameHistoryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(level)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }
        if (!(player instanceof ServerPlayer sp)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }

        MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sp);
        if (server == null) return com.jamie.jamiebingo.util.InteractionResultUtil.success();

        BingoGameData data = BingoGameData.get(server);
        NetworkHandler.sendToPlayer(
                sp,
                PacketOpenGameHistory.fromEntries(data.getGameHistoryForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(sp)))
        );
        return com.jamie.jamiebingo.util.InteractionResultUtil.success();
    }
}

