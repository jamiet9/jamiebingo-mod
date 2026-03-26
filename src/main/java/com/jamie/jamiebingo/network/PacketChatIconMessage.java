package com.jamie.jamiebingo.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
public class PacketChatIconMessage {
    private final Component message;
    private final String slotId;

    public PacketChatIconMessage(Component message, String slotId) {
        this.message = message;
        this.slotId = slotId;
    }

    public static void encode(PacketChatIconMessage msg, FriendlyByteBuf buf) {
        ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buf, msg.message);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.slotId == null ? "" : msg.slotId);
    }

    public static PacketChatIconMessage decode(FriendlyByteBuf buf) {
        Component message = ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buf);
        String slotId = com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767);
        return new PacketChatIconMessage(message, slotId);
    }

    public static void handle(PacketChatIconMessage msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> ClientPacketInvoker.invoke(
                        "handleChatIconMessage",
                        new Class<?>[]{Component.class, String.class},
                        msg.message,
                        msg.slotId
                )
        ));
        ctx.setPacketHandled(true);
    }
}

