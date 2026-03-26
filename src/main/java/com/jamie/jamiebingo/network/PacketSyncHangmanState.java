package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientHangmanState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketSyncHangmanState {

    private final boolean active;
    private final boolean slotRevealed;
    private final String line1;
    private final String line2;

    public PacketSyncHangmanState(boolean active, boolean slotRevealed, String line1, String line2) {
        this.active = active;
        this.slotRevealed = slotRevealed;
        this.line1 = line1 == null ? "" : line1;
        this.line2 = line2 == null ? "" : line2;
    }

    public static void encode(PacketSyncHangmanState msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.slotRevealed);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.line1);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.line2);
    }

    public static PacketSyncHangmanState decode(FriendlyByteBuf buf) {
        return new PacketSyncHangmanState(
                buf.readBoolean(),
                buf.readBoolean(),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767)
        );
    }

    public static void handle(PacketSyncHangmanState msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientHangmanState.update(msg.active, msg.slotRevealed, msg.line1, msg.line2)
        ));
        ctx.setPacketHandled(true);
    }
}

