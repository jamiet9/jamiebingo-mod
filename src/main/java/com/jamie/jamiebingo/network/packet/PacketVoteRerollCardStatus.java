package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.client.ClientVoteRerollCardState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketVoteRerollCardStatus {
    private final int votes;
    private final int total;
    private final boolean localVoted;

    public PacketVoteRerollCardStatus(int votes, int total, boolean localVoted) {
        this.votes = votes;
        this.total = total;
        this.localVoted = localVoted;
    }

    public static void encode(PacketVoteRerollCardStatus msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.votes);
        buf.writeInt(msg.total);
        buf.writeBoolean(msg.localVoted);
    }

    public static PacketVoteRerollCardStatus decode(FriendlyByteBuf buf) {
        return new PacketVoteRerollCardStatus(buf.readInt(), buf.readInt(), buf.readBoolean());
    }

    public static void handle(PacketVoteRerollCardStatus msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> ClientVoteRerollCardState.update(msg.votes, msg.total, msg.localVoted));
        context.setPacketHandled(true);
    }
}
