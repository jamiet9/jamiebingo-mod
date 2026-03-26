package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashSet;
import java.util.Set;

public class PacketToggleBlacklistId {

    private final String id;
    private final boolean add;

    public PacketToggleBlacklistId(String id, boolean add) {
        this.id = id == null ? "" : id;
        this.add = add;
    }

    public static void encode(PacketToggleBlacklistId msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.id);
        buf.writeBoolean(msg.add);
    }

    public static PacketToggleBlacklistId decode(FriendlyByteBuf buf) {
        String id = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
        boolean add = buf.readBoolean();
        return new PacketToggleBlacklistId(id, add);
    }

    public static void handle(PacketToggleBlacklistId msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) return;

            String id = msg.id == null ? "" : msg.id.trim();
            if (id.isBlank()) return;

            BingoGameData data = BingoGameData.get(server);
            java.util.UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(sender);
            Set<String> next = new HashSet<>(data.getPlayerBlacklistedSlotIds(playerId));
            if (msg.add) {
                next.add(id);
            } else {
                next.remove(id);
            }
            data.setPlayerBlacklistedSlotIds(playerId, next);
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        });
        ctx.setPacketHandled(true);
    }
}
