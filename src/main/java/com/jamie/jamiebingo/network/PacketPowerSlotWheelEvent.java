package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientPowerSlotWheelAnimation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class PacketPowerSlotWheelEvent {
    private final boolean buff;

    public PacketPowerSlotWheelEvent(boolean buff) {
        this.buff = buff;
    }

    public static void encode(PacketPowerSlotWheelEvent msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.buff);
    }

    public static PacketPowerSlotWheelEvent decode(FriendlyByteBuf buf) {
        return new PacketPowerSlotWheelEvent(buf.readBoolean());
    }

    public static void handle(PacketPowerSlotWheelEvent msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPowerSlotWheelAnimation.start(msg.buff)
        ));
        ctx.setPacketHandled(true);
    }
}
