package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.data.TeamData;
import com.jamie.jamiebingo.menu.TeamSelectMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;

public class PacketSetPreferredTeamColor {
    private final int colorId;

    public PacketSetPreferredTeamColor(int colorId) {
        this.colorId = colorId;
    }

    public static void encode(PacketSetPreferredTeamColor msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.colorId);
    }

    public static PacketSetPreferredTeamColor decode(FriendlyByteBuf buf) {
        return new PacketSetPreferredTeamColor(buf.readInt());
    }

    public static void handle(PacketSetPreferredTeamColor msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) return;

            if (msg.colorId < 0 || msg.colorId >= DyeColor.values().length) return;
            DyeColor color = DyeColor.byId(msg.colorId);
            UUID playerId = com.jamie.jamiebingo.util.EntityUtil.getUUID(sender);

            com.jamie.jamiebingo.util.NbtUtil.putInt(sender.getPersistentData(), TeamSelectMenu.PREF_TEAM_COLOR_TAG, color.getId());

            TeamData teamData = TeamData.get(server);
            teamData.setPreferredTeamColor(playerId, color);
            UUID preferredTeamId = teamData.getOrCreateTeamForColor(color);
            teamData.movePlayerToTeam(playerId, preferredTeamId);
        });
        ctx.setPacketHandled(true);
    }
}
