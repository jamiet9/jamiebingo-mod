package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.BingoRarityUtil;
import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PacketSetRarityOverridesChunk {
    private static final Map<UUID, Map<String, String>> PENDING = new ConcurrentHashMap<>();

    private final boolean clearExisting;
    private final boolean finalChunk;
    private final Map<String, String> overrides;

    public PacketSetRarityOverridesChunk(boolean clearExisting, boolean finalChunk, Map<String, String> overrides) {
        this.clearExisting = clearExisting;
        this.finalChunk = finalChunk;
        this.overrides = overrides == null ? Map.of() : new LinkedHashMap<>(overrides);
    }

    public static void encode(PacketSetRarityOverridesChunk msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.clearExisting);
        buf.writeBoolean(msg.finalChunk);
        buf.writeInt(msg.overrides.size());
        for (Map.Entry<String, String> entry : msg.overrides.entrySet()) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, entry.getKey() == null ? "" : entry.getKey());
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, BingoRarityUtil.normalize(entry.getValue()));
        }
    }

    public static PacketSetRarityOverridesChunk decode(FriendlyByteBuf buf) {
        boolean clearExisting = buf.readBoolean();
        boolean finalChunk = buf.readBoolean();
        int size = Math.max(0, buf.readInt());
        Map<String, String> overrides = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String id = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
            String rarity = BingoRarityUtil.normalize(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
            if (id != null && !id.isBlank() && BingoRarityUtil.isKnown(rarity)) {
                overrides.put(id, rarity);
            }
        }
        return new PacketSetRarityOverridesChunk(clearExisting, finalChunk, overrides);
    }

    public static void handle(PacketSetRarityOverridesChunk msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(sender);
            if (playerId == null) return;

            Map<String, String> accum;
            if (msg.clearExisting) {
                accum = new HashMap<>();
                PENDING.put(playerId, accum);
            } else {
                accum = PENDING.computeIfAbsent(playerId, id -> new HashMap<>());
            }
            accum.putAll(msg.overrides);

            if (!msg.finalChunk) return;

            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) {
                PENDING.remove(playerId);
                return;
            }
            BingoGameData data = BingoGameData.get(server);
            data.setPlayerRarityOverrides(playerId, accum);
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
            PENDING.remove(playerId);
        });
        ctx.setPacketHandled(true);
    }
}
