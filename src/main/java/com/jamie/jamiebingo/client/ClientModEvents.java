package com.jamie.jamiebingo.client;

import com.jamie.jamiebingo.JamieBingo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only lifecycle hooks.
 *
 * IMPORTANT:
 * Overlay registration must ONLY happen in OverlayInit.java
 * (RegisterGuiOverlaysEvent). Do NOT register overlays here.
 */
@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientModEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Intentionally empty.
        // Keep for future client-only setup if needed.
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ClientKeybinds.TOGGLE_FULLSCREEN_CARD);
        event.register(ClientKeybinds.TOGGLE_CARD_OVERLAY);
        event.register(ClientKeybinds.TOGGLE_SETTINGS_OVERLAY);
        event.register(ClientKeybinds.TOGGLE_SCOREBOARD_OVERLAY);
    }
}




