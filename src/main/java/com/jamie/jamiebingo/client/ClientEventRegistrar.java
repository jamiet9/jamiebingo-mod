package com.jamie.jamiebingo.client;

import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;

public final class ClientEventRegistrar {

    private ClientEventRegistrar() {
    }

    public static void register(BusGroup modBus) {
        MovementInputUpdateEvent.BUS.addListener(ClientMovementHandler::onMovementInput);
        AddGuiOverlayLayersEvent.getBus(modBus).addListener(OverlayInit::registerOverlays);
        RegisterKeyMappingsEvent.getBus(modBus).addListener(ClientModEvents::onRegisterKeyMappings);
        TickEvent.ClientTickEvent.Post.BUS.addListener(ClientForgeEvents::onClientTick);
        ClientChatEvent.BUS.addListener(ClientChatEvents::onChat);
        CustomizeGuiOverlayEvent.Chat.BUS.addListener(ClientChatIconEvents::onChatOverlay);
        com.jamie.jamiebingo.client.render.GlobalWallScreenRenderer.register();
    }
}
