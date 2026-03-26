package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientGameTimer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketSyncGameTimer {

    private final boolean active;
    private final boolean countdownEnabled;
    private final int seconds;
    private final boolean rushActive;
    private final int rushSeconds;

    public PacketSyncGameTimer(boolean active, boolean countdownEnabled, int seconds, boolean rushActive, int rushSeconds) {
        this.active = active;
        this.countdownEnabled = countdownEnabled;
        this.seconds = seconds;
        this.rushActive = rushActive;
        this.rushSeconds = rushSeconds;
    }

    public static void encode(PacketSyncGameTimer msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        buf.writeBoolean(msg.countdownEnabled);
        buf.writeInt(msg.seconds);
        buf.writeBoolean(msg.rushActive);
        buf.writeInt(msg.rushSeconds);
    }

    public static PacketSyncGameTimer decode(FriendlyByteBuf buf) {
        return new PacketSyncGameTimer(
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readInt(),
                buf.readBoolean(),
                buf.readInt()
        );
    }

    public static void handle(PacketSyncGameTimer msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientGameTimer.update(msg.active, msg.countdownEnabled, msg.seconds, msg.rushActive, msg.rushSeconds)
        ));
        ctx.setPacketHandled(true);
    }
}
