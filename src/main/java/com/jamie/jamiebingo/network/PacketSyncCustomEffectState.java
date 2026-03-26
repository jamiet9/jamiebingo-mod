package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.network.ClientPacketInvoker;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Supplier;

public class PacketSyncCustomEffectState {

    private final String activeId;

    public PacketSyncCustomEffectState(String activeId) {
        this.activeId = activeId == null ? "" : activeId;
    }

    public static void encode(PacketSyncCustomEffectState msg, FriendlyByteBuf buf) {
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.activeId);
    }

    public static PacketSyncCustomEffectState decode(FriendlyByteBuf buf) {
        return new PacketSyncCustomEffectState(com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767));
    }

    public static void handle(PacketSyncCustomEffectState msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketInvoker.invoke(
                        "handleCustomEffectState",
                        new Class<?>[]{String.class},
                        msg.activeId
                )
        ));
        ctx.setPacketHandled(true);
    }
}

