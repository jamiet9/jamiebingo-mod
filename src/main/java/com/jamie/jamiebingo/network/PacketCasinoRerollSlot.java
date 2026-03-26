package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.casino.CasinoRerollManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client → Server
 * Request a casino-style reroll animation for a single slot.
 *
 * This packet carries intent only.
 * All validation is performed server-side in CasinoRerollManager.
 */
public class PacketCasinoRerollSlot {

    private final int x;
    private final int y;

    public PacketCasinoRerollSlot(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /* ===============================
       ENCODE / DECODE
       =============================== */

    public static void encode(PacketCasinoRerollSlot msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
    }

    public static PacketCasinoRerollSlot decode(FriendlyByteBuf buf) {
        return new PacketCasinoRerollSlot(
                buf.readInt(),
                buf.readInt()
        );
    }

    /* ===============================
       HANDLE (SERVER ONLY)
       =============================== */

    public static void handle(PacketCasinoRerollSlot msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;

        // 🔒 ABSOLUTE SIDE GUARD — REQUIRED
        if (!context.isServerSide()) {
            context.setPacketHandled(true);
            return;
        }

        context.enqueueWork(() -> {

            ServerPlayer player = context.getSender();
            if (player == null) return;

            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            if (server == null) return;

            CasinoRerollManager.requestReroll(
                    server,
                    player,
                    msg.x,
                    msg.y
            );
        });

        context.setPacketHandled(true);
    }
}

