package com.jamie.jamiebingo.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketSeedPasteFinish {

    public static void encode(PacketSeedPasteFinish msg, FriendlyByteBuf buf) {
    }

    public static PacketSeedPasteFinish decode(FriendlyByteBuf buf) {
        return new PacketSeedPasteFinish();
    }

    public static void handle(PacketSeedPasteFinish msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            PacketSeedPastePart.finishFor(player);
        });
        ctx.setPacketHandled(true);
    }
}
