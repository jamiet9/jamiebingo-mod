package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.network.ClientPacketInvoker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;


/**
 * Clientbound packet notifying that a random effect has been applied.
 */
public class PacketEffectApplied {

    private final Component effectName;

    public PacketEffectApplied(Component effectName) {
        this.effectName = effectName;
    }

    public static void encode(PacketEffectApplied msg, FriendlyByteBuf buf) {
        ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buf, msg.effectName);
    }

    public static PacketEffectApplied decode(FriendlyByteBuf buf) {
        return new PacketEffectApplied(ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buf));
    }

    public static void handle(PacketEffectApplied msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketInvoker.invoke(
                        "handleEffectApplied",
                        new Class<?>[]{Component.class},
                        msg.effectName
                )
        ));
        ctx.setPacketHandled(true);
    }
}
