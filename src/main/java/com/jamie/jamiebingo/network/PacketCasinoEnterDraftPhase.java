package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketCasinoEnterDraftPhase {

    public static void encode(PacketCasinoEnterDraftPhase msg, FriendlyByteBuf buf) {
    }

    public static PacketCasinoEnterDraftPhase decode(FriendlyByteBuf buf) {
        return new PacketCasinoEnterDraftPhase();
    }

    public static void handle(PacketCasinoEnterDraftPhase msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(ClientCasinoState::beginDraftPhase);
        ctx.setPacketHandled(true);
    }
}
