package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Server -> Client
 * Explicitly tells the client to enter reroll phase.
 */
public class PacketCasinoEnterRerollPhase {

    private final int rerolls;
    private final boolean fakePhase;

    public PacketCasinoEnterRerollPhase(int rerolls) {
        this(rerolls, false);
    }

    public PacketCasinoEnterRerollPhase(int rerolls, boolean fakePhase) {
        this.rerolls = rerolls;
        this.fakePhase = fakePhase;
    }

    public static void encode(PacketCasinoEnterRerollPhase msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.rerolls);
        buf.writeBoolean(msg.fakePhase);
    }

    public static PacketCasinoEnterRerollPhase decode(FriendlyByteBuf buf) {
        int rerolls = buf.readInt();
        boolean fakePhase = buf.readBoolean();
        return new PacketCasinoEnterRerollPhase(rerolls, fakePhase);
    }

    public static void handle(PacketCasinoEnterRerollPhase msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ClientCasinoState.beginRerollPhase(msg.rerolls, msg.fakePhase);
        });
        ctx.setPacketHandled(true);
    }
}
