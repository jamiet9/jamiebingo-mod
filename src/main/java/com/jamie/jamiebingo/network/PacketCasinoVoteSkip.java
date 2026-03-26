package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.casino.CasinoModeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Client → Server
 *
 * Sent when a player votes to skip the casino animation.
 * Server is authoritative.
 */
public class PacketCasinoVoteSkip {

    /* ===============================
       ENCODE / DECODE
       =============================== */

    public static void encode(PacketCasinoVoteSkip msg, FriendlyByteBuf buf) {
        // no payload
    }

    public static PacketCasinoVoteSkip decode(FriendlyByteBuf buf) {
        return new PacketCasinoVoteSkip();
    }

    /* ===============================
       HANDLE (SERVER)
       =============================== */

    public static void handle(PacketCasinoVoteSkip msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            if (server == null) return;

            CasinoModeManager.registerSkipVote(server, player);
        });

        context.setPacketHandled(true);
    }
}

