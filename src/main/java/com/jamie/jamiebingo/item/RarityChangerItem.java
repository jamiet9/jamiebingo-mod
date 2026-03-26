package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketOpenRarityChanger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class RarityChangerItem extends Item {
    public RarityChangerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(level)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }
        if (player instanceof ServerPlayer sp) {
            NetworkHandler.sendToPlayer(sp, new PacketOpenRarityChanger());
        }
        return com.jamie.jamiebingo.util.InteractionResultUtil.success();
    }
}
