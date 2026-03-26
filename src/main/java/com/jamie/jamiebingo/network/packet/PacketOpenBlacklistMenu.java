package com.jamie.jamiebingo.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class PacketOpenBlacklistMenu {

    public PacketOpenBlacklistMenu() {
    }

    public static void encode(PacketOpenBlacklistMenu msg, FriendlyByteBuf buf) {
    }

    public static PacketOpenBlacklistMenu decode(FriendlyByteBuf buf) {
        return new PacketOpenBlacklistMenu();
    }

    public static void handle(PacketOpenBlacklistMenu msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> com.jamie.jamiebingo.client.ClientPacketHandlers.handleOpenBlacklistMenu()
        ));
        ctx.setPacketHandled(true);
    }
}
