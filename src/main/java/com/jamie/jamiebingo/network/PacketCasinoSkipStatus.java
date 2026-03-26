package com.jamie.jamiebingo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Server -> Client live vote status for casino skip.
 */
public class PacketCasinoSkipStatus {

    private final int votes;
    private final int total;

    public PacketCasinoSkipStatus(int votes, int total) {
        this.votes = votes;
        this.total = total;
    }

    public static void encode(PacketCasinoSkipStatus msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.votes);
        buf.writeInt(msg.total);
    }

    public static PacketCasinoSkipStatus decode(FriendlyByteBuf buf) {
        return new PacketCasinoSkipStatus(buf.readInt(), buf.readInt());
    }

    public static void handle(PacketCasinoSkipStatus msg, CustomPayloadEvent.Context ctx) {
        if (ctx == null) return;
        ctx.enqueueWork(() ->
                com.jamie.jamiebingo.client.casino.ClientCasinoState.setSkipVoteStatus(msg.votes, msg.total));
        ctx.setPacketHandled(true);
    }
}
