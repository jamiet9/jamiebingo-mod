package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientQuestProgressData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PacketSyncQuestProgress {

    private final Map<String, Integer> progress;
    private final Map<String, Integer> max;

    public PacketSyncQuestProgress(Map<String, Integer> progress, Map<String, Integer> max) {
        this.progress = progress;
        this.max = max;
    }

    public static void encode(PacketSyncQuestProgress msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.progress.size());
        for (Map.Entry<String, Integer> entry : msg.progress.entrySet()) {
            String id = entry.getKey();
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id);
            buf.writeInt(entry.getValue());
            buf.writeInt(msg.max.getOrDefault(id, 0));
        }
    }

    public static PacketSyncQuestProgress decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Map<String, Integer> progress = new HashMap<>();
        Map<String, Integer> max = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String id = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
            int p = buf.readInt();
            int m = buf.readInt();
            progress.put(id, p);
            max.put(id, m);
        }
        return new PacketSyncQuestProgress(progress, max);
    }

    public static void handle(PacketSyncQuestProgress msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> ClientQuestProgressData.set(msg.progress, msg.max));
        ctx.setPacketHandled(true);
    }
}

