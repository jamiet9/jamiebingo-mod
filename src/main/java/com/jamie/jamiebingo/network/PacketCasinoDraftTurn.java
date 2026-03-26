package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketCasinoDraftTurn {

    public final String playerName;
    public final boolean yourTurn;

    public PacketCasinoDraftTurn(String playerName, boolean yourTurn) {
        this.playerName = playerName;
        this.yourTurn = yourTurn;
    }

    public static void encode(PacketCasinoDraftTurn msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.playerName);
        buf.writeBoolean(msg.yourTurn);
    }

    public static PacketCasinoDraftTurn decode(FriendlyByteBuf buf) {
        return new PacketCasinoDraftTurn(
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                buf.readBoolean()
        );
    }

    public static void handle(PacketCasinoDraftTurn msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (!ClientCasinoState.isActive()) return;
            ClientCasinoState.setDraftTurn(msg.playerName, msg.yourTurn);
        });
        ctx.setPacketHandled(true);
    }
}
