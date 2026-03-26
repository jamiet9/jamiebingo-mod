package com.jamie.jamiebingo;

import com.jamie.jamiebingo.bingo.BingoCommands;
import com.jamie.jamiebingo.data.ItemDatabase;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEvents {
    private static final Logger LOGGER = LogManager.getLogger(JamieBingo.MOD_ID);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(BingoCommands.register());
        LOGGER.info("[JamieBingo] Registered /bingo commands");
    }

    @SubscribeEvent
    public static void serverStarting(net.minecraftforge.event.server.ServerStartingEvent event) {
        com.jamie.jamiebingo.mines.MineModeManager.resetTransientRuntime();
        com.jamie.jamiebingo.power.PowerSlotManager.resetTransientRuntime();
        com.jamie.jamiebingo.casino.CasinoModeManager.resetTransientState(event.getServer(), false);
        try {
            var dispatcher = event.getServer().getCommands().getDispatcher();
            if (dispatcher.getRoot().getChild("bingo") == null) {
                dispatcher.register(BingoCommands.register());
                LOGGER.info("[JamieBingo] Registered /bingo commands (serverStarting fallback)");
            } else {
                LOGGER.info("[JamieBingo] /bingo already registered at serverStarting");
            }
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void serverStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        LOGGER.info("[JamieBingo] Ensuring database ready...");
        ItemDatabase.load();

        // Fallback command registration if RegisterCommandsEvent didn't fire.
        try {
            var server = event.getServer();
            var dispatcher = server.getCommands().getDispatcher();
            if (dispatcher.getRoot().getChild("bingo") == null) {
                dispatcher.register(BingoCommands.register());
                LOGGER.info("[JamieBingo] Registered /bingo commands (serverStarted fallback)");
            }
        } catch (Throwable ignored) {
        }
    }
}




