package com.jamie.jamiebingo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class PacketSyncRevealedSlots {

    private final Set<String> revealed;

    public PacketSyncRevealedSlots(Set<String> revealed) {
        this.revealed = revealed;
    }

    public static void encode(PacketSyncRevealedSlots msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.revealed.size());
        for (String id : msg.revealed) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id);
        }
    }

    public static PacketSyncRevealedSlots decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Set<String> revealed = new HashSet<>();
        for (int i = 0; i < size; i++) {
            revealed.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        return new PacketSyncRevealedSlots(revealed);
    }

    public static void handle(PacketSyncRevealedSlots msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            com.jamie.jamiebingo.client.ClientRevealedSlots.set(msg.revealed);
        });
        ctx.setPacketHandled(true);
    }
}

