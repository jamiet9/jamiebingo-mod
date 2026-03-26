package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.client.ClientConnectionEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketClearClientState {

    public static void encode(PacketClearClientState msg, FriendlyByteBuf buf) {
    }

    public static PacketClearClientState decode(FriendlyByteBuf buf) {
        return new PacketClearClientState();
    }

    public static void handle(PacketClearClientState msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(ClientConnectionEvents::clearClientState);
        ctx.setPacketHandled(true);
    }
}
