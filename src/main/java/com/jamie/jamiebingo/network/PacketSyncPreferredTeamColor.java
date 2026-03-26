package com.jamie.jamiebingo.network;

import com.jamie.jamiebingo.client.ClientTeamPreferenceSettings;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;

public class PacketSyncPreferredTeamColor {
    private final int colorId;

    public PacketSyncPreferredTeamColor(int colorId) {
        this.colorId = colorId;
    }

    public static void encode(PacketSyncPreferredTeamColor msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.colorId);
    }

    public static PacketSyncPreferredTeamColor decode(FriendlyByteBuf buf) {
        return new PacketSyncPreferredTeamColor(buf.readInt());
    }

    public static void handle(PacketSyncPreferredTeamColor msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> {
                    if (msg.colorId >= 0) {
                        ClientTeamPreferenceSettings.setPreferredTeamColorId(msg.colorId);
                    }
                }
        ));
        ctx.setPacketHandled(true);
    }
}
