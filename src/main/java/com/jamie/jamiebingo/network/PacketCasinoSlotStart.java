package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketCasinoSlotStart {

    public final int x;
    public final int y;

    public PacketCasinoSlotStart(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static void encode(PacketCasinoSlotStart msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
    }

    public static PacketCasinoSlotStart decode(FriendlyByteBuf buf) {
        return new PacketCasinoSlotStart(buf.readInt(), buf.readInt());
    }

    public static void handle(PacketCasinoSlotStart msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {

            // 🔒 HARD GUARD — never assume casino is active
            if (!ClientCasinoState.isActive()) return;

            ClientCasinoState.onSlotStart(msg.x, msg.y);
        });
        context.setPacketHandled(true);
    }
}
