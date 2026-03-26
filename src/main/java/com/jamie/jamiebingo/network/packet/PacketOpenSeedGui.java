package com.jamie.jamiebingo.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketOpenSeedGui {

    public static void encode(PacketOpenSeedGui msg, FriendlyByteBuf buf) {
    }

    public static PacketOpenSeedGui decode(FriendlyByteBuf buf) {
        return new PacketOpenSeedGui();
    }

    public static void handle(PacketOpenSeedGui msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> PacketOpenSeedGui::openClient
        ));
        ctx.setPacketHandled(true);
    }

    private static void openClient() {
        com.jamie.jamiebingo.client.ClientSeedGuiOpener.open();
    }
}
