package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientHighlightedSlots;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

import java.util.HashSet;
import java.util.Set;

public class PacketSyncHighlightedSlots {

    private final Set<String> slots;

    public PacketSyncHighlightedSlots(Set<String> slots) {
        this.slots = slots == null ? Set.of() : new HashSet<>(slots);
    }

    public static void encode(PacketSyncHighlightedSlots msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.slots.size());
        for (String id : msg.slots) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id);
        }
    }

    public static PacketSyncHighlightedSlots decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        Set<String> slots = new HashSet<>();
        for (int i = 0; i < count; i++) {
            slots.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        return new PacketSyncHighlightedSlots(slots);
    }

    public static void handle(PacketSyncHighlightedSlots msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientHighlightedSlots.set(msg.slots)
        ));
        ctx.setPacketHandled(true);
    }
}

