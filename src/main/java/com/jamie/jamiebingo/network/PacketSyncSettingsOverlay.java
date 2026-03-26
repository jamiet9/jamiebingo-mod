package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientSettingsOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class PacketSyncSettingsOverlay {

    private final List<String> lines;

    public PacketSyncSettingsOverlay(List<String> lines) {
        this.lines = lines == null ? List.of() : new ArrayList<>(lines);
    }

    public static void encode(PacketSyncSettingsOverlay msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.lines.size());
        for (String line : msg.lines) {
            com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, line);
        }
    }

    public static PacketSyncSettingsOverlay decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lines.add(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
        }
        return new PacketSyncSettingsOverlay(lines);
    }

    public static void handle(PacketSyncSettingsOverlay msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> ClientSettingsOverlay.setLines(msg.lines));
        ctx.setPacketHandled(true);
    }
}

