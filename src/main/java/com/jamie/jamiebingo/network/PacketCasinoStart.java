package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.network.ClientPacketInvoker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketCasinoStart {

    public final long endTimeMillis;
    public final long visualSeed;
    public final int gridSize;

    public PacketCasinoStart(long endTimeMillis, long visualSeed, int gridSize) {
        this.endTimeMillis = endTimeMillis;
        this.visualSeed = visualSeed;
        this.gridSize = gridSize;
    }

    public static void encode(PacketCasinoStart msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.endTimeMillis);
        buf.writeLong(msg.visualSeed);
        buf.writeInt(msg.gridSize);
    }

    public static PacketCasinoStart decode(FriendlyByteBuf buf) {
        return new PacketCasinoStart(
                buf.readLong(),
                buf.readLong(),
                buf.readInt()
        );
    }

    public static void handle(PacketCasinoStart msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketInvoker.invoke(
                        "handleCasinoStart",
                        new Class<?>[]{PacketCasinoStart.class},
                        msg
                )
        ));
        ctx.setPacketHandled(true);
    }
}
