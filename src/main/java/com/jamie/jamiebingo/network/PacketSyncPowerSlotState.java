package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientPowerSlotState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketSyncPowerSlotState {
    public final boolean active;
    public final String slotId;
    public final String displayName;
    public final int remainingSeconds;

    public PacketSyncPowerSlotState(boolean active, String slotId, String displayName, int remainingSeconds) {
        this.active = active;
        this.slotId = slotId == null ? "" : slotId;
        this.displayName = displayName == null ? "" : displayName;
        this.remainingSeconds = remainingSeconds;
    }

    public static void encode(PacketSyncPowerSlotState msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.active);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.slotId);
        com.jamie.jamiebingo.util.FriendlyByteBufUtil.writeString(buf, msg.displayName);
        buf.writeInt(msg.remainingSeconds);
    }

    public static PacketSyncPowerSlotState decode(FriendlyByteBuf buf) {
        return new PacketSyncPowerSlotState(
                buf.readBoolean(),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                com.jamie.jamiebingo.util.FriendlyByteBufUtil.readString(buf, 32767),
                buf.readInt()
        );
    }

    public static void handle(PacketSyncPowerSlotState msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (!msg.active) {
                ClientPowerSlotState.clear();
                return;
            }
            ClientPowerSlotState.set(true, msg.slotId, msg.displayName, msg.remainingSeconds);
        });
        ctx.setPacketHandled(true);
    }
}
