package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.bingo.BingoSeedHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketSeedPastePart {

    private static final Map<UUID, SeedBuffer> BUFFERS = new HashMap<>();

    private final int index;
    private final int total;
    private final String chunk;

    public PacketSeedPastePart(int index, int total, String chunk) {
        this.index = index;
        this.total = total;
        this.chunk = chunk == null ? "" : chunk;
    }

    public static void encode(PacketSeedPastePart msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.index);
        buf.writeInt(msg.total);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.chunk);
    }

    public static PacketSeedPastePart decode(FriendlyByteBuf buf) {
        return new PacketSeedPastePart(
                buf.readInt(),
                buf.readInt(),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
        );
    }

    public static void handle(PacketSeedPastePart msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (msg.total <= 0 || msg.index <= 0 || msg.index > msg.total) return;
            SeedBuffer buf = BUFFERS.computeIfAbsent(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), id -> new SeedBuffer(msg.total));
            if (buf.total != msg.total) {
                BUFFERS.put(com.jamie.jamiebingo.util.EntityUtil.getUUID(player), new SeedBuffer(msg.total));
                buf = BUFFERS.get(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
            }
            buf.parts.put(msg.index, msg.chunk);
        });
        ctx.setPacketHandled(true);
    }

    public static boolean finishFor(ServerPlayer player) {
        SeedBuffer buf = BUFFERS.get(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        if (buf == null) {
            player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("No seed parts received."));
            return false;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= buf.total; i++) {
            String part = buf.parts.get(i);
            if (part == null) {
                player.sendSystemMessage(com.jamie.jamiebingo.util.ComponentUtil.literal("Missing seed part " + i + "/" + buf.total + "."));
                return false;
            }
            sb.append(part.trim());
        }
        BUFFERS.remove(com.jamie.jamiebingo.util.EntityUtil.getUUID(player));
        return BingoSeedHelper.startFromSeed(player.createCommandSourceStack(), sb.toString());
    }

    private static final class SeedBuffer {
        final int total;
        final Map<Integer, String> parts = new HashMap<>();

        SeedBuffer(int total) {
            this.total = total;
        }
    }
}


