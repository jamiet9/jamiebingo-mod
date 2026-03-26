package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.menu.TeamSelectMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class TeamSelectorItem extends Item {
    public TeamSelectorItem(Properties properties) {
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

        com.jamie.jamiebingo.util.MenuOpenUtil.open(
                serverPlayer,
                new SimpleMenuProvider(
                        (id, inv, p) -> new TeamSelectMenu(id, inv),
                        com.jamie.jamiebingo.util.ComponentUtil.literal("Teams")
                )
        );
        SoundEvent click = com.jamie.jamiebingo.util.SoundUtil.create(
                com.jamie.jamiebingo.util.IdUtil.id("minecraft:ui.button.click")
        );
        com.jamie.jamiebingo.util.SoundUtil.playToPlayer(serverPlayer, click, 0.6f, 1.0f);
        return com.jamie.jamiebingo.util.InteractionResultUtil.success();
    }
}
