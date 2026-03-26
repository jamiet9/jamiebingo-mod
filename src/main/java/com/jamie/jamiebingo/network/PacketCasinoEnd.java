package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.network.ClientPacketInvoker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Server to client.
 * Signals that casino generation is complete.
 */
public class PacketCasinoEnd {

    public static void encode(PacketCasinoEnd msg, FriendlyByteBuf buf) {
    }

    public static PacketCasinoEnd decode(FriendlyByteBuf buf) {
        return new PacketCasinoEnd();
    }

    public static void handle(PacketCasinoEnd msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketInvoker.invoke(
                        "handleCasinoEnd",
                        new Class<?>[]{PacketCasinoEnd.class},
                        msg
                )
        ));
        ctx.setPacketHandled(true);
    }
}
