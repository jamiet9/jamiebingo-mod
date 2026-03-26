package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.network.PacketTeamChatMessage;
import net.minecraftforge.client.event.ClientChatEvent;

public class ClientChatEvents {

    public static void onChat(ClientChatEvent event) {
        if (!ClientTeamChatState.pendingTeamChat) return;

        var level = ClientMinecraftUtil.getLevel(ClientMinecraftUtil.getMinecraft());
        if (level != null && ClientTeamChatState.pendingTeamChatUntilTick > 0
                && level.getGameTime() > ClientTeamChatState.pendingTeamChatUntilTick) {
            ClientTeamChatState.pendingTeamChat = false;
            ClientTeamChatState.pendingTeamChatUntilTick = -1;
            return;
        }

        String message = event.getMessage();

        if (message == null || message.isBlank()) {
            ClientTeamChatState.pendingTeamChat = false;
            ClientTeamChatState.pendingTeamChatUntilTick = -1;
            return;
        }

        if (!message.isBlank()) {
            NetworkHandler.sendToServer(
                    new PacketTeamChatMessage(message)
            );
        }

        try {
            var m = event.getClass().getMethod("setCanceled", boolean.class);
            m.invoke(event, true);
        } catch (Throwable ignored) {
        }
        try {
            var m = event.getClass().getMethod("setMessage", String.class);
            m.invoke(event, "");
        } catch (Throwable ignored) {
        }
        ClientTeamChatState.pendingTeamChat = false;
        ClientTeamChatState.pendingTeamChatUntilTick = -1;
    }
}
