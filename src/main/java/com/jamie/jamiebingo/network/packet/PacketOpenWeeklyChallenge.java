package com.jamie.jamiebingo.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class PacketOpenWeeklyChallenge {
    public static void encode(PacketOpenWeeklyChallenge msg, FriendlyByteBuf buf) {
    }

    public static PacketOpenWeeklyChallenge decode(FriendlyByteBuf buf) {
        return new PacketOpenWeeklyChallenge();
    }

    public static void handle(PacketOpenWeeklyChallenge msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> com.jamie.jamiebingo.client.ClientPacketHandlers.handleOpenWeeklyChallenge()
        ));
        ctx.setPacketHandled(true);
    }
}
