package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketOpenCardLayoutConfigurator;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CardLayoutConfiguratorItem extends Item {
    private static final Logger LOGGER = LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID);

    public CardLayoutConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        if (com.jamie.jamiebingo.util.LevelSideUtil.isClientSide(level)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }

        if (!(player instanceof ServerPlayer sp)) {
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }
        NetworkHandler.sendToPlayer(
                sp,
                new PacketOpenCardLayoutConfigurator()
        );

        return com.jamie.jamiebingo.util.InteractionResultUtil.success();
    }
}



