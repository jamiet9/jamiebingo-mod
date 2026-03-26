package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.network.ClientPacketInvoker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

/**
 * Clientbound packet that displays a random effect countdown.
 */
public class PacketEffectCountdown {

    private final String effectName;
    private final int amplifier;
    private final int secondsRemaining;
    private final boolean playSound;

    public PacketEffectCountdown(
            String effectName,
            int amplifier,
            int secondsRemaining,
            boolean playSound
    ) {
        this.effectName = effectName;
        this.amplifier = amplifier;
        this.secondsRemaining = secondsRemaining;
        this.playSound = playSound;
    }

    public static void encode(PacketEffectCountdown msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.effectName);
        buf.writeInt(msg.amplifier);
        buf.writeInt(msg.secondsRemaining);
        buf.writeBoolean(msg.playSound);
    }

    public static PacketEffectCountdown decode(FriendlyByteBuf buf) {
        return new PacketEffectCountdown(
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                buf.readInt(),
                buf.readInt(),
                buf.readBoolean()
        );
    }

    public static void handle(PacketEffectCountdown msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketInvoker.invoke(
                        "handleEffectCountdown",
                        new Class<?>[]{String.class, int.class, int.class, boolean.class},
                        msg.effectName,
                        msg.amplifier,
                        msg.secondsRemaining,
                        msg.playSound
                )
        ));
        ctx.setPacketHandled(true);
    }
}

