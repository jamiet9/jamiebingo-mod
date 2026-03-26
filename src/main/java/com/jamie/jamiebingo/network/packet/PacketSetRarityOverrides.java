package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.BingoRarityUtil;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashMap;
import java.util.Map;

public class PacketSetRarityOverrides {
    private final Map<String, String> overrides;

    public PacketSetRarityOverrides(Map<String, String> overrides) {
        this.overrides = overrides == null ? Map.of() : new HashMap<>(overrides);
    }

    public static void encode(PacketSetRarityOverrides msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.overrides.size());
        for (Map.Entry<String, String> entry : msg.overrides.entrySet()) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, entry.getKey() == null ? "" : entry.getKey());
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, BingoRarityUtil.normalize(entry.getValue()));
        }
    }

    public static PacketSetRarityOverrides decode(FriendlyByteBuf buf) {
        int size = Math.max(0, buf.readInt());
        Map<String, String> overrides = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String id = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
            String rarity = BingoRarityUtil.normalize(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
            if (id != null && !id.isBlank() && BingoRarityUtil.isKnown(rarity)) {
                overrides.put(id, rarity);
            }
        }
        return new PacketSetRarityOverrides(overrides);
    }

    public static void handle(PacketSetRarityOverrides msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) return;
            BingoGameData data = BingoGameData.get(server);
            data.setPlayerRarityOverrides(com.jamie.jamiebingo.util.EntityUtil.getUUID(sender), msg.overrides);
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        });
        ctx.setPacketHandled(true);
    }
}
