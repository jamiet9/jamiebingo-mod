package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketCasinoRerollTurn {

    public final String playerName;
    public final boolean yourTurn;

    public PacketCasinoRerollTurn(String playerName, boolean yourTurn) {
        this.playerName = playerName;
        this.yourTurn = yourTurn;
    }

    public static void encode(PacketCasinoRerollTurn msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.playerName);
        buf.writeBoolean(msg.yourTurn);
    }

    public static PacketCasinoRerollTurn decode(FriendlyByteBuf buf) {
        return new PacketCasinoRerollTurn(
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                buf.readBoolean()
        );
    }

    public static void handle(PacketCasinoRerollTurn msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {

            if (!ClientCasinoState.isActive()) return;

            ClientCasinoState.setRerollTurn(msg.playerName, msg.yourTurn);
        });
        context.setPacketHandled(true);
    }
}

