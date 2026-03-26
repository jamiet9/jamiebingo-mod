package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.network.ClientPacketInvoker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Server to client.
 * Signals that all casino phases are complete.
 */
public class PacketCasinoShutdown {

    public static void encode(PacketCasinoShutdown msg, FriendlyByteBuf buf) {
    }

    public static PacketCasinoShutdown decode(FriendlyByteBuf buf) {
        return new PacketCasinoShutdown();
    }

    public static void handle(PacketCasinoShutdown msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketInvoker.invoke(
                        "handleCasinoShutdown",
                        new Class<?>[]{PacketCasinoShutdown.class},
                        msg
                )
        ));
        ctx.setPacketHandled(true);
    }
}
