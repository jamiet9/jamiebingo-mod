package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server -> Client
 * Sync remaining rerolls for THIS player only.
 *
 * This packet is informational only.
 * Authoritative reroll state lives server-side in BingoGameData.
 */
public class PacketCasinoRerollCount {

    private final int remaining;

    public PacketCasinoRerollCount(int remaining) {
        this.remaining = remaining;
    }

    /* ===============================
       ENCODE / DECODE
       =============================== */

    public static void encode(PacketCasinoRerollCount msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.remaining);
    }

    public static PacketCasinoRerollCount decode(FriendlyByteBuf buf) {
        return new PacketCasinoRerollCount(buf.readInt());
    }

    /* ===============================
       HANDLE (CLIENT)
       =============================== */

    public static void handle(PacketCasinoRerollCount msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (!ClientCasinoState.isRerollPhase()) {
                ClientCasinoState.beginRerollPhase(msg.remaining, ClientCasinoState.isFakeRerollPhase());
            }
            ClientCasinoState.setRerollsRemaining(msg.remaining);
        });
        ctx.setPacketHandled(true);
    }
}
