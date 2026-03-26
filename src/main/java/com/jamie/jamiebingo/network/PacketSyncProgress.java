package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientProgressData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class PacketSyncProgress {

    private final Set<String> completed;

    public PacketSyncProgress(Set<String> completed) {
        this.completed = completed;
    }

    public static void encode(PacketSyncProgress msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.completed.size());
        for (String s : msg.completed) com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, s);
    }

    public static PacketSyncProgress decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < count; i++) set.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        return new PacketSyncProgress(set);
    }

    public static void handle(PacketSyncProgress msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> ClientProgressData.set(msg.completed));
        ctx.setPacketHandled(true);
    }
}

