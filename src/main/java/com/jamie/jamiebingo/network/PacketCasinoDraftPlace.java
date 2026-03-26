package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.casino.CasinoDraftManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketCasinoDraftPlace {

    private final int choiceIndex;
    private final int x;
    private final int y;

    public PacketCasinoDraftPlace(int choiceIndex, int x, int y) {
        this.choiceIndex = choiceIndex;
        this.x = x;
        this.y = y;
    }

    public static void encode(PacketCasinoDraftPlace msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.choiceIndex);
        buf.writeInt(msg.x);
        buf.writeInt(msg.y);
    }

    public static PacketCasinoDraftPlace decode(FriendlyByteBuf buf) {
        return new PacketCasinoDraftPlace(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(PacketCasinoDraftPlace msg, CustomPayloadEvent.Context ctx) {
        if (!ctx.isServerSide()) {
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(player);
            if (server == null) return;

            CasinoDraftManager.requestPlace(server, player, msg.choiceIndex, msg.x, msg.y);
        });

        ctx.setPacketHandled(true);
    }
}
