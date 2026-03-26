package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.client.screen.GameHistoryScreen;
import com.jamie.jamiebingo.network.packet.PacketOpenGameHistory;
import net.minecraft.client.Minecraft;

import java.util.List;

public final class ClientGameHistoryOpener {
    private ClientGameHistoryOpener() {
    }

    public static void open(List<PacketOpenGameHistory.EntryData> entries) {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null) return;
        List<PacketOpenGameHistory.EntryData> merged =
                ClientGameHistoryStore.mergeForCurrentAccount(entries);
        ClientMinecraftUtil.setScreen(mc, new GameHistoryScreen(merged));
    }
}
