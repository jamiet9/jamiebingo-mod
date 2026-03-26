package com.jamie.jamiebingo.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketOpenCardLayoutConfigurator {

    public PacketOpenCardLayoutConfigurator() {
    }

    public static void encode(PacketOpenCardLayoutConfigurator msg, FriendlyByteBuf buf) {
    }

    public static PacketOpenCardLayoutConfigurator decode(FriendlyByteBuf buf) {
        return new PacketOpenCardLayoutConfigurator();
    }

    public static void handle(PacketOpenCardLayoutConfigurator msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> com.jamie.jamiebingo.client.ClientPacketHandlers.handleOpenCardLayoutConfigurator()));
        ctx.setPacketHandled(true);
    }
}
