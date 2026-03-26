package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.menu.ControllerSettingsSnapshot;
import com.jamie.jamiebingo.network.packet.PacketGlobalWallSettingsSync;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ClientControllerSettingsStore {
    private static final Path CONFIG_PATH =
            FMLPaths.CONFIGDIR.get().resolve("jamiebingo_controller_snapshot_v1.txt");

    private ClientControllerSettingsStore() {
    }

    public static void save(ControllerSettingsSnapshot snapshot) {
        if (snapshot == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        PacketGlobalWallSettingsSync.writeSnapshot(buf, snapshot);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(0, bytes);
        String encoded = java.util.Base64.getEncoder().encodeToString(bytes);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, encoded, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    public static ControllerSettingsSnapshot load() {
        if (!Files.exists(CONFIG_PATH)) return null;
        try {
            String encoded = Files.readString(CONFIG_PATH, StandardCharsets.UTF_8);
            if (encoded == null || encoded.isBlank()) return null;
            byte[] bytes = java.util.Base64.getDecoder().decode(encoded.trim());
            if (bytes.length == 0) return null;
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            return PacketGlobalWallSettingsSync.readSnapshot(buf);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
