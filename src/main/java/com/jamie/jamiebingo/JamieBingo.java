package com.jamie.jamiebingo;

import com.jamie.jamiebingo.addons.effects.*;
import com.jamie.jamiebingo.data.GlobalChatHandler;
import com.jamie.jamiebingo.commands.CommandEvents;
import com.jamie.jamiebingo.item.ModItems;
import com.jamie.jamiebingo.menu.ModMenus;
import com.jamie.jamiebingo.network.NetworkHandler;
import com.jamie.jamiebingo.quest.QuestDatabase;
import com.jamie.jamiebingo.sound.ModSounds;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

@Mod(JamieBingo.MOD_ID)
public class JamieBingo {

    public static final String MOD_ID = "jamiebingo";
    private static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean(false);

    public JamieBingo() {
        BusGroup modBus = FMLJavaModLoadingContext.get().getModBusGroup();

        // Registries
        ModMenus.register(modBus);
        ModItems.register(modBus);
        ModSounds.register(modBus);

        DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> com.jamie.jamiebingo.client.ClientEventRegistrar.register(modBus)
        );

        // Fallback bootstrap for launcher environments where MOD bus setup may not fire.
        LOGGER.info("[JamieBingo] Constructor called, bootstrapping");
        bootstrapCommon();
    }

    /* ===============================
       CUSTOM EFFECT REGISTRATION
       =============================== */
    private static void registerCustomEffects() {

        CustomEffectRegistry.register(new EffectInvertedScreen());
        CustomEffectRegistry.register(new EffectRandomMovement());
        CustomEffectRegistry.register(new EffectInvertedDrowning());
        CustomEffectRegistry.register(new EffectPlayerSwap());
        CustomEffectRegistry.register(new EffectRTP());
        CustomEffectRegistry.register(new EffectPlayerScaleRandom());

        LOGGER.info("[JamieBingo] Custom effects registered");
    }

    private static void bootstrapCommon() {
        if (!BOOTSTRAPPED.compareAndSet(false, true)) return;

        // ===============================
        // NETWORK
        // ===============================
        NetworkHandler.register();
        LOGGER.info("[JamieBingo] Network setup complete");

        // ===============================
        // QUESTS
        // ===============================
        QuestDatabase.load();

        // ===============================
        // GLOBAL EVENTS
        // ===============================
        ServerChatEvent.BUS.addListener(GlobalChatHandler::onServerChat);
        LOGGER.info("[JamieBingo] GlobalChatHandler registered");

        LivingDeathEvent.BUS.addListener(com.jamie.jamiebingo.world.HardcoreHandler::onPlayerDeath);
        LOGGER.info("[JamieBingo] HardcoreHandler registered");

        PlayerEvent.Clone.BUS.addListener(com.jamie.jamiebingo.world.InventoryDeathHandler::onPlayerClone);
        LOGGER.info("[JamieBingo] InventoryDeathHandler registered");

        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(com.jamie.jamiebingo.item.BingoControllerGiveHandler::onPlayerJoin);
        LOGGER.info("[JamieBingo] BingoControllerGiveHandler registered");

        PlayerInteractEvent.RightClickItem.BUS.addListener(com.jamie.jamiebingo.item.PlayerTrackerHandler::onRightClickItem);
        TickEvent.PlayerTickEvent.Post.BUS.addListener(com.jamie.jamiebingo.item.PlayerTrackerHandler::onPlayerTick);
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(com.jamie.jamiebingo.item.PlayerTrackerHandler::onPlayerRespawn);
        LivingDropsEvent.BUS.addListener(com.jamie.jamiebingo.item.PlayerTrackerHandler::onLivingDrops);
        LOGGER.info("[JamieBingo] PlayerTrackerHandler registered");

        PlayerEvent.Clone.BUS.addListener(com.jamie.jamiebingo.rtp.BingoRtpHandler::onPlayerClone);
        LivingDeathEvent.BUS.addListener(com.jamie.jamiebingo.rtp.BingoRtpHandler::onLivingDeath);
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(com.jamie.jamiebingo.rtp.BingoRtpHandler::onPlayerRespawn);
        LOGGER.info("[JamieBingo] BingoRtpHandler registered");

        // ===============================
        // COMMANDS
        // ===============================
        RegisterCommandsEvent.BUS.addListener(event -> {
            LOGGER.info("[JamieBingo] RegisterCommandsEvent fired");
            CommandEvents.onRegisterCommands(event);
        });

        // ===============================
        // SERVER STARTED
        // ===============================
        net.minecraftforge.event.server.ServerStartedEvent.BUS.addListener(ServerEvents::serverStarted);
        net.minecraftforge.event.server.ServerStartingEvent.BUS.addListener(ServerEvents::serverStarting);

        // ===============================
        // CUSTOM RANDOM EFFECTS
        // ===============================
        registerCustomEffects();
    }

    @Mod.EventBusSubscriber(modid = JamieBingo.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class ModLifecycle {

        @net.minecraftforge.eventbus.api.listener.SubscribeEvent
        public static void commonSetup(final FMLCommonSetupEvent event) {
            event.enqueueWork(() -> {
                bootstrapCommon();
            });
        }
    }
}




