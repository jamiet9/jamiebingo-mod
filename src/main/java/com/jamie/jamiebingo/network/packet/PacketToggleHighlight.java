package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.data.BingoGameData;
import com.jamie.jamiebingo.data.BroadcastHelper;
import com.jamie.jamiebingo.data.TeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketToggleHighlight {

    private final String slotId;

    public PacketToggleHighlight(String slotId) {
        this.slotId = slotId == null ? "" : slotId;
    }

    public static void encode(PacketToggleHighlight msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.slotId);
    }

    public static PacketToggleHighlight decode(FriendlyByteBuf buf) {
        return new PacketToggleHighlight(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
    }

    public static void handle(PacketToggleHighlight msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            if (msg.slotId == null || msg.slotId.isBlank()) return;

            BingoGameData data = BingoGameData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
            if (!data.isActive() || data.getCurrentCard() == null) return;
            if (data.startCountdownActive) return;

            TeamData teamData = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
            UUID teamId = teamData.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            if (teamId == null) {
                teamId = com.jamie.jamiebingo.util.EntityUtil.getUUID(player);
            }

            if (!data.cardContainsForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), msg.slotId)) return;

            data.toggleHighlight(teamId, msg.slotId);
            BroadcastHelper.broadcastHighlightedSlots(com.jamie.jamiebingo.util.EntityServerUtil.getServer(player));
            com.jamie.jamiebingo.util.SavedDataUtil.markDirty(data);
        });
        context.setPacketHandled(true);
    }
}




