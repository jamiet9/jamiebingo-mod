package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.menu.TeamChestMenuProvider;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketOpenTeamChest {

    public PacketOpenTeamChest() {
    }

    public static void encode(PacketOpenTeamChest msg, FriendlyByteBuf buf) {
        // no data
    }

    public static PacketOpenTeamChest decode(FriendlyByteBuf buf) {
        return new PacketOpenTeamChest();
    }

    public static void handle(PacketOpenTeamChest msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                var server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
                if (server == null) return;
                if (!BingoGameData.get(server).teamChestEnabled) {
                    return;
                }
                player.openMenu(
                        TeamChestMenuProvider.create(player)
                );
            }
        });
        ctx.setPacketHandled(true);
    }
}
