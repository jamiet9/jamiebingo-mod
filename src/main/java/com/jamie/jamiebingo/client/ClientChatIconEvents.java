package com.jamie.jamiebingo.client;

import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;

public final class ClientChatIconEvents {

    private ClientChatIconEvents() {}

    public static void onChatOverlay(CustomizeGuiOverlayEvent.Chat event) {
        if (event == null) return;
        ClientChatIconOverlay.render(event.getGuiGraphics());
    }
}
