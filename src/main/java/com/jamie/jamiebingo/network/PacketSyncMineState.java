package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientMineState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketSyncMineState {

    public final boolean active;
    public final java.util.List<String> sourceQuestIds;
    public final java.util.List<String> displayNames;
    public final String triggeredQuestId;
    public final int remainingSeconds;
    public final String progressQuestId;
    public final int progress;
    public final int progressMax;
    public final String defuseQuestId;
    public final String defuseDisplayName;

    public PacketSyncMineState(
            boolean active,
            java.util.List<String> sourceQuestIds,
            java.util.List<String> displayNames,
            String triggeredQuestId,
            int remainingSeconds,
            String progressQuestId,
            int progress,
            int progressMax,
            String defuseQuestId,
            String defuseDisplayName
    ) {
        this.active = active;
        this.sourceQuestIds = sourceQuestIds == null ? java.util.List.of() : new java.util.ArrayList<>(sourceQuestIds);
        this.displayNames = displayNames == null ? java.util.List.of() : new java.util.ArrayList<>(displayNames);
        this.triggeredQuestId = triggeredQuestId == null ? "" : triggeredQuestId;
        this.remainingSeconds = remainingSeconds;
        this.progressQuestId = progressQuestId == null ? "" : progressQuestId;
        this.progress = progress;
        this.progressMax = progressMax;
        this.defuseQuestId = defuseQuestId == null ? "" : defuseQuestId;
        this.defuseDisplayName = defuseDisplayName == null ? "" : defuseDisplayName;
    }

    public static void encode(PacketSyncMineState msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeInt(msg.sourceQuestIds.size());
        for (String id : msg.sourceQuestIds) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, id == null ? "" : id);
        }
        buf.writeInt(msg.displayNames.size());
        for (String name : msg.displayNames) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, name == null ? "" : name);
        }
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.triggeredQuestId);
        buf.writeInt(msg.remainingSeconds);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.progressQuestId);
        buf.writeInt(msg.progress);
        buf.writeInt(msg.progressMax);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.defuseQuestId);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.defuseDisplayName);
    }

    public static PacketSyncMineState decode(FriendlyByteBuf buf) {
        boolean active = buf.readBoolean();
        int idCount = Math.max(0, Math.min(64, buf.readInt()));
        java.util.List<String> ids = new java.util.ArrayList<>();
        for (int i = 0; i < idCount; i++) {
            ids.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        int nameCount = Math.max(0, Math.min(64, buf.readInt()));
        java.util.List<String> names = new java.util.ArrayList<>();
        for (int i = 0; i < nameCount; i++) {
            names.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        return new PacketSyncMineState(
                active,
                ids,
                names,
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                buf.readInt(),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                buf.readInt(),
                buf.readInt(),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
        );
    }

    public static void handle(PacketSyncMineState msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (!msg.active) {
                ClientMineState.clear();
                return;
            }
            ClientMineState.set(
                    true,
                    msg.sourceQuestIds,
                    msg.displayNames,
                    msg.triggeredQuestId,
                    msg.remainingSeconds,
                    msg.progressQuestId,
                    msg.progress,
                    msg.progressMax,
                    msg.defuseQuestId,
                    msg.defuseDisplayName
            );
        });
        ctx.setPacketHandled(true);
    }
}
