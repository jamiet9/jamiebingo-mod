package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Server → Client
 * Sent when a reroll request is rejected for ANY reason.
 *
 * This packet exists purely to keep the client from freezing
 * or crashing due to missing follow-up animation packets.
 */
public class PacketCasinoRerollReject {

    private final int x;
    private final int y;

    public PacketCasinoRerollReject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /* ===============================
       ENCODE / DECODE
       =============================== */

    public static void encode(PacketCasinoRerollReject msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
    }

    public static PacketCasinoRerollReject decode(FriendlyByteBuf buf) {
        return new PacketCasinoRerollReject(
                buf.readInt(),
                buf.readInt()
        );
    }

    /* ===============================
       HANDLE (CLIENT)
       =============================== */

    public static void handle(PacketCasinoRerollReject msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {

            // HARD SAFETY — never assume state
            if (!ClientCasinoState.isActive()) return;

            // Cancel any rolling animation immediately
            ClientCasinoState.cancelRollingSlot(msg.x, msg.y);
        });
        context.setPacketHandled(true);
    }
}
