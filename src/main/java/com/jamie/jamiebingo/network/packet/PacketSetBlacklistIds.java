package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.data.BingoGameData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

public class PacketSetBlacklistIds {

    private final List<String> blacklistIds;
    private final List<String> whitelistIds;

    public PacketSetBlacklistIds(List<String> blacklistIds) {
        this(blacklistIds, List.of());
    }

    public PacketSetBlacklistIds(List<String> blacklistIds, List<String> whitelistIds) {
        this.blacklistIds = blacklistIds == null ? List.of() : new ArrayList<>(blacklistIds);
        this.whitelistIds = whitelistIds == null ? List.of() : new ArrayList<>(whitelistIds);
    }

    public static void encode(PacketSetBlacklistIds msg, FriendlyByteBuf buf) {
        writeIds(buf, msg.blacklistIds);
        writeIds(buf, msg.whitelistIds);
    }

    public static PacketSetBlacklistIds decode(FriendlyByteBuf buf) {
        List<String> blacklistIds = readIds(buf);
        List<String> whitelistIds = readIds(buf);
        return new PacketSetBlacklistIds(blacklistIds, whitelistIds);
    }

    public static void handle(PacketSetBlacklistIds msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) return;

            BingoGameData data = BingoGameData.get(server);
            data.setPlayerSlotListSettings(
                    com.jamie.jamiebingo.util.EntityUtil.getUUID(sender),
                    msg.blacklistIds,
                    msg.whitelistIds
            );
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        });
        ctx.setPacketHandled(true);
    }

    private static void writeIds(FriendlyByteBuf buf, List<String> ids) {
        buf.writeInt(ids.size());
        for (String id : ids) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
        }
    }

    private static List<String> readIds(FriendlyByteBuf buf) {
        int size = Math.max(0, buf.readInt());
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            String id = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
            if (id != null && !id.isBlank()) {
                ids.add(id);
            }
        }
        return ids;
    }
}
