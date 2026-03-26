package com.jamie.jamiebingo.item;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.packet.PacketOpenWorldSettings;
import com.jamie.jamiebingo.world.PregameBoxManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class WorldSettingsItem extends Item {
    public WorldSettingsItem(Properties properties) {
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
        if (server == null) return com.jamie.jamiebingo.util.InteractionResultUtil.success();

        BingoGameData data = BingoGameData.get(server);
        if (!data.pregameBoxActive || !PregameBoxManager.isInsideBox(serverPlayer, data)) {
            serverPlayer.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Use this in the spawnbox."));
            return com.jamie.jamiebingo.util.InteractionResultUtil.success();
        }

        NetworkHandler.sendToPlayer(serverPlayer, new PacketOpenWorldSettings(
                data.worldUseNewSeedEachGame,
                data.worldTypeMode,
                data.worldCustomBiomeSizeBlocks,
                data.worldTerrainHillinessPercent,
                data.worldStructureFrequencyPercent,
                data.worldSingleBiomeId,
                data.worldSurfaceCaveBiomes,
                data.worldSetSeedText,
                data.adventureMode,
                data.prelitPortalsMode
        ));
        return com.jamie.jamiebingo.util.InteractionResultUtil.success();
    }
}
