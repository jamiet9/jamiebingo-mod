package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.client.ClientVoteEndGameState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketVoteEndGameStatus {
    private final int votes;
    private final int total;
    private final boolean localVoted;

    public PacketVoteEndGameStatus(int votes, int total, boolean localVoted) {
        this.votes = votes;
        this.total = total;
        this.localVoted = localVoted;
    }

    public static void encode(PacketVoteEndGameStatus msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.votes);
        buf.writeInt(msg.total);
        buf.writeBoolean(msg.localVoted);
    }

    public static PacketVoteEndGameStatus decode(FriendlyByteBuf buf) {
        return new PacketVoteEndGameStatus(buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(PacketVoteEndGameStatus msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> ClientVoteEndGameState.update(msg.votes, msg.total, msg.localVoted));
        context.setPacketHandled(true);
    }
}

