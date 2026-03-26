package com.jamie.jamiebingo.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketOpenRarityChanger {
    public static void encode(PacketOpenRarityChanger msg, FriendlyByteBuf buf) {
    }

    public static PacketOpenRarityChanger decode(FriendlyByteBuf buf) {
        return new PacketOpenRarityChanger();
    }

    public static void handle(PacketOpenRarityChanger msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(com.jamie.jamiebingo.client.ClientPacketHandlers::handleOpenRarityChanger);
        ctx.setPacketHandled(true);
    }
}
