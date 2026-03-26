package com.jamie.jamiebingo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
public class PacketPlayTeamSound {
    private final String soundId;
    private final float volume;
    private final float pitch;

    public PacketPlayTeamSound(String soundId, float volume, float pitch) {
        this.soundId = soundId;
        this.volume = volume;
        this.pitch = pitch;
    }

    public static void encode(PacketPlayTeamSound msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.soundId == null ? "" : msg.soundId);
        buf.writeFloat(msg.volume);
        buf.writeFloat(msg.pitch);
    }

    public static PacketPlayTeamSound decode(FriendlyByteBuf buf) {
        String id = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
        float volume = buf.readFloat();
        float pitch = buf.readFloat();
        return new PacketPlayTeamSound(id, volume, pitch);
    }

    public static void handle(PacketPlayTeamSound msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketInvoker.invoke(
                        "handleTeamSound",
                        new Class<?>[]{String.class, float.class, float.class},
                        msg.soundId,
                        msg.volume,
                        msg.pitch
                )
        ));
        ctx.setPacketHandled(true);
    }
}

