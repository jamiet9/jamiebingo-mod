package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.network.ClientPacketInvoker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketStartCountdown {

    private final int seconds;

    public PacketStartCountdown(int seconds) {
        this.seconds = seconds;
    }

    public static void encode(PacketStartCountdown msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.seconds);
    }

    public static PacketStartCountdown decode(FriendlyByteBuf buf) {
        return new PacketStartCountdown(buf.readInt());
    }

    public static void handle(PacketStartCountdown msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketInvoker.invoke(
                        "handleStartCountdown",
                        new Class<?>[]{int.class},
                        msg.seconds
                )
        ));
        ctx.setPacketHandled(true);
    }
}
