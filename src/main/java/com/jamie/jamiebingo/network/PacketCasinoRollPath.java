package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketCasinoRollPath {

    public final int x;
    public final int y;
    public final String value;

    public PacketCasinoRollPath(int x, int y, String value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public static void encode(PacketCasinoRollPath msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.value);
    }

    public static PacketCasinoRollPath decode(FriendlyByteBuf buf) {
        return new PacketCasinoRollPath(
                buf.readInt(),
                buf.readInt(),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
        );
    }

    public static void handle(PacketCasinoRollPath msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {

            if (!ClientCasinoState.isActive()) return;

            ClientCasinoState.onPathRolled(msg.x, msg.y, msg.value);
        });
        context.setPacketHandled(true);
    }
}

