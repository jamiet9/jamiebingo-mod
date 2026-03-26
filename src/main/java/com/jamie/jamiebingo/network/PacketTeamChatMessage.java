package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.data.TeamData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class PacketTeamChatMessage {

    private final String message;

    public PacketTeamChatMessage(String message) {
        this.message = message;
    }

    public static void encode(PacketTeamChatMessage msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.message);
    }

    public static PacketTeamChatMessage decode(FriendlyByteBuf buf) {
        return new PacketTeamChatMessage(
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
        );
    }

    public static void handle(PacketTeamChatMessage msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            org.apache.logging.log4j.LogManager.getLogger(com.jamie.jamiebingo.JamieBingo.MOD_ID)
                    .info("[JamieBingo] PacketTeamChatMessage received from {}: {}", sender.getGameProfile().name(), msg.message);

            TeamData data = TeamData.get(com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender));
            UUID teamId = data.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(sender));
            if (teamId == null) return;

            Component formatted = com.jamie.jamiebingo.util.ComponentUtil.literal(
                    "[Team] " + sender.getGameProfile().name() + ": " + msg.message
            );

            var server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            for (ServerPlayer player : com.jamie.jamiebingo.util.ServerPlayerListUtil.getPlayers(server)) {
                if (teamId.equals(data.getTeamForPlayer(com.jamie.jamiebingo.util.EntityUtil.getUUID(player)))) {
                    player.sendSystemMessage(formatted);
                }
            }
        });

        ctx.setPacketHandled(true);
    }
}



