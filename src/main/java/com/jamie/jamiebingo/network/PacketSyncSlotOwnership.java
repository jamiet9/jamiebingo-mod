package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientSlotOwnership;
import com.jamie.jamiebingo.util.FriendlyByteBufUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class PacketSyncSlotOwnership {

    private final Map<String, DyeColor> ownership;

    public PacketSyncSlotOwnership(Map<String, DyeColor> ownership) {
        this.ownership = ownership;
    }

    public static void encode(PacketSyncSlotOwnership msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.ownership.size());
        for (var e : msg.ownership.entrySet()) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, e.getKey());
            FriendlyByteBufUtil.writeEnum(buf, e.getValue());
        }
    }

    public static PacketSyncSlotOwnership decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        Map<String, DyeColor> map = new HashMap<>();
        for (int i = 0; i < size; i++) {
            map.put(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767), FriendlyByteBufUtil.readEnum(buf, DyeColor.class));
        }
        return new PacketSyncSlotOwnership(map);
    }

    public static void handle(PacketSyncSlotOwnership msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ClientSlotOwnership.setAll(msg.ownership);
        });
        ctx.setPacketHandled(true);
    }
}

