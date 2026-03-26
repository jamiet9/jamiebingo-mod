package com.jamie.jamiebingo.client;

import net.minecraft.client.Minecraft;

public final class ClientSeedGuiOpener {

    private ClientSeedGuiOpener() {
    }

    public static void open() {
        Minecraft mc = ClientMinecraftUtil.getMinecraft();
        if (mc == null) return;
        ClientMinecraftUtil.setScreen(mc, new SeedPasteScreen());
    }
}
