package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.casino.ClientCasinoState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketCasinoRollFinal {

    public final int x;
    public final int y;
    public final String id;
    public final String name;
    public final String category;
    public final String rarity;
    public final boolean isQuest;

    public PacketCasinoRollFinal(
            int x, int y,
            String id, String name,
            String category, String rarity,
            boolean isQuest
    ) {
        this.x = x;
        this.y = y;
        this.id = id;
        this.name = name;
        this.category = category;
        this.rarity = rarity;
        this.isQuest = isQuest;
    }

    public static void encode(PacketCasinoRollFinal msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.id);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.name);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.category);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.rarity);
        buf.writeBoolean(msg.isQuest);
    }

    public static PacketCasinoRollFinal decode(FriendlyByteBuf buf) {
        return new PacketCasinoRollFinal(
                buf.readInt(),
                buf.readInt(),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                buf.readBoolean()
        );
    }

    public static void handle(PacketCasinoRollFinal msg, CustomPayloadEvent.Context ctx) {
        CustomPayloadEvent.Context context = ctx;
        context.enqueueWork(() -> {

            if (!ClientCasinoState.isActive()) return;

            ClientCasinoState.onFinalRolled(
                    msg.x,
                    msg.y,
                    msg.id,
                    msg.name,
                    msg.category,
                    msg.rarity,
                    msg.isQuest
            );
        });
        context.setPacketHandled(true);
    }
}

