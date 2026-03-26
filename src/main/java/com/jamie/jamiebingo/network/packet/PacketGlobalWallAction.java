package com.jamie.jamiebingo.network.packet;

import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import com.jamie.jamiebingo.world.PregameSettingsWallManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class PacketGlobalWallAction {
    private final ControllerSettingsSnapshot snapshot;
    private final boolean startPressed;
    private final int settingsPage;

    public PacketGlobalWallAction(ControllerSettingsSnapshot snapshot, boolean startPressed, int settingsPage) {
        this.snapshot = snapshot;
        this.startPressed = startPressed;
        this.settingsPage = Math.max(0, settingsPage);
    }

    public static void encode(PacketGlobalWallAction msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.startPressed);
        buf.writeInt(msg.settingsPage);
        buf.writeBoolean(msg.snapshot != null);
        if (msg.snapshot != null) {
            PacketGlobalWallSettingsSync.writeSnapshot(buf, msg.snapshot);
        }
    }

    public static PacketGlobalWallAction decode(FriendlyByteBuf buf) {
        boolean startPressed = buf.readBoolean();
        int settingsPage = Math.max(0, buf.readInt());
        ControllerSettingsSnapshot snapshot = buf.readBoolean() ? PacketGlobalWallSettingsSync.readSnapshot(buf) : null;
        return new PacketGlobalWallAction(snapshot, startPressed, settingsPage);
    }

    public static void handle(PacketGlobalWallAction msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender == null) return;
            net.minecraft.server.MinecraftServer server = com.jamie.jamiebingo.util.EntityServerUtil.getServer(sender);
            if (server == null) return;
            com.jamie.jamiebingo.data.BingoGameData data = com.jamie.jamiebingo.data.BingoGameData.get(server);
            if (data == null || data.isActive() || !data.pregameBoxActive
                    || !com.jamie.jamiebingo.world.PregameBoxManager.isInsideBox(sender, data)) {
                return;
            }
            if (msg.snapshot != null) {
                PregameSettingsWallManager.applySharedSnapshot(server, msg.snapshot, msg.settingsPage);
            }
            if (msg.startPressed) {
                PregameSettingsWallManager.startFromShared(sender);
            }
        });
        ctx.setPacketHandled(true);
    }
}
