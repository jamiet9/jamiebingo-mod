package com.jamie.jamiebingo.menu;

import com.jamie.jamiebingo.network.packet.PacketGlobalWallSettingsSync;
import com.jamie.jamiebingo.util.NbtUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public final class PlayerControllerSettingsStore {
    private static final String TAG = "jamiebingo_controller_snapshot_v1";

    private PlayerControllerSettingsStore() {
    }

    public static void save(ServerPlayer player, ControllerSettingsSnapshot snapshot) {
        if (player == null || snapshot == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        PacketGlobalWallSettingsSync.writeSnapshot(buf, snapshot);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(0, bytes);
        String encoded = java.util.Base64.getEncoder().encodeToString(bytes);
        NbtUtil.putString(player.getPersistentData(), TAG, encoded);
    }

    public static ControllerSettingsSnapshot load(ServerPlayer player) {
        if (player == null) return null;
        String encoded = NbtUtil.getString(player.getPersistentData(), TAG, "");
        if (encoded == null || encoded.isBlank()) return null;
        try {
            byte[] bytes = java.util.Base64.getDecoder().decode(encoded);
            if (bytes.length == 0) return null;
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            return PacketGlobalWallSettingsSync.readSnapshot(buf);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void clear(ServerPlayer player) {
        if (player == null) return;
        player.getPersistentData().remove(TAG);
    }
}
