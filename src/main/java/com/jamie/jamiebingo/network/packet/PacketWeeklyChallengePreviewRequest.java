package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.WeeklyChallengeManager;
import com.jamie.jamiebingo.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketWeeklyChallengePreviewRequest {
    private final long baseSeed;

    public PacketWeeklyChallengePreviewRequest(long baseSeed) {
        this.baseSeed = baseSeed;
    }

    public static void encode(PacketWeeklyChallengePreviewRequest msg, FriendlyByteBuf buf) {
        buf.writeLong(msg.baseSeed);
    }

    public static PacketWeeklyChallengePreviewRequest decode(FriendlyByteBuf buf) {
        return new PacketWeeklyChallengePreviewRequest(buf.readLong());
    }

    public static void handle(PacketWeeklyChallengePreviewRequest msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            if (server == null) return;
            WeeklyChallengeManager.WeeklyChallenge weekly = WeeklyChallengeManager.build(server, msg.baseSeed);
            NetworkHandler.sendToPlayer(player, PacketWeeklyChallengePreviewResponse.from(weekly));
        });
        ctx.setPacketHandled(true);
    }
}
