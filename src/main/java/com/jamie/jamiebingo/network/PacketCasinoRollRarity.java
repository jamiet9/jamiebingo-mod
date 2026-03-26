package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketCasinoRollRarity {

    public final int x;
    public final int y;
    public final String rarity;

    public PacketCasinoRollRarity(int x, int y, String rarity) {
        this.x = x;
        this.y = y;
        this.rarity = rarity;
    }

    public static void encode(PacketCasinoRollRarity msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.rarity);
    }

    public static PacketCasinoRollRarity decode(FriendlyByteBuf buf) {
        return new PacketCasinoRollRarity(
                buf.readInt(),
                buf.readInt(),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
        );
    }

    public static void handle(PacketCasinoRollRarity msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {

            if (!ClientCasinoState.isActive()) return;

            ClientCasinoState.onRarityRolled(msg.x, msg.y, msg.rarity);
        });
        context.setPacketHandled(true);
    }
}

